package indent.rainbow

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import indent.rainbow.highlightingPass.LineIndent

val PsiFile.document: Document?
    get() = PsiDocumentManager.getInstance(project).getDocument(this)

fun Int.ifNotPositive(provider: () -> Int): Int = if (this > 0) this else provider()

inline fun <T> Array<out T>.mapToIndentArray(transform: (T, T) -> LineIndent): Array<LineIndent> = Array(size) { i -> transform(this[i], if (i > 0) this[i - 1] else this[i]) }
