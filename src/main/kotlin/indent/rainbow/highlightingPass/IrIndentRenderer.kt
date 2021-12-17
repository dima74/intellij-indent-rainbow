package indent.rainbow.highlightingPass

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.view.EditorPainter
import com.intellij.openapi.editor.impl.view.VisualLinesIterator
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import indent.rainbow.IrColors
import java.awt.Graphics

/** Paints one [IndentDescriptor]. */
class IrHighlighterRenderer(
    private var level: Int,
    private var taKey: TextAttributesKey,
    private val indentSize: Int,
) : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        val startPosition = editor.offsetToVisualPosition(highlighter.startOffset)
        val endPosition = editor.offsetToVisualPosition(highlighter.endOffset)

        if (level != -1 && endPosition.column - startPosition.column != indentSize) return

        val isOneLine = startPosition.line == endPosition.line
        if (isOneLine && startPosition.column == endPosition.column) return

        g.color = editor.colorsScheme.getAttributes(taKey).backgroundColor
        val indentGuideShift = EditorPainter.getIndentGuideShift(editor)
        if (isOneLine || !editor.hasSoftWraps()) {
            editor.paintIndent(startPosition, endPosition, indentGuideShift, g)
        } else {
            paintBetweenSoftWraps(editor, startPosition, endPosition, indentGuideShift, g)
        }
    }

    private fun paintBetweenSoftWraps(
        editor: Editor,
        startPosition: VisualPosition,
        endPosition: VisualPosition,
        indentGuideShift: Int,
        g: Graphics
    ) {
        fun paintOneBetweenSoftWraps(previousSoftWrap: Int, currentSoftWrap: Int) {
            if (previousSoftWrap + 1 == currentSoftWrap) return
            val indentLineStart = previousSoftWrap + 1
            val indentLineEnd = currentSoftWrap - 1
            editor.paintIndent(
                VisualPosition(indentLineStart, startPosition.column),
                VisualPosition(indentLineEnd, endPosition.column),
                indentGuideShift,
                g
            )
        }

        val iterator = VisualLinesIterator(editor as EditorImpl, startPosition.line)
        var previousSoftWrap = startPosition.line - 1
        while (!iterator.atEnd() && iterator.visualLine <= endPosition.line) {
            if (iterator.startsWithSoftWrap()) {
                val currentSoftWrap = iterator.visualLine
                paintOneBetweenSoftWraps(previousSoftWrap, currentSoftWrap)
                previousSoftWrap = currentSoftWrap
            }
            iterator.advance()
        }
        paintOneBetweenSoftWraps(previousSoftWrap, endPosition.line + 1)
    }

    private fun Editor.paintIndent(start: VisualPosition, end: VisualPosition, indentGuideShift: Int, g: Graphics) {
        val startXY = visualPositionToXY(start)
        val endXY = visualPositionToXY(end)

        val indentGuideWidth = 1
        val left = startXY.x + if (level <= 0) 0 else (indentGuideShift + indentGuideWidth)
        val top = startXY.y
        val right = endXY.x + indentGuideShift
        val bottom = endXY.y + lineHeight
        g.fillRect(left, top, right - left, bottom - top)
    }

    fun setLevel(level: Int) {
        this.level = level
        taKey = IrColors.getTextAttributes(level)
    }
}

private fun Editor.hasSoftWraps(): Boolean =
    (this as EditorEx).softWrapModel.registeredSoftWraps.isNotEmpty()
