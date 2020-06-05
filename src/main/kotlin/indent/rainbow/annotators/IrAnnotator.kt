package indent.rainbow.annotators

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import indent.rainbow.settings.IrConfig

enum class IrAnnotatorType {
    SIMPLE,
    SIMPLE_WITHOUT_PSI,
    FORMATTER_SEQUENTIAL,
    FORMATTER_INCREMENTAL,
}

fun IrConfig.isAnnotatorEnabled(annotatorType: IrAnnotatorType, file: PsiFile): Boolean =
    enabled
            && (file.isWritable || isEnabledForReadOnlyFiles)
            && annotatorType == getAnnotatorTypeForFile(file)

// we can't check `element is PsiWhiteSpace`, because e.g. in Yaml custom LeafPsiElement is used
fun PsiElement.isWhiteSpace(): Boolean = text.isBlank()

// We want highlighting to "Cut through multiline strings": https://github.com/dima74/intellij-indent-rainbow/issues/9
// Multiline strings usually implement `PsiLanguageInjectionHost`
fun PsiElement.isCommentOrInjectedHost(): Boolean = this is PsiComment || this is PsiLanguageInjectionHost

private fun IrConfig.getAnnotatorTypeForFile(file: PsiFile): IrAnnotatorType =
    if (file.isSingleNodeFile()) IrAnnotatorType.SIMPLE_WITHOUT_PSI else annotatorType

// This function is needed to detect two types of languages:
// 1. Language which doesn't have plugin yet,
//    but has syntax highlighting thanks to textmate — https://plugins.jetbrains.com/plugin/7221-textmate-bundles
// 2. Language which doesn't implement proper PSI structure yet
//    Like F# — see https://github.com/izhangzhihao/intellij-rainbow-brackets/issues/186#issuecomment-609733425 for details
fun PsiFile.isSingleNodeFile(): Boolean {
    val firstChild = firstChild
    return firstChild != null && firstChild.nextSibling == null && firstChild.firstChild == null
}
