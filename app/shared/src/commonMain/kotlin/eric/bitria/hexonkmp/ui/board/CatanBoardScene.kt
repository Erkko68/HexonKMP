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

    // Angled orthographic camera driven by our own BoardCameraState (pan + zoom,
    // fixed tilt). The eye/target/projection are computed there so board tiles
    // and cube buildings share one consistent parallel projection.
    val cameraState = rememberCameraState(up = Direction(0f, 1f, 0f))
    val camera = rememberBoardCameraState(cameraState, baseHalfExtent = halfExtent.toFloat())
    val skybox = rememberSkyboxState(source = SkyboxSource.Color(Color(0.10f, 0.14f, 0.20f)))

    var viewportHeight by remember { mutableStateOf(1) }

    // The single LIT color material, instanced per color below.
    val material: Material? = rememberMaterial(engine) {
        Res.readBytes("files/materials/board_color.filamat")
    }

    FilamentSceneView(
        modifier = modifier
            .onSizeChanged {
                viewportHeight = it.height
                camera.setViewport(it.width, it.height)
            }
            .boardGestures(camera, viewportHeight = { viewportHeight }),
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
