package indent.rainbow

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import java.awt.Color

object MyColors {
    private val COLORS: Array<Color> = arrayOf(
        Color(255, 255, 64, 18),
        Color(127, 255, 127, 18),
        Color(255, 127, 255, 18),
        Color(79, 236, 236, 18)
    )
    private val ERROR_COLOR = Color(128, 32, 32, 77)

    val errorTextAttributesKey = createTextAttributesKey(ERROR_COLOR, 0)
    private val textAttributesKeys: Array<TextAttributesKey> = COLORS
        .mapIndexed { index, color -> createTextAttributesKey(color, 1 + index) }
        .toTypedArray()

    private fun createTextAttributesKey(color: Color, index: Int): TextAttributesKey {
        val defaultTextAttributesKey = EditorColors.SEARCH_RESULT_ATTRIBUTES
        val defaultTextAttributes = defaultTextAttributesKey.defaultAttributes

        val textAttributes = defaultTextAttributes.clone()
        textAttributes.backgroundColor = color

        val key = "indent.rainbow.textAttribute.$index"
        return TextAttributesKey.createTextAttributesKey(key, textAttributes)
    }

    fun getTextAttributes(tabIndex: Int): TextAttributesKey = textAttributesKeys[tabIndex % textAttributesKeys.size]
}
