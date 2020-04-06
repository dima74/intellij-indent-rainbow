package indent.rainbow.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import indent.rainbow.settings.IrConfig

class IrAnnotatorProxy : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (config.isAnnotatorEnabled(IrAnnotatorType.FORMATTER_INCREMENTAL, element)) {
            val success = IrFormatterIncrementalAnnotator.INSTANCE.tryAnnotate(element, holder)
            if (!success) {
                IrSimpleAnnotator.INSTANCE.annotate(element, holder, true)
            }
        } else if (config.isAnnotatorEnabled(IrAnnotatorType.SIMPLE, element)) {
            IrSimpleAnnotator.INSTANCE.annotate(element, holder, false)
        }
    }

    companion object {
        val INSTANCE: IrAnnotatorProxy = IrAnnotatorProxy()
        private val config = IrConfig.INSTANCE
    }
}
