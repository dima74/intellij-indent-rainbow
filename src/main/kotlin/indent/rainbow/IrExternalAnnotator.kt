package indent.rainbow

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.*
import com.intellij.lang.LanguageFormatting
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.util.PsiTreeUtil
import indent.rainbow.FormatterImplHelper.buildProcessorAndWrapBlocks
import indent.rainbow.FormatterImplHelper.calcIndent
import indent.rainbow.FormatterImplHelper.getWhiteSpaceAtOffset
import java.lang.reflect.Method

@Suppress("RedundantUnitReturnType", "RedundantUnitExpression")
class IrExternalAnnotator : ExternalAnnotator<Unit, Unit>(), DumbAware {

    init {
        getWhiteSpaceAtOffset.isAccessible = true
        buildProcessorAndWrapBlocks.isAccessible = true
        calcIndent.isAccessible = true
    }

    override fun collectInformation(file: PsiFile): Unit {
        return Unit
    }

    override fun doAnnotate(collectedInfo: Unit): Unit {
        return Unit
    }

    override fun apply(file: PsiFile, annotationResult: Unit, holder: AnnotationHolder) {
        if (!config.enabled || !config.useFormatterBasedAnnotator) return

        val project = file.project
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val codeStyleSettings = CodeStyle.getSettings(file)
        val indentOptions = codeStyleSettings.getIndentOptionsByFile(file)

        val formattingModelBuilder = LanguageFormatting.INSTANCE.forContext(file) ?: return
        val formattingModel = CoreFormatterUtil.buildModel(formattingModelBuilder, file, codeStyleSettings, FormattingMode.ADJUST_INDENT)
        val formattingDocumentModel = formattingModel.documentModel

        val formatter = FormatterEx.getInstanceEx()
        val formatProcessor = buildProcessorAndWrapBlocks
            .invoke(formatter, formattingModel, codeStyleSettings, indentOptions, file.textRange, 0 /* ? */) as FormatProcessor

        IrExternalAnnotatorImpl(file, document, formattingDocumentModel, formatter, formatProcessor, holder, indentOptions).apply()
    }

    companion object {
        val instance: IrExternalAnnotator = IrExternalAnnotator()
        private val config = IrConfig.instance
    }
}

private class IrExternalAnnotatorImpl(
    val file: PsiFile,
    val document: Document,
    val documentModel: FormattingDocumentModel,
    val formatter: FormatterEx,
    val formatProcessor: FormatProcessor,
    val holder: AnnotationHolder,
    indentOptions: CommonCodeStyleSettings.IndentOptions
) {
    val useTabs: Boolean = indentOptions.USE_TAB_CHARACTER
    val tabSize: Int = indentOptions.TAB_SIZE
    val indentSize: Int = indentOptions.INDENT_SIZE

    fun apply() {
        for (line in 0 until document.lineCount) {
            val offset = document.getLineStartOffset(line)
            val element = file.findElementAt(offset) ?: continue
            // we can't check `element is PsiWhiteSpace`, because e.g. in Yaml custom LeafPsiElement is used
            if (!element.text.isBlank()) continue
            // unfortunately spaces in doc comments (at beginning of lines) are PsiWhiteSpace
            if (PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null) continue

            val whiteSpace0 = getWhiteSpaceAtOffset.invoke(formatter, offset, formatProcessor)
            val whiteSpace = whiteSpace0 as WhiteSpace? ?: continue

            // it is vital to call `calcIndent` method to get correct indent and alignment values
            // so, we can't use `indentSpaces` and `spaces` from whiteSpace
            val indentInfo = calcIndent.invoke(null, offset, documentModel, formatProcessor, whiteSpace) as IndentInfo
            val indent = indentInfo.indentSpaces
            val alignment = indentInfo.spaces

            highlight(line, indent, alignment)
        }
    }

    private fun highlight(line: Int, indentSpaces: Int, alignment: Int) {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        if (lineText.isEmpty()) return

        val indent = if (useTabs) indentSpaces / tabSize else indentSpaces
        val prefixExpected = if (useTabs) {
            assert(indentSpaces % tabSize == 0)
            "\t".repeat(indent) + " ".repeat(alignment)
        } else {
            " ".repeat(indentSpaces + alignment)
        }
        if (prefixExpected.isEmpty()) return

        val prefixActual = lineText.takeWhile { it == ' ' || it == '\t' }

        if (prefixActual == prefixExpected) {
            val step = if (useTabs) 1 else indentSize
            for (offset in 0 until indent step step) {
                val start = lineStartOffset + offset
                val end = start + step
                annotate(holder, start, end, offset / step)
            }
        } else {
            val end = lineStartOffset + prefixActual.length
            annotate(holder, lineStartOffset, end, null)
        }
    }

    private fun annotate(holder: AnnotationHolder, start: Int, end: Int, colorIndex: Int?) {
        val textAttributes = if (colorIndex != null) {
            IrColors.getTextAttributes(colorIndex)
        } else {
            IrColors.ERROR
        }

        val highlightRange = TextRange(start, end)
        val annotation = holder.createInfoAnnotation(highlightRange, null)
        annotation.textAttributes = textAttributes
    }
}

private object FormatterImplHelper {
    val getWhiteSpaceAtOffset: Method = FormatterImpl::class.java.getDeclaredMethod(
        "getWhiteSpaceAtOffset",
        Int::class.java,
        FormatProcessor::class.java
    )
    val buildProcessorAndWrapBlocks: Method = FormatterImpl::class.java.getDeclaredMethod(
        "buildProcessorAndWrapBlocks",
        FormattingModel::class.java,
        CodeStyleSettings::class.java,
        CommonCodeStyleSettings.IndentOptions::class.java,
        TextRange::class.java,
        Int::class.java
    )
    val calcIndent: Method = FormatterImpl::class.java.getDeclaredMethod(
        "calcIndent",
        Int::class.java,
        FormattingDocumentModel::class.java,
        FormatProcessor::class.java,
        WhiteSpace::class.java
    )
}
