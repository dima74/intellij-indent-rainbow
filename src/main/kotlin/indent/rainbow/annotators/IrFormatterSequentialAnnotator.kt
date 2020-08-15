package indent.rainbow.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiFile
import indent.rainbow.IrAnnotatorImpl
import indent.rainbow.settings.IrConfig
import indent.rainbow.settings.document

@Suppress("RedundantUnitReturnType", "RedundantUnitExpression")
class IrFormatterSequentialAnnotator : ExternalAnnotator<Unit, Unit>(), DumbAware {

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
        if (!config.isAnnotatorEnabled(IrAnnotatorType.FORMATTER_SEQUENTIAL, file)) return

        val document = file.document ?: return
        val annotatorImpl = IrAnnotatorImpl.getInstance(file, document, holder, true) ?: return
        annotatorImpl.runForAllLines()
    }

    companion object {
        val INSTANCE: IrFormatterSequentialAnnotator = IrFormatterSequentialAnnotator()
        private val config = IrConfig.INSTANCE
    }
}
