package indent.rainbow.annotators

import com.intellij.psi.PsiElement
import indent.rainbow.settings.IrConfig

enum class IrAnnotatorType {
    SIMPLE,
    FORMATTER_SEQUENTIAL,
    FORMATTER_INCREMENTAL,
}

fun IrConfig.isAnnotatorEnabled(annotator: IrAnnotatorType, element: PsiElement?): Boolean {
    return enabled
            && annotator == annotatorType
            && (element == null || isAnnotatorEnabled(element))
}

fun IrConfig.isAnnotatorEnabled(element: PsiElement): Boolean {
    // we can't check `element is PsiWhiteSpace`, because e.g. in Yaml custom LeafPsiElement is used
    return element.text.isBlank() && element.isWritable
}
