package eric.bitria.hexon.ui.utils

import androidx.compose.ui.graphics.Path
import io.aimei.wk.model.PathCommand
import io.aimei.wk.model.PathCommands

/**
 * Converts PathCommands to Compose UI Path
 */
object PathRenderer {

    /**
     * Create a Compose Path from PathCommands
     */
    fun createPath(commands: PathCommands): Path {
        return createPath(commands.commands)
    }

    /**
     * Create a Compose Path from a list of PathCommand
     */
    fun createPath(commands: List<PathCommand>): Path {
        val path = Path()

        for (command in commands) {
            when (command) {
                is PathCommand.MoveTo -> {
                    path.moveTo(command.x, command.y)
                }
                is PathCommand.LineTo -> {
                    path.lineTo(command.x, command.y)
                }
                is PathCommand.CubicTo -> {
                    path.cubicTo(
                        command.x1, command.y1,
                        command.x2, command.y2,
                        command.x, command.y
                    )
                }
                is PathCommand.QuadTo -> {
                    path.quadraticBezierTo(
                        command.x1, command.y1,
                        command.x, command.y
                    )
                }
                is PathCommand.Close -> {
                    path.close()
                }
            }
        }

        return path
    }
}