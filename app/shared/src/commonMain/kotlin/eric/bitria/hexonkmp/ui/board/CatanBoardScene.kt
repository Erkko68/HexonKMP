package eric.bitria.hexonkmp.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import eric.bitria.hexonkmp.core.game.model.GameState
import eric.bitria.hexonkmp.core.game.model.board.Edge
import eric.bitria.hexonkmp.core.game.model.board.Vertex
import hexonkmp.app.shared.generated.resources.Res
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.LightManager
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.compose.FilamentSceneView
import io.github.erkko68.filament.compose.FilamentViewState
import io.github.erkko68.filament.compose.rememberFilamentViewState
import io.github.erkko68.filament.compose.scene.Color
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.GltfAsset
import io.github.erkko68.filament.compose.scene.GltfInstance
import io.github.erkko68.filament.compose.scene.Light
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.Scale
import io.github.erkko68.filament.compose.scene.SkyboxSource
import io.github.erkko68.filament.compose.scene.primitives.Cube
import io.github.erkko68.filament.compose.scene.rememberCameraState
import io.github.erkko68.filament.compose.scene.rememberGltfAsset
import io.github.erkko68.filament.compose.scene.rememberMaterial
import io.github.erkko68.filament.compose.scene.rememberMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberSkyboxState
import io.github.erkko68.filament.utils.Quaternion
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val HEX_SIZE = 1f
private const val BUILDING_Y = 0.15f
private const val ROAD_Y = 0.06f

// Number tokens (glb models, exported flat from Blender) sit just above the tile
// at its center. Scale/height are eyeballed for a ~1-unit hex — tweak to taste.
private const val NUMBER_Y = 0.08f
private const val NUMBER_SCALE = 0.8f

private val RAD_TO_DEG = 180f / kotlin.math.PI.toFloat()

// Lay the (flat) number models with a 45° counterclockwise spin about the
// up axis (positive Y rotation reads CCW from the top-down camera).
private val NUMBER_ROTATION = Quaternion.fromAxisAngle(Direction(0f, 1f, 0f), 45f)

