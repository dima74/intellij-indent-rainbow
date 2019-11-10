package indent.rainbow

import com.intellij.openapi.editor.colors.TextAttributesKey

object MyColors {

//    private val IDE_DEFAULT = EditorColors.SEARCH_RESULT_ATTRIBUTES

    val ERROR = TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_ERROR")

    private val COLORS = (1..4)
        .map { TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_COLOR_$it") }
        .toTypedArray()

//    private fun createTextAttributesKey(color: Color, index: Int): TextAttributesKey {
//        val defaultTextAttributesKey = EditorColors.SEARCH_RESULT_ATTRIBUTES
//        val defaultTextAttributes = defaultTextAttributesKey.defaultAttributes
//
//        val textAttributes = defaultTextAttributes.clone()
//        textAttributes.backgroundColor = color
//
//        val key = "indent.rainbow.textAttribute.$index"
//        return TextAttributesKey.createTextAttributesKey(key, textAttributes)
//    }

    fun getTextAttributes(tabIndex: Int): TextAttributesKey = COLORS[tabIndex % COLORS.size]
}
