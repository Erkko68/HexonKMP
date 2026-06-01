package eric.bitria.hexonkmp.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.erkko68.filament.Box
import io.github.erkko68.filament.IndexBuffer
import io.github.erkko68.filament.MaterialInstance
import io.github.erkko68.filament.RenderableManager
import io.github.erkko68.filament.SurfaceOrientation
import io.github.erkko68.filament.VertexBuffer
import io.github.erkko68.filament.VertexBuffer.AttributeType
import io.github.erkko68.filament.VertexBuffer.VertexAttribute
import io.github.erkko68.filament.compose.FilamentSceneScope
import io.github.erkko68.filament.compose.LocalFilamentEngine
import io.github.erkko68.filament.compose.LocalFilamentScene
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.toBytes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// A flat-top hexagon lying in the XZ plane, top face up (+Y). The library only
// ships Cube/Sphere/Cylinder/Plane and keeps its Mesh/MeshData internal, so we
// build the hexagon directly from the public Filament buffer API — same approach
// the library uses internally (see filament-compose primitives/Plane.kt).
//
// @param radius  centre-to-corner distance (circumradius).
// @param onCreate receives the renderable entity id (for pick() mapping later).
@Composable
fun FilamentSceneScope.Hexagon(
    material: MaterialInstance,
    position: Position = Position(0f),
    radius: Float = 1f,
    onCreate: (entity: Int) -> Unit = {},
) {
    val engine = LocalFilamentEngine.current
    val scene = LocalFilamentScene.current

    // Build CPU geometry once per radius.
    val geometry = remember(radius) { hexagonGeometry(radius) }

    // Upload to GPU buffers; recreate if the geometry changes.
    val handles = remember(geometry) { geometry.upload(engine) }
    DisposableEffect(handles) {
        onDispose {
            engine.destroyVertexBuffer(handles.first)
            engine.destroyIndexBuffer(handles.second)
        }
    }

    val entity = remember(handles, material) {
        engine.getEntityManager().create().also { e ->
            RenderableManager.Builder(1)
                .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, handles.first, handles.second)
                .material(0, material)
                .boundingBox(Box(0f, 0f, 0f, radius, 0.01f, radius))
                .build(engine, e)
        }
    }

    DisposableEffect(entity) {
        scene.addEntity(entity)
        onCreate(entity)
        onDispose {
            scene.removeEntity(entity)
            engine.getRenderableManager().destroy(entity)
            engine.getEntityManager().destroy(entity)
        }
    }

    // Position via the transform manager (translation only — hexes don't rotate).
    DisposableEffect(entity, position) {
        val tm = engine.getTransformManager()
        if (!tm.hasComponent(entity)) tm.create(entity)
        val m = translationMatrix(position.x, position.y, position.z)
        tm.setTransform(tm.getInstance(entity), m)
        onDispose { }
    }
}

private class HexGeometry(
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray,
    val indices: IntArray,
)

// Triangle-fan hexagon: a centre vertex + 6 rim vertices, top face only (the
// board is viewed from above). Flat-top: first corner at angle 0 (+X).
private fun hexagonGeometry(radius: Float): HexGeometry {
    val corners = 6
    val vertexCount = corners + 1 // centre + rim

    val positions = FloatArray(vertexCount * 3)
    val normals = FloatArray(vertexCount * 3)
    val uvs = FloatArray(vertexCount * 2)

    // Centre vertex.
    positions[0] = 0f; positions[1] = 0f; positions[2] = 0f
    normals[0] = 0f; normals[1] = 1f; normals[2] = 0f
    uvs[0] = 0.5f; uvs[1] = 0.5f

    for (i in 0 until corners) {
        val angle = (PI.toFloat() / 3f) * i
        val x = radius * cos(angle)
        val z = radius * sin(angle)
        val v = (i + 1)
        positions[v * 3] = x
        positions[v * 3 + 1] = 0f
        positions[v * 3 + 2] = z
        normals[v * 3] = 0f
        normals[v * 3 + 1] = 1f
        normals[v * 3 + 2] = 0f
        uvs[v * 2] = 0.5f + 0.5f * cos(angle)
        uvs[v * 2 + 1] = 0.5f + 0.5f * sin(angle)
    }

    // Fan triangles, CCW when viewed from +Y (above).
    val indices = IntArray(corners * 3)
    for (i in 0 until corners) {
        val next = (i + 1) % corners
        indices[i * 3] = 0
        indices[i * 3 + 1] = next + 1
        indices[i * 3 + 2] = i + 1
    }

    return HexGeometry(positions, normals, uvs, indices)
}

private fun HexGeometry.upload(engine: io.github.erkko68.filament.Engine): Pair<VertexBuffer, IndexBuffer> {
    val vertexCount = positions.size / 3
    val triangleCount = indices.size / 3

    // LIT materials need the packed tangent frame derived from normals + UVs.
    val tangents = FloatArray(vertexCount * 4)
    val orientation = SurfaceOrientation.Builder()
        .vertexCount(vertexCount)
        .positions(positions)
        .normals(normals)
        .uvs(uvs)
        .triangleCount(triangleCount)
        .triangles32(indices)
        .build()
    orientation.getQuatsAsFloat(tangents, vertexCount)
    orientation.destroy()

    val vb = VertexBuffer.Builder()
        .vertexCount(vertexCount)
        .bufferCount(3)
        .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3)
        .attribute(VertexAttribute.TANGENTS, 1, AttributeType.FLOAT4)
        .attribute(VertexAttribute.UV0, 2, AttributeType.FLOAT2)
        .build(engine)
    vb.setBufferAt(engine, 0, positions.toBytes())
    vb.setBufferAt(engine, 1, tangents.toBytes())
    vb.setBufferAt(engine, 2, uvs.toBytes())

    val ib = IndexBuffer.Builder()
        .indexCount(indices.size)
        .bufferType(IndexBuffer.Builder.IndexType.UINT)
        .build(engine)
    ib.setBuffer(engine, indices.toBytes())

    return vb to ib
}

// Column-major 4x4 translation matrix (Filament's TransformManager convention).
private fun translationMatrix(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f,
)
