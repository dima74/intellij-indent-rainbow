package indent.rainbow

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

val PsiFile.document: Document?
    get() = PsiDocumentManager.getInstance(project).getDocument(this)

fun Int.ifNotPositive(provider: () -> Int): Int = if (this > 0) this else provider()

inline fun <T> Array<out T>.mapToIntArray(transform: (T) -> Int): IntArray = IntArray(size) { i -> transform(this[i]) }
