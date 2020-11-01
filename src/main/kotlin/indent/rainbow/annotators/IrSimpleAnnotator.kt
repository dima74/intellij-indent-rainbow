package indent.rainbow.annotators

import com.intellij.application.options.CodeStyle
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import indent.rainbow.IrColors
import indent.rainbow.settings.IrConfig
import indent.rainbow.settings.document

class IrSimpleAnnotator {

    fun annotate(element: PsiElement, holder: AnnotationHolder, asFallback: Boolean) {
        if (!element.isWhiteSpace()) return

        val file = element.containingFile
        val document = file.document ?: return

        val indentOptions = CodeStyle.getIndentOptions(file)
        val useTabs = indentOptions.USE_TAB_CHARACTER
        val tabSize = indentOptions.TAB_SIZE

        val range = element.textRange
        var lineStart = document.getLineNumber(range.startOffset)
        val lineEnd = document.getLineNumber(range.endOffset)
        if (document.getLineStartOffset(lineStart) != range.startOffset) {
            ++lineStart
        }

        for (line in lineStart..lineEnd) {
            val highlightStartOffset = document.getLineStartOffset(line)
            var highlightEndOffset = if (line < lineEnd) {
                document.getLineEndOffset(line)
            } else {
                range.endOffset
            }
            // Я не смог придумать пример когда строго больше, но было несколько репортов из-за этого в Sentry
            if (highlightStartOffset >= highlightEndOffset) continue

            val highlightText = document.getText(TextRange(highlightStartOffset, highlightEndOffset))
            val okSpaces = !useTabs
                    && highlightText.chars().allMatch { it == ' '.toInt() }
                    && (highlightEndOffset - highlightStartOffset) % tabSize == 0
            val okTabs = useTabs && highlightText.chars().allMatch { it == '\t'.toInt() }
            val disableErrorHighlighting = config.disableErrorHighlighting || asFallback
                    || PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null
            if (okSpaces || okTabs || disableErrorHighlighting) {
                if (disableErrorHighlighting && !useTabs) {
                    highlightEndOffset -= (highlightEndOffset - highlightStartOffset) % tabSize
                }

                val step = if (useTabs) 1 else tabSize
                for (i in highlightStartOffset until highlightEndOffset step step) {
                    val textAttributes = IrColors.getTextAttributes((i - highlightStartOffset) / step)
                    highlight(holder, i, i + step, textAttributes)
                }
            } else {
                highlight(holder, highlightStartOffset, highlightEndOffset, IrColors.getErrorTextAttributes())
            }
        }
    }

    private fun highlight(holder: AnnotationHolder, start: Int, end: Int, textAttributes: TextAttributesKey) {
        val highlightRange = TextRange(start, end)
        // 2018.2
        @Suppress("DEPRECATION") val annotation = holder.createInfoAnnotation(highlightRange, null)
        annotation.textAttributes = textAttributes
    }

    companion object {
        val INSTANCE: IrSimpleAnnotator = IrSimpleAnnotator()
        private val config = IrConfig.INSTANCE
    }
}
