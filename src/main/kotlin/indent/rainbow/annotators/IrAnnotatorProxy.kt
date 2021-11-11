package indent.rainbow.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import indent.rainbow.LOG
import indent.rainbow.settings.IrConfig

class IrAnnotatorProxy : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!IrConfig.isInitialized) {
            LOG.warn("IrConfig is not initialized")
            return
        }
        val file = element.containingFile
        if (!config.isAnnotatorEnabled(file)) return
        when (config.getAnnotatorTypeForFile(file)) {
            IrAnnotatorType.FORMATTER_INCREMENTAL -> {
                val success = IrFormatterIncrementalAnnotator.INSTANCE.tryAnnotate(element, holder)
                if (!success) {
                    IrSimpleAnnotator.INSTANCE.annotate(element, holder, true)
                }
            }
            IrAnnotatorType.SIMPLE -> {
                IrSimpleAnnotator.INSTANCE.annotate(element, holder, false)
            }
            IrAnnotatorType.SIMPLE_WITHOUT_PSI -> {
                IrSimpleWithoutPsiAnnotator.INSTANCE.annotate(element, holder)
            }
        }
    }

    companion object {
        val INSTANCE: IrAnnotatorProxy = IrAnnotatorProxy()
        private val config = IrConfig.INSTANCE
    }
}
