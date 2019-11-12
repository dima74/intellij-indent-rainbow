package indent.rainbow

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

fun applyAlpha(color: Color, background: Color): Color {
    assert(background.alpha == 255)
    assert(color.alpha != 255)

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = colorF[3]

    val resultF = (0..2).map { i -> colorF[i] * alpha + backgroundF[i] * (1 - alpha) }
    return Color(resultF[0], resultF[1], resultF[2])
}

object MyColorsOriginal {
    val ERROR = TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_ERROR")

    val COLORS = (1..4)
        .map { TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_COLOR_$it") }
        .toTypedArray()
}

object MyColors {

    private val editorBackground: Color
        get() = scheme.defaultBackground

    private val scheme: EditorColorsScheme
        get() = EditorColorsManager.getInstance().schemeForCurrentUITheme


    val ERROR: TextAttributesKey = createTextAttributesKey("INDENT_RAINBOW_ERROR_copy")
    private val COLORS = (1..4)
        .map { createTextAttributesKey("INDENT_RAINBOW_COLOR_${it}_copy") }
        .toTypedArray()

    init {
        onSchemeChange()
    }

    private fun createTextAttributesKey(name: String): TextAttributesKey {
        val textAttributes = TextAttributes(null, null, null, null, Font.PLAIN)
        return TextAttributesKey.createTextAttributesKey(name, textAttributes)
    }

    fun getTextAttributes(tabIndex: Int): TextAttributesKey = COLORS[tabIndex % COLORS.size]

    fun onSchemeChange() {
        updateTextAttributes(ERROR, MyColorsOriginal.ERROR)
        for (i in 0..3) {
            updateTextAttributes(COLORS[i], MyColorsOriginal.COLORS[i])
        }
    }

    private fun updateTextAttributes(taKey: TextAttributesKey, taKeyOriginal: TextAttributesKey) {
        val taOriginal = scheme.getAttributes(taKeyOriginal)
        taKey.defaultAttributes.backgroundColor = applyAlpha(taOriginal.backgroundColor, editorBackground)
    }
}
