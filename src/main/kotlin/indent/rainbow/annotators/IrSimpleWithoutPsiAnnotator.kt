package indent.rainbow.annotators

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.impl.FileStatusMap
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import indent.rainbow.IrAnnotatorImpl

class IrSimpleWithoutPsiAnnotator {

    fun annotate(file: PsiFile, holder: AnnotationHolder) {
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return
        val dirtyTextRange = getDirtyTextRange(file, document) ?: file.textRange

        val annotatorImpl = IrAnnotatorImpl.getInstance(file, document, holder, false) ?: return
        annotatorImpl.runForTextRange(dirtyTextRange)
    }

    // unfortunately usually not working for files with one node (this annotator is used only for such files)
    private fun getDirtyTextRange(file: PsiFile, document: Document): TextRange? {
        val editors = EditorFactory.getInstance().getEditors(document, file.project)
        val editor = editors.firstOrNull() ?: return null
        return FileStatusMap.getDirtyTextRange(editor, Pass.UPDATE_ALL)
    }

    companion object {
        val INSTANCE: IrSimpleWithoutPsiAnnotator = IrSimpleWithoutPsiAnnotator()
    }
}
