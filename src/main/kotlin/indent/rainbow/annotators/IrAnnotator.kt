package indent.rainbow.annotators

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import indent.rainbow.document
import indent.rainbow.settings.IrConfig
import indent.rainbow.settings.cachedData
import java.util.regex.Pattern

enum class IrAnnotatorType {
    SIMPLE_HIGHLIGHTING_PASS,
    FORMATTER_INCREMENTAL,
}

fun IrConfig.isAnnotatorEnabled(file: PsiFile): Boolean =
    enabled
            && matchesFileMask(cachedData.fileMasks, file.name)
            && (file.isWritable || isEnabledForReadOnlyFiles)
            && !(disableOnBigFiles && file.hasMoreLinesThan(bigFilesLineThreshold))

fun IrConfig.isAnnotatorEnabled(file: PsiFile, type: IrAnnotatorType): Boolean =
    isAnnotatorEnabled(file) && getAnnotatorTypeForFile(file) == type

private fun PsiFile.hasMoreLinesThan(count: Int): Boolean {
    val document = document ?: return false
    return document.lineCount > count
}

private fun matchesFileMask(fileMasks: List<Pattern>?, fileName: String): Boolean {
    if (fileMasks == null) return true
    return fileMasks.any { it.matcher(fileName).matches() }
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
    if (useFormatterHighlighter && matchesFileMask(cachedData.formatterHighlighterFileMasks, file.name)) {
        IrAnnotatorType.FORMATTER_INCREMENTAL
    } else {
        IrAnnotatorType.SIMPLE_HIGHLIGHTING_PASS
    }
