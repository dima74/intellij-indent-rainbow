package indent.rainbow.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import indent.rainbow.IrFormatterAnnotatorImpl
import indent.rainbow.LOG
import indent.rainbow.settings.IrConfig

@Suppress("RedundantUnitReturnType", "RedundantUnitExpression")
class IrFormatterAnnotator : ExternalAnnotator<Unit, Unit>(), DumbAware {

    override fun collectInformation(file: PsiFile): Unit {
        return Unit
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): Unit {
        return collectInformation(file)
    }

    override fun doAnnotate(collectedInfo: Unit): Unit {
        return Unit
    }

    override fun apply(file: PsiFile, annotationResult: Unit, holder: AnnotationHolder) {
        if (!config.enabled || !config.useFormatterBasedAnnotator || config.useIncrementalHighlighter) return

        LOG.info("IrExternalAnnotator::apply")

        val project = file.project
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val formatterAnnotatorImpl = IrFormatterAnnotatorImpl.getInstance(file, document, holder) ?: return
        formatterAnnotatorImpl.runForAllLines()
    }

    companion object {
        val instance: IrFormatterAnnotator = IrFormatterAnnotator()
        private val config = IrConfig.instance
    }
}
