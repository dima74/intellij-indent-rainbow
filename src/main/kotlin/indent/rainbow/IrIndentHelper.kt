package indent.rainbow

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.*
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import indent.rainbow.FormatterImplHelper.buildProcessorAndWrapBlocks
import indent.rainbow.FormatterImplHelper.calcIndent
import indent.rainbow.FormatterImplHelper.getWhiteSpaceAtOffset
import indent.rainbow.settings.document
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.math.max

abstract class IrIndentHelper {
    abstract val indentOptions: CommonCodeStyleSettings.IndentOptions
    abstract fun getIndentAndAlignment(line: Int): Pair<Int, Int>?
}

class IrSimpleIndentHelper(
    file: PsiFile,
    private val document: Document,
) : IrIndentHelper() {

    override val indentOptions: CommonCodeStyleSettings.IndentOptions =
        CodeStyle.getSettings(file).getIndentOptionsByFile(file)

    override fun getIndentAndAlignment(line: Int): Pair<Int, Int>? {
        val offset = document.getLineStartOffset(line)
        val fileText = document.charsSequence

        var offsetEnd = offset
        while (offsetEnd < fileText.length && fileText[offsetEnd].let { it == ' ' || it == '\t' }) {
            ++offsetEnd
        }
        return Pair(offsetEnd - offset, 0)
    }
}

class IrFormatterIndentHelper private constructor(
    private val document: Document,
    private val formattingDocumentModel: FormattingDocumentModel,
    private val formatter: FormatterEx,
    private val formatProcessor: FormatProcessor,
    override val indentOptions: CommonCodeStyleSettings.IndentOptions,
) : IrIndentHelper() {

    override fun getIndentAndAlignment(line: Int): Pair<Int, Int>? {
        val offset = document.getLineStartOffset(line)
        val (indent1, alignment1) = getIndentAndAlignmentMethod1(offset)
        val (indent2, alignment2) = getIndentAndAlignmentMethod2(offset) ?: return null

        val total1 = indent1 + alignment1
        val total2 = indent2 + alignment2
        if (total1 != total2) return Pair(indent2, alignment2)

        // We calculate maximum of indents because there are some strange cases when
        // indent1 == alignment2 == 0 and indent2 == alignment1 == N
        // For example in Rust:
        // ```
        // struct Foo {
        //     x1: i32,  // indent1=4, alignment1=0  indent2=4, alignment2=0
        //     x2: i32,  // indent1=0, alignment1=4  indent2=4, alignment2=0
        //     x3: i32,  // indent1=0, alignment1=4  indent2=4, alignment2=0
        // }
        // ```
        val indent = max(indent1, indent2)
        val alignment = total1 - indent
        return Pair(indent, alignment)
    }

    /**
     * For some reason this method does not work for lines with closed brackets, e.g.:
     * ```
     * fun foo() {    // line 1
     *     println()  // line 2
     * }              // line 3
     * ```
     * For line 3 it will return same indent as for line 2 (indent=4, alignment=0)
     */
    private fun getIndentAndAlignmentMethod1(offset: Int): Pair<Int, Int> {
        val indentInfo = formatProcessor.getIndentAt(offset)
        val indent = indentInfo.indentSpaces
        val alignment = indentInfo.spaces
        return Pair(indent, alignment)
    }

    /**
     * For some reason this method produces incorrect indent vs alignment values
     * when run in incremental mode (that is not applied sequentially to all lines in file)
     *
     * For example (after changing foo method body):
     * ```
     * class Foo {        // line 1
     *     fun foo() {    // line 2:  indent=4, alignment=0
     *         println()  // line 3:  indent=4, alignment=4
     *     }              // line 4:  indent=0, alignment=4
     * }                  // line 5
     * ```
     */
    private fun getIndentAndAlignmentMethod2(offset: Int): Pair<Int, Int>? {
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
        fun getInstance(file: PsiFile): IrFormatterIndentHelper? {
            val codeStyleSettings = CodeStyle.getSettings(file)
            val indentOptions = codeStyleSettings.getIndentOptionsByFile(file)

            val formattingModelBuilder = LanguageFormatting.INSTANCE.forContext(file) ?: return null
            val formattingModel = CoreFormatterUtil.buildModel(formattingModelBuilder, file, codeStyleSettings, FormattingMode.ADJUST_INDENT)
            val formattingDocumentModel = formattingModel.documentModel

            val formatter = FormatterEx.getInstanceEx()
            val formatProcessor = buildProcessorAndWrapBlocks
                .invokeWithRethrow(formatter, formattingModel, codeStyleSettings, indentOptions, file.textRange, 0 /* ? */) as FormatProcessor

            val document = file.document ?: return null
            return IrFormatterIndentHelper(document, formattingDocumentModel, formatter, formatProcessor, indentOptions)
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
