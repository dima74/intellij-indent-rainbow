package indent.rainbow.highlightingPass

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

/**
 * Paints all selections.
 * We need to repaint selections so that Indent rainbows don't hide it.
 */
object IrSelectionsRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        val painter = IrSelectionPainter(editor, g as Graphics2D)
        for (caret in editor.caretModel.allCarets) {
            val start = caret.selectionStart
            val end = caret.selectionEnd
            if (start >= end) return
            painter.paintSelection(start, end)
        }
    }
}

private class IrSelectionPainter(private val editor: Editor, private val g: Graphics2D) {
    private val color: Color? = editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
    private val lineHeight: Int = editor.lineHeight
    private val editorWidth: Int = g.clipBounds.width

    fun paintSelection(startOffset: Int, endOffset: Int) {
        // visual positions take soft-wraps into account
        val startPosition = editor.offsetToVisualPosition(startOffset)
        val endPosition = editor.offsetToVisualPosition(endOffset)

        g.color = color ?: return
        if (startPosition.line == endPosition.line) {
            fillVisualOneLineRect(startPosition.line, startPosition.column, endPosition.column)
        } else {
            fillVisualFullWidthRect(startPosition.line, startPosition.line, startPosition.column)
            if (startPosition.line + 1 < endPosition.line) {
                fillVisualFullWidthRect(startPosition.line + 1, endPosition.line - 1, 0)
            }
            fillVisualOneLineRect(endPosition.line, 0, endPosition.column)
        }
    }

    private fun fillVisualOneLineRect(line: Int, column1: Int, column2: Int) {
        val startXY = editor.visualPositionToXY(VisualPosition(line, column1))
        val endXY = editor.visualPositionToXY(VisualPosition(line, column2))
        if (startXY.y == endXY.y) {
            g.fillRect(startXY.x, startXY.y, endXY.x - startXY.x, lineHeight)
        } else {
            fillFullWidthRect(startXY.x, startXY.y, lineHeight)
        }
    }

    private fun fillVisualFullWidthRect(line1: Int, line2: Int, column: Int) {
        val startXY = editor.visualPositionToXY(VisualPosition(line1, column))
        val endXY = editor.visualPositionToXY(VisualPosition(line2, column))
        if (startXY.x == endXY.x) {
            fillFullWidthRect(startXY.x, startXY.y, endXY.y - startXY.y + lineHeight)
        }
    }

    private fun fillFullWidthRect(x: Int, y: Int, height: Int) {
        g.fillRect(x, y, editorWidth - x, height)
    }
}
