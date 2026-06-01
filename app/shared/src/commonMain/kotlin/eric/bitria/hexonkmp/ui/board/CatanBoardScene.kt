package eric.bitria.hexonkmp.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import eric.bitria.hexonkmp.core.game.model.GameState
import hexonkmp.app.shared.generated.resources.Res
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.LightManager
import io.github.erkko68.filament.Material
import io.github.erkko68.filament.compose.FilamentSceneView
import io.github.erkko68.filament.compose.mapGestures
import io.github.erkko68.filament.compose.rememberMapCameraState
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
// tile, cubes for settlements/cities, thin cubes for roads. Top-down MAP camera
// (pan + zoom, no rotation) — the natural fit for a board game. Pure view of
// GameState — no game logic here.
@OptIn(ExperimentalResourceApi::class)
@Composable
fun CatanBoardScene(state: GameState, engine: Engine, modifier: Modifier = Modifier) {
    // Look straight down the Y axis at the board. MAP mode pairs with an
    // orthographic projection so tiles keep a constant on-screen size.
    val cameraState = rememberCameraState(
        eye = Position(0f, 12f, 0f),
        target = Position(0f, 0f, 0f),
        up = Direction(0f, 0f, -1f), // with a top-down eye, "up" on screen is -Z
        projection = Projection.Orthographic(
            left = -6.0, right = 6.0, bottom = -6.0, top = 6.0, near = 0.1, far = 100.0,
        ),
    )
    val map = rememberMapCameraState(cameraState)
    val skybox = rememberSkyboxState(source = SkyboxSource.Color(Color(0.10f, 0.14f, 0.20f)))

    // The single LIT color material, instanced per color below.
    val material: Material? = rememberMaterial(engine) {
        Res.readBytes("files/materials/board_color.filamat")
    }

    FilamentSceneView(
        modifier = modifier
            .onSizeChanged { map.setViewport(it.width, it.height) }
            .mapGestures(map),
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
