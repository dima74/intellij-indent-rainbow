package indent.rainbow.settings

import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import indent.rainbow.IrColors
import javax.swing.Icon

class IrColorSettingsPage : ColorSettingsPage {

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        val errorAttributesDescriptor = AttributesDescriptor("Error", IrColors.getErrorTextAttributes())
        val attributesDescriptors = (0 until 4)
            .map { AttributesDescriptor("Indent ${it + 1}", IrColors.getTextAttributes(it)) }
            .toTypedArray()
        return arrayOf(errorAttributesDescriptor, *attributesDescriptors)
    }

    override fun getDemoText(): String {
        @org.intellij.lang.annotations.Language("Java")
        val text = """
            public class Matrix {
                private int[][] matrix;
            
                public Matrix(int n) {
                    matrix = new int[n][n];
                     /* example of incorrect indent */ 
                }
            
                public Matrix multiply(Matrix other) {
                    int n = other.matrix.length;
                    Matrix result = new Matrix(n);
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            for (int k = 0; k < n; k++) {
                                result.matrix[i][j] += matrix[i][k] * matrix[k][j];
                            }
                        }
                    }
                    return result;
                }
            }

            """.trimIndent()
        return wrapTextWithTags(text)
    }

    override fun getHighlighter(): SyntaxHighlighter {
        val language = Language.findLanguageByID("JAVA") ?: Language.ANY
        return SyntaxHighlighterFactory.getSyntaxHighlighter(language, null, null)
            ?: SyntaxHighlighterFactory.getSyntaxHighlighter(Language.ANY, null, null)
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        attributeDescriptors.associate { it.displayName to it.key }

    override fun getIcon(): Icon? = null

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Indent Rainbow"

    private fun wrapTextWithTags(text: String): String {
        return text.lines()
            .joinToString("\n") { wrapLineWithTags(it) }
    }

    private fun wrapLineWithTags(line: String): String {
        val lineTrimmed = line.trimStart()
        val numberSpaces = line.length - lineTrimmed.length

        val indents = if (numberSpaces % 4 != 0) {
            val indent = " ".repeat(numberSpaces)
            "<Error>$indent</Error>"
        } else {
            val numberIndents = numberSpaces / 4
            (0 until numberIndents)
                .joinToString("") {
                    val tagName = "Indent ${it % 4 + 1}"
                    "<$tagName>    </$tagName>"
                }
        }
        return indents + lineTrimmed
    }
}
