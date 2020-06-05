package indent.rainbow.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import indent.rainbow.IrAnnotatorImpl
import indent.rainbow.settings.document

class IrFormatterIncrementalAnnotator {

    fun tryAnnotate(element: PsiElement, holder: AnnotationHolder): Boolean {
        val file = element.containingFile
        val document = file.document ?: return false

        val annotatorImpl = getOrCreateAnnotatorImpl(file, document, holder) ?: return false
        annotatorImpl.runForElement(element)
        return true
    }

    private fun getOrCreateAnnotatorImpl(file: PsiFile, document: Document, holder: AnnotationHolder): IrAnnotatorImpl? {
        val session = holder.currentAnnotationSession
        var annotatorImpl = session.getUserData(ANNOTATOR_IMPL_KEY)
        if (annotatorImpl != null) return annotatorImpl

        annotatorImpl = IrAnnotatorImpl.getInstance(file, document, holder, true)
        session.putUserData(ANNOTATOR_IMPL_KEY, annotatorImpl)
        return annotatorImpl
    }

    companion object {
        val INSTANCE: IrFormatterIncrementalAnnotator = IrFormatterIncrementalAnnotator()
        private val ANNOTATOR_IMPL_KEY: Key<IrAnnotatorImpl> = Key("INDENT_RAINBOW_ANNOTATOR_IMPL_KEY")
    }
}
