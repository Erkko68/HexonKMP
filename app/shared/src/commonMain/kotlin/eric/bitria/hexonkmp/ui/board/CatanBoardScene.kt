package eric.bitria.hexonkmp.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import eric.bitria.hexonkmp.core.game.model.GameState
import hexonkmp.app.shared.generated.resources.Res
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.LightManager
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.compose.FilamentSceneView
import io.github.erkko68.filament.compose.scene.Color
import io.github.erkko68.filament.compose.scene.Direction
import io.github.erkko68.filament.compose.scene.Light
import io.github.erkko68.filament.compose.scene.Position
import io.github.erkko68.filament.compose.scene.Projection
import io.github.erkko68.filament.compose.scene.SkyboxSource
import io.github.erkko68.filament.compose.scene.primitives.Cube
import io.github.erkko68.filament.compose.scene.rememberCameraState
import io.github.erkko68.filament.compose.scene.rememberMaterial
import io.github.erkko68.filament.compose.scene.rememberMaterialInstance
import io.github.erkko68.filament.compose.scene.rememberSkyboxState
import io.github.erkko68.filament.utils.Quaternion
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val HEX_SIZE = 1f
private const val BUILDING_Y = 0.15f
private const val ROAD_Y = 0.06f

// Renders the authoritative GameState as a 3D Catan board: a colored hexagon per
// tile, cubes for settlements/cities, thin cubes for roads. Angled orthographic
// camera, framed to the board's extent and aspect-corrected so the image never
// stretches. Pure view of GameState — no game logic here.
@OptIn(ExperimentalResourceApi::class)
@Composable
fun CatanBoardScene(state: GameState, engine: Engine, modifier: Modifier = Modifier) {
    // Half-extent of the board in world units, plus a margin, so the orthographic
    // frustum frames every tile. The extra factor accounts for the camera tilt
    // foreshortening the board's depth axis.
    val halfExtent = remember(state.board.tiles, HEX_SIZE) {
        val maxR = state.board.tiles.maxOfOrNull { tile ->
            val c = HexMath.center(tile.hex, HEX_SIZE)
            maxOf(kotlin.math.abs(c.x), kotlin.math.abs(c.z))
        } ?: 4f
        (maxR + HEX_SIZE * 2.0f).toDouble()
    }

    // Angled orthographic camera: eye above and in front of the board so cubes
    // (buildings/roads) show some height, with parallel projection (no perspective
    // distortion). up = +Y for a normal upright tilt.
    val cameraState = rememberCameraState(
        eye = Position(0f, 16f, 13f),
        target = Position(0f, 0f, 0f),
        up = Direction(0f, 1f, 0f),
        projection = Projection.Orthographic(
            left = -halfExtent, right = halfExtent,
            bottom = -halfExtent, top = halfExtent,
            near = 0.1, far = 100.0,
        ),
    )
    val skybox = rememberSkyboxState(source = SkyboxSource.Color(Color(0.10f, 0.14f, 0.20f)))

    // Keep the orthographic frustum matched to the viewport aspect ratio so the
    // board is never stretched: widen (or heighten) the half-extents by aspect.
    var aspect by remember { mutableStateOf(1f) }
    cameraState.projection = if (aspect >= 1f) {
        Projection.Orthographic(
            left = -halfExtent * aspect, right = halfExtent * aspect,
            bottom = -halfExtent, top = halfExtent, near = 0.1, far = 100.0,
        )
    } else {
        Projection.Orthographic(
            left = -halfExtent, right = halfExtent,
            bottom = -halfExtent / aspect, top = halfExtent / aspect, near = 0.1, far = 100.0,
        )
    }

    // The single LIT color material, instanced per color below.
    val material: Material? = rememberMaterial(engine) {
        Res.readBytes("files/materials/board_color.filamat")
    }

    FilamentSceneView(
        modifier = modifier.onSizeChanged {
            if (it.height > 0) aspect = it.width.toFloat() / it.height.toFloat()
        },
        engine = engine,
        cameraState = cameraState,
        skyboxState = skybox,
    ) {
        Light(
            type = LightManager.Type.DIRECTIONAL,
            direction = Direction(0.4f, -1f, -0.5f),
            intensity = 90_000f,
        )

        val template = material ?: return@FilamentSceneView

        // Tiles.
        state.board.tiles.forEach { tile ->
            val c = HexMath.center(tile.hex, HEX_SIZE)
            val color = ResourceColors.forTerrain(tile.terrain)
            val inst = rememberMaterialInstance(template, engine = engine).apply {
                setParameter("baseColor", color.x, color.y, color.z)
            }
            Hexagon(
                material = inst,
                position = Position(c.x, 0f, c.z),
                radius = HEX_SIZE * 0.95f, // small gap between tiles
            )
        }

        // Settlements / cities — a cube on the vertex, tinted by owner.
        state.buildings.forEach { b ->
            val p = HexMath.vertexCenter(b.vertex, HEX_SIZE)
            val color = ResourceColors.forPlayer(b.owner, state.players)
            val inst = rememberMaterialInstance(template, engine = engine).apply {
                setParameter("baseColor", color.x, color.y, color.z)
            }
            Cube(
                material = inst,
                position = Position(p.x, BUILDING_Y, p.z),
                size = if (b.kind.yield >= 2) 0.34f else 0.24f, // city bigger than settlement
            )
        }

        // Roads — a thin, long cube laid along the edge, tinted by owner.
        state.roads.forEach { road ->
            val p = HexMath.edgeCenter(road.edge, HEX_SIZE)
            val angle = HexMath.edgeAngleY(road.edge, HEX_SIZE)
            val color = ResourceColors.forPlayer(road.owner, state.players)
            val inst = rememberMaterialInstance(template, engine = engine).apply {
                setParameter("baseColor", color.x, color.y, color.z)
            }
            Cube(
                material = inst,
                position = Position(p.x, ROAD_Y, p.z),
                rotation = remember(angle) {
                    // Rotate about Y so the cube's long axis follows the edge.
                    Quaternion.fromAxisAngle(Direction(0f, 1f, 0f), -angle * 180f / kotlin.math.PI.toFloat())
                },
                scale = io.github.erkko68.filament.compose.scene.Scale(0.5f, 0.08f, 0.12f),
            )
        }
    }
}
