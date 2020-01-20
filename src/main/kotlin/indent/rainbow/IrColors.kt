package indent.rainbow

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import java.awt.Color

fun applyAlpha(color: Color, background: Color): Color {
    assert(background.alpha == 255) { background.toString() }
    assert(color.alpha != 255) { color.toString() }

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
    private val defaultScheme: EditorColorsScheme
        get() = EditorColorsManager.getInstance().getScheme(EditorColorsManager.DEFAULT_SCHEME_NAME)


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
        val taDefault = defaultScheme.getAttributes(taKey)
        val indentColor = taDefault.backgroundColor

        val ta = taDefault.clone()
        ta.backgroundColor = applyAlpha(indentColor, editorBackground)
        scheme.setAttributes(taKey, ta)
    }
}
