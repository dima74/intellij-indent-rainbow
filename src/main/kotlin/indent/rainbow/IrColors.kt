package indent.rainbow

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import java.awt.Color

fun applyAlpha(color: Color, background: Color): Color {
    assert(background.alpha == 255)
    assert(color.alpha != 255)

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = colorF[3]

    val resultF = (0..2).map { i -> colorF[i] * alpha + backgroundF[i] * (1 - alpha) }
    return Color(resultF[0], resultF[1], resultF[2])
}

object IrColors {

    private val editorBackground: Color
        get() = scheme.defaultBackground

    private val scheme: EditorColorsScheme
        get() = EditorColorsManager.getInstance().schemeForCurrentUITheme


    val ERROR = TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_ERROR")
    val COLORS = (1..4)
        .map { TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_COLOR_${it}") }
        .toTypedArray()

    init {
        onSchemeChange()
    }

    fun getTextAttributes(tabIndex: Int): TextAttributesKey = COLORS[tabIndex % COLORS.size]

    fun onSchemeChange() {
        updateTextAttributes(ERROR)
        for (i in 0..3) {
            updateTextAttributes(COLORS[i])
        }
    }

    private fun updateTextAttributes(taKey: TextAttributesKey) {
        val taDefault = taKey.defaultAttributes
        val ta = scheme.getAttributes(taKey)
        ta.backgroundColor = applyAlpha(taDefault.backgroundColor, editorBackground)
    }
}