// Renders the authoritative GameState as a 3D Catan board: colored hexagon tiles,
// cubes for settlements/cities, thin cubes for roads. When [ghostSettlements] /
// [ghostRoads] are non-empty they're drawn as OPAQUE, desaturated player-colored
// markers the player taps to place there. Markers are opaque on purpose: Filament
// picking reads the depth buffer, which transparent materials don't write to, so
// only opaque geometry is hit-testable. Pure view — no game logic here.
@OptIn(ExperimentalResourceApi::class)
@Composable
fun CatanBoardScene(
    state: GameState,
    engine: Engine,
    modifier: Modifier = Modifier,
    me: eric.bitria.hexonkmp.core.game.model.PlayerId? = null,
    ghostSettlements: List<Vertex> = emptyList(),
    ghostRoads: List<Edge> = emptyList(),
    ghostCities: List<Vertex> = emptyList(),
    onPickVertex: (Vertex) -> Unit = {},
    onPickEdge: (Edge) -> Unit = {},
) {
    val halfExtent = remember(state.board.tiles) {
        val maxR = state.board.tiles.maxOfOrNull { tile ->
            val c = HexMath.center(tile.hex, HEX_SIZE)
            maxOf(kotlin.math.abs(c.x), kotlin.math.abs(c.z))
        } ?: 4f
        (maxR + HEX_SIZE * 2.0f).toDouble()
    }

    val cameraState = rememberCameraState(up = Direction(0f, 1f, 0f))
    val camera = rememberBoardCameraState(cameraState, baseHalfExtent = halfExtent.toFloat())
    val skybox = rememberSkyboxState(source = SkyboxSource.Color(Color(0.10f, 0.14f, 0.20f)))
    val viewState = rememberFilamentViewState()

    var viewportHeight by remember { mutableStateOf(1) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // True only while placing — i.e. ghost markers are on the board. Picking is
    // gated on this so taps do no work the rest of the time.
    val placing = ghostSettlements.isNotEmpty() || ghostRoads.isNotEmpty() || ghostCities.isNotEmpty()

    // Map each ghost marker's renderable entity -> the board location it offers,
    // so a pick result resolves back to a Vertex/Edge. Rebuilt when the candidate
    // set changes (so stale entity ids from a previous build mode are dropped).
    // Settlement and city ghosts both resolve to a Vertex (onPickVertex routes by
    // the armed build mode).
    val entityToVertex = remember(ghostSettlements, ghostCities) { mutableMapOf<Int, Vertex>() }
    val entityToEdge = remember(ghostRoads) { mutableMapOf<Int, Edge>() }

    val solid: Material? = rememberMaterial(engine) {
        Res.readBytes("files/materials/board_color.filamat")
    }

    // Number-token models, indexed 0..12 (a tile's token is its index). Loaded
    // once and shared; each tile places its own instance. The fixed range keeps
    // these composable call sites stable across recompositions.
    val numberAssets = ArrayList<GltfAsset?>(13)
    for (n in 0..12) {
        numberAssets += rememberGltfAsset(engine, key = n) { Res.readBytes("files/models/numbers/$n.glb") }
    }

    val ghostColor = remember(me, state.players) {
        // Desaturated tint of the player's color; falls back to a neutral grey.
        val base = me?.let { ResourceColors.forPlayer(it, state.players) } ?: Color(0.8f, 0.8f, 0.8f)
        ResourceColors.ghost(base)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                boxSize = it
                viewportHeight = it.height
                camera.setViewport(it.width, it.height)
            }
            // One gesture handler arbitrates tap vs pan/zoom. We only run a pick
            // query while placing (ghost markers are showing) — otherwise a tap
            // does nothing and we skip the work entirely.
            .boardGestures(
                state = camera,
                viewportHeight = { viewportHeight },
                onTap = if (!placing) null else { offset ->
                    pickAt(offset, viewState, boxSize) { entity ->
                        entityToVertex[entity]?.let { onPickVertex(it); return@pickAt }
                        entityToEdge[entity]?.let { onPickEdge(it) }
                    }
                },
            ),
    ) {
        FilamentSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            cameraState = cameraState,
            viewState = viewState,
            skyboxState = skybox,
        ) {
            Light(
                type = LightManager.Type.DIRECTIONAL,
                direction = Direction(0.4f, -1f, -0.5f),
                intensity = 90_000f,
            )

            val solidMat = solid ?: return@FilamentSceneView

            // Tiles.
            state.board.tiles.forEach { tile ->
                val c = HexMath.center(tile.hex, HEX_SIZE)
                val color = ResourceColors.forTerrain(tile.terrain)
                val inst = rememberMaterialInstance(solidMat, engine = engine).apply {
                    setParameter("baseColor", color.x, color.y, color.z)
                }
                Hexagon(material = inst, position = Position(c.x, 0f, c.z), radius = HEX_SIZE * 0.95f)
            }

            // Number tokens: a white glb model centered on each non-desert tile.
            // One shared white material instance overrides whatever the glb shipped
            // with. The model's pivot is centered, so it drops straight on the hex
            // center; it was exported flat, so identity rotation lies it on the board.
            val whiteNumber = rememberMaterialInstance(solidMat, engine = engine).apply {
                setParameter("baseColor", 1f, 1f, 1f)
            }
            state.board.tiles.forEach { tile ->
                val token = tile.token ?: return@forEach
                val asset = numberAssets.getOrNull(token) ?: return@forEach
                val c = HexMath.center(tile.hex, HEX_SIZE)
                GltfInstance(
                    asset = asset,
                    position = Position(c.x, NUMBER_Y, c.z),
                    rotation = NUMBER_ROTATION,
                    scale = Scale(NUMBER_SCALE),
                    onCreate = {
                        val rm = engine.getRenderableManager()
                        instance.getEntities().forEach { entity ->
                            if (!rm.hasComponent(entity)) return@forEach
                            val ri = rm.getInstance(entity)
                            for (p in 0 until rm.getPrimitiveCount(ri)) {
                                rm.setMaterialInstanceAt(ri, p, whiteNumber)
                            }
                        }
                    },
                )
            }

            // Settlements / cities.
            state.buildings.forEach { b ->
                val p = HexMath.vertexCenter(b.vertex, HEX_SIZE)
                val color = ResourceColors.forPlayer(b.owner, state.players)
                val inst = rememberMaterialInstance(solidMat, engine = engine).apply {
                    setParameter("baseColor", color.x, color.y, color.z)
                }
                Cube(
                    material = inst,
                    position = Position(p.x, BUILDING_Y, p.z),
                    size = if (b.kind.yield >= 2) 0.34f else 0.24f,
                )
            }

            // Roads.
            state.roads.forEach { road ->
                val p = HexMath.edgeCenter(road.edge, HEX_SIZE)
                val angle = HexMath.edgeAngleY(road.edge, HEX_SIZE)
                val color = ResourceColors.forPlayer(road.owner, state.players)
                val inst = rememberMaterialInstance(solidMat, engine = engine).apply {
                    setParameter("baseColor", color.x, color.y, color.z)
                }
                Cube(
                    material = inst,
                    position = Position(p.x, ROAD_Y, p.z),
                    rotation = remember(angle) {
                        Quaternion.fromAxisAngle(Direction(0f, 1f, 0f), angle * RAD_TO_DEG)
                    },
                    scale = Scale(0.5f, 0.08f, 0.12f),
                )
            }

            // --- Ghost markers for legal placements (opaque -> pickable) ---
            ghostSettlements.forEach { vertex ->
                val p = HexMath.vertexCenter(vertex, HEX_SIZE)
                val inst = rememberMaterialInstance(solidMat, engine = engine).apply {
                    setParameter("baseColor", ghostColor.x, ghostColor.y, ghostColor.z)
                }
                Cube(
                    material = inst,
                    position = Position(p.x, BUILDING_Y, p.z),
                    size = 0.26f,
                    onCreate = { entityToVertex[it] = vertex },
                )
            }
            ghostRoads.forEach { edge ->
                val p = HexMath.edgeCenter(edge, HEX_SIZE)
                val angle = HexMath.edgeAngleY(edge, HEX_SIZE)
                val inst = rememberMaterialInstance(solidMat, engine = engine).apply {
                    setParameter("baseColor", ghostColor.x, ghostColor.y, ghostColor.z)
                }
                Cube(
                    material = inst,
                    position = Position(p.x, ROAD_Y, p.z),
                    rotation = remember(angle) {
                        Quaternion.fromAxisAngle(Direction(0f, 1f, 0f), angle * RAD_TO_DEG)
                    },
                    scale = Scale(0.5f, 0.08f, 0.12f),
                    onCreate = { entityToEdge[it] = edge },
                )
            }

            // Ghost markers for upgradeable settlements -> cities (city-sized cube
            // sitting over your existing settlement).
            ghostCities.forEach { vertex ->
                val p = HexMath.vertexCenter(vertex, HEX_SIZE)
                val inst = rememberMaterialInstance(solidMat, engine = engine).apply {
                    setParameter("baseColor", ghostColor.x, ghostColor.y, ghostColor.z)
                }
                Cube(
                    material = inst,
                    position = Position(p.x, BUILDING_Y, p.z),
                    size = 0.36f,
                    onCreate = { entityToVertex[it] = vertex },
                )
            }
        }
    }
}

// Issues a Filament pick at a Compose tap offset, mapping layout pixels to the
// viewport and flipping Y (Filament's viewport origin is bottom-left). Calls
// [onEntity] with the picked renderable entity id (0 if nothing was hit).
private fun pickAt(
    offset: Offset,
    viewState: FilamentViewState,
    boxSize: IntSize,
    onEntity: (Int) -> Unit,
) {
    val v = viewState.view ?: return
    val vw = v.viewport.width
    val vh = v.viewport.height
    val px = (offset.x * vw / boxSize.width.coerceAtLeast(1)).toInt()
    val py = vh - (offset.y * vh / boxSize.height.coerceAtLeast(1)).toInt()
    viewState.pick(px, py) { result -> onEntity(result.renderable) }
}
