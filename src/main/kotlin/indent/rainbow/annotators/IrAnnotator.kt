package indent.rainbow.annotators

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import indent.rainbow.settings.IrConfig

enum class IrAnnotatorType {
    SIMPLE,
    SIMPLE_WITHOUT_PSI,
    FORMATTER_SEQUENTIAL,
    FORMATTER_INCREMENTAL,
}

fun IrConfig.isAnnotatorEnabled(annotatorType: IrAnnotatorType, element: PsiElement): Boolean {
    return isAnnotatorEnabled(annotatorType, element, element.containingFile)
}

fun IrConfig.isAnnotatorEnabled(annotatorType: IrAnnotatorType, element: PsiElement?, containingFile: PsiFile): Boolean {
    if (!enabled) return false
    if (!(containingFile.isWritable || isEnabledForReadOnlyFiles)) return false

    val annotatorTypeExpected = getAnnotatorTypeForFile(containingFile)
    if (annotatorType != annotatorTypeExpected) return false

    return if (annotatorTypeExpected == IrAnnotatorType.SIMPLE_WITHOUT_PSI) {
        check(element != null)
        element == containingFile
    } else {
        element == null || isAnnotatorEnabled(element)
    }
}

fun isAnnotatorEnabled(element: PsiElement): Boolean {
    // we can't check `element is PsiWhiteSpace`, because e.g. in Yaml custom LeafPsiElement is used
    return element.text.isBlank()
}

private fun IrConfig.getAnnotatorTypeForFile(file: PsiFile): IrAnnotatorType {
    return if (file.isSingleNodeFile()) IrAnnotatorType.SIMPLE_WITHOUT_PSI else annotatorType
}

// This function is needed to detect two types of languages:
// 1. Language which doesn't have plugin yet, but has syntax highlighting thanks to textmate — https://plugins.jetbrains.com/plugin/7221-textmate-bundles
// 2. Language which doesn't implement proper PSI structure yet
//    Like F# — see https://github.com/izhangzhihao/intellij-rainbow-brackets/issues/186#issuecomment-609733425 for details
fun PsiFile.isSingleNodeFile(): Boolean {
    val firstChild = firstChild
    return firstChild != null && firstChild.nextSibling == null && firstChild.firstChild == null
}
