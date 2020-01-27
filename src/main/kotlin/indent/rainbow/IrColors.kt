package indent.rainbow

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import java.awt.Color
import kotlin.math.abs
import kotlin.math.pow

fun getAlpha(alpha: Float): Float {
    var opacityMultiplier = IrConfig.instance.opacityMultiplier  // [-1, +1]
    val needMoreOpacity = opacityMultiplier > 0F

    opacityMultiplier = abs(opacityMultiplier)
    // чтобы например при opacityMultiplier == 1 цвета оставались видны
    opacityMultiplier *= 0.7F
    // чтобы при изменении opacity возле стандартного значения цвета не сильно менялись
    opacityMultiplier = opacityMultiplier.pow(3)

    // кусочно-линейная функция 1 -> alpha -> 0
    return if (needMoreOpacity) {
        // more opacity
        alpha * (1 - opacityMultiplier) + 0 * opacityMultiplier
    } else {
        // less opacity
        alpha * (1 - opacityMultiplier) + 1 * opacityMultiplier
    }
}

fun applyAlpha(color: Color, background: Color): Color {
    assert(background.alpha == 255) { background.toString() }
    assert(color.alpha != 255) { color.toString() }

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = getAlpha(colorF[3])

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
    private val COLORS = (1..4)
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
