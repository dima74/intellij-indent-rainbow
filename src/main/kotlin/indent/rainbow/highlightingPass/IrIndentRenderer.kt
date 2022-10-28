package indent.rainbow.highlightingPass

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.view.EditorPainter
import com.intellij.openapi.editor.impl.view.VisualLinesIterator
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import indent.rainbow.getColorWithAdjustedAlpha
import indent.rainbow.settings.IrConfig
import java.awt.Graphics

/** Paints one [IndentDescriptor]. */
class IrHighlighterRenderer(
    private var level: Int,
    private var indentSize: Int,
) : CustomHighlighterRenderer {

    private val config: IrConfig? = serviceOrNull()

    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
        val startPosition = editor.offsetToVisualPosition(highlighter.startOffset)
        val endPosition = editor.offsetToVisualPosition(highlighter.endOffset)

        val topRightOffset = editor.visualPositionToOffset(VisualPosition(startPosition.line, endPosition.column))
        if (level != -1 && topRightOffset - highlighter.startOffset != indentSize) return

        val isOneLine = startPosition.line == endPosition.line
        if (isOneLine && startPosition.column == endPosition.column) return

        if (config == null) return
        g.color = config.getColorWithAdjustedAlpha(level, editor)

        val indentGuideShift = alignWithIndentGuides ? EditorPainter.getIndentGuideShift(editor) : 0
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
        val width = right - left
        val height = bottom - top

        val cornerRadius = config?.cornerRadius ?: 0
        when {
            cornerRadius <= 0 ->
                g.fillRect(left, top, width, height)

            config!!.applyRadiusToBothSides ->
                g.fillRoundRect(left, top, width, height, cornerRadius, cornerRadius)

            else ->
                g.fillRectangleWithRoundedRightSide(left, top, width, height, cornerRadius)
        }
    }

    private fun Graphics.fillRectangleWithRoundedRightSide(left: Int, top: Int, width: Int, height: Int, cornerRadius: Int) {
        // Top right corner
        fillArc(left + width - cornerRadius, top, cornerRadius, cornerRadius, 0, 90)
        // Bottom right corner
        fillArc(
            left + width - cornerRadius,
            top + height - cornerRadius,
            cornerRadius,
            cornerRadius,
            90 + 180,
            90
        )
        // Part between corners
        fillRect(
            left + cornerRadius / 2 + width - cornerRadius,
            top + cornerRadius / 2,
            cornerRadius / 2,
            height - cornerRadius
        )
        // The rest area
        fillRect(left, top, width - cornerRadius / 2, height)
    }

    fun updateFrom(descriptor: IndentDescriptor) {
        level = descriptor.level
        indentSize = descriptor.indentSize
    }
}

private fun Editor.hasSoftWraps(): Boolean =
    (this as EditorEx).softWrapModel.registeredSoftWraps.isNotEmpty()
