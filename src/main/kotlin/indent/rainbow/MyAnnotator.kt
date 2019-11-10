package indent.rainbow

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

class MyAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiWhiteSpace) return

        val project = element.project
        val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile) ?: return
        val editor = EditorFactory.getInstance().getEditors(document).firstOrNull() ?: return
        val tabSize = EditorUtil.getTabSize(editor)

        val range = element.textRange
        var startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber(range.endOffset)
        if (document.getLineStartOffset(startLine) != range.startOffset) {
            ++startLine
        }

        for (line in startLine..endLine) {
            val highlightStartOffset = document.getLineStartOffset(line)
            val highlightEndOffset = if (line < endLine) {
                document.getLineEndOffset(line)
            } else {
                range.endOffset
            }
            if (highlightStartOffset == highlightEndOffset) continue

            if ((highlightEndOffset - highlightStartOffset) % tabSize != 0) {
                highlight(holder, highlightStartOffset, highlightEndOffset, MyColors.errorTextAttributesKey)
            } else {
                for (i in highlightStartOffset until highlightEndOffset step tabSize) {
                    val textAttributes = MyColors.getTextAttributes((i - highlightStartOffset) / tabSize)
                    highlight(holder, i, i + tabSize, textAttributes)
                }
            }
        }
    }

    private fun highlight(holder: AnnotationHolder, start: Int, end: Int, textAttributes: TextAttributesKey) {
        val highlightRange = TextRange(start, end)
        val annotation = holder.createInfoAnnotation(highlightRange, null)
        annotation.textAttributes = textAttributes
    }

    companion object {
        val instance: MyAnnotator = MyAnnotator()
    }
}
