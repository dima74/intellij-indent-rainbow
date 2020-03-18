package indent.rainbow.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import indent.rainbow.IrFormatterAnnotatorImpl
import indent.rainbow.settings.IrConfig

class IrFormatterIncrementalAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!config.isAnnotatorEnabled(IrAnnotatorType.FORMATTER_INCREMENTAL, element)) return

        val project = element.project
        val file = element.containingFile
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val elementLines = getElementLinesRange(element, document)

        val formatterAnnotatorImpl = getOrCreateFormatterAnnotatorImpl(file, document, holder) ?: return
        formatterAnnotatorImpl.runForLines(elementLines)
    }

    private fun getElementLinesRange(element: PsiElement, document: Document): IntRange {
        val range = element.textRange
        var startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber(range.endOffset)
        if (document.getLineStartOffset(startLine) != range.startOffset) {
            ++startLine
        }
        return startLine..endLine
    }

    private fun getOrCreateFormatterAnnotatorImpl(file: PsiFile, document: Document, holder: AnnotationHolder): IrFormatterAnnotatorImpl? {
        val session = holder.currentAnnotationSession
        var formatterAnnotatorImpl = session.getUserData(FORMATTER_ANNOTATOR_KEY)
        if (formatterAnnotatorImpl != null) return formatterAnnotatorImpl

        formatterAnnotatorImpl = IrFormatterAnnotatorImpl.getInstance(file, document, holder)
        session.putUserData(FORMATTER_ANNOTATOR_KEY, formatterAnnotatorImpl)
        return formatterAnnotatorImpl
    }

    companion object {
        val INSTANCE: IrFormatterIncrementalAnnotator = IrFormatterIncrementalAnnotator()
        private val config: IrConfig = IrConfig.INSTANCE
        private val FORMATTER_ANNOTATOR_KEY: Key<IrFormatterAnnotatorImpl> =
            Key("INDENT_RAINBOW_FORMATTER_ANNOTATOR_KEY")
    }
}
