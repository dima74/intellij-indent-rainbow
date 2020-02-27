package indent.rainbow

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.*
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import indent.rainbow.FormatterImplHelper.buildProcessorAndWrapBlocks
import indent.rainbow.FormatterImplHelper.calcIndent
import indent.rainbow.FormatterImplHelper.getWhiteSpaceAtOffset
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class IrIndentHelper private constructor(
    private val formattingDocumentModel: FormattingDocumentModel,
    private val formatter: FormatterEx,
    private val formatProcessor: FormatProcessor,
    val indentOptions: CommonCodeStyleSettings.IndentOptions
) {

    fun getIndentAndAlignment(offset: Int): Pair<Int, Int>? {
        val whiteSpace0 = getWhiteSpaceAtOffset.invokeWithRethrow(formatter, offset, formatProcessor)
        val whiteSpace = whiteSpace0 as WhiteSpace? ?: return null

        // it is vital to call `calcIndent` method to get correct indent and alignment values
        // so, we can't use `indentSpaces` and `spaces` from whiteSpace
        val indentInfo = calcIndent.invokeWithRethrow(null, offset, formattingDocumentModel, formatProcessor, whiteSpace) as IndentInfo
        val indent = indentInfo.indentSpaces
        val alignment = indentInfo.spaces
        return Pair(indent, alignment)
    }

    companion object {

        fun getInstance(file: PsiFile): IrIndentHelper? {
            val codeStyleSettings = CodeStyle.getSettings(file)
            val indentOptions = codeStyleSettings.getIndentOptionsByFile(file)

            val formattingModelBuilder = LanguageFormatting.INSTANCE.forContext(file) ?: return null
            val formattingModel = CoreFormatterUtil.buildModel(formattingModelBuilder, file, codeStyleSettings, FormattingMode.ADJUST_INDENT)
            val formattingDocumentModel = formattingModel.documentModel

            val formatter = FormatterEx.getInstanceEx()
            val formatProcessor = buildProcessorAndWrapBlocks
                .invokeWithRethrow(formatter, formattingModel, codeStyleSettings, indentOptions, file.textRange, 0 /* ? */) as FormatProcessor

            return IrIndentHelper(formattingDocumentModel, formatter, formatProcessor, indentOptions)
        }
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

    init {
        getWhiteSpaceAtOffset.isAccessible = true
        buildProcessorAndWrapBlocks.isAccessible = true
        calcIndent.isAccessible = true
    }
}

private fun Method.invokeWithRethrow(obj: Any?, vararg args: Any?): Any? {
    try {
        return invoke(obj, *args)
    } catch (e: InvocationTargetException) {
        throw if (e.targetException is Exception) {
            e.targetException
        } else {
            e
        }
    }
}
