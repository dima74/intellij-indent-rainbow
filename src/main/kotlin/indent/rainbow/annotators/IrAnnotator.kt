package indent.rainbow.annotators

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.util.PatternUtil
import indent.rainbow.settings.IrConfig

enum class IrAnnotatorType {
    SIMPLE,
    SIMPLE_WITHOUT_PSI,
    FORMATTER_INCREMENTAL,
}

fun IrConfig.isAnnotatorEnabled(file: PsiFile): Boolean =
    enabled
            && matchesFileMask(fileMasks, file.name)
            && (file.isWritable || isEnabledForReadOnlyFiles)

private fun matchesFileMask(fileMasks: String, fileName: String): Boolean {
    val masks = fileMasks.trim()
    if (masks == "*") return true
    return masks.split(";").any {
        val mask = it.trim()
        mask.isNotEmpty() && PatternUtil.fromMask(mask).matcher(fileName).matches()
    }
}

// we can't check `element is PsiWhiteSpace`, because e.g. in Yaml custom LeafPsiElement is used
fun PsiElement.isWhiteSpace(): Boolean {
    val text = text ?: return false
    return text.isBlank()
}

// We want highlighting to "Cut through multiline strings": https://github.com/dima74/intellij-indent-rainbow/issues/9
// Multiline strings usually implement `PsiLanguageInjectionHost`
fun PsiElement.isCommentOrInjectedHost(): Boolean = this is PsiComment || this is PsiLanguageInjectionHost

fun IrConfig.getAnnotatorTypeForFile(file: PsiFile): IrAnnotatorType =
    when {
        file.isSingleNodeFile() -> IrAnnotatorType.SIMPLE_WITHOUT_PSI
        useSimpleHighlighter && matchesFileMask(simpleHighlighterFileMasks, file.name) -> IrAnnotatorType.SIMPLE
        else -> IrAnnotatorType.FORMATTER_INCREMENTAL
    }

// This function is needed to detect two types of languages:
// 1. Language which doesn't have plugin yet,
//    but has syntax highlighting thanks to textmate — https://plugins.jetbrains.com/plugin/7221-textmate-bundles
// 2. Language which doesn't implement proper PSI structure yet
//    Like F# — see https://github.com/izhangzhihao/intellij-rainbow-brackets/issues/186#issuecomment-609733425 for details
fun PsiFile.isSingleNodeFile(): Boolean {
    val firstChild = firstChild
    return firstChild != null && firstChild.nextSibling == null && firstChild.firstChild == null
}
