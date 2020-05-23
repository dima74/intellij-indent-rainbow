package indent.rainbow.settings

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

val PsiFile.document: Document?
    get() = PsiDocumentManager.getInstance(project).getDocument(this)
