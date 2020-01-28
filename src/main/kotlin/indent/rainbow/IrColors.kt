package indent.rainbow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl
import java.awt.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt


// используется чтобы различать наши цвета в color scheme от цветов установленных пользователем
const val MARKER_ALPHA_VALUE: Int = 255 - 1

fun Color.toStringWithAlpha(): String {
    return javaClass.name + "[r=" + red + ",g=" + green + ",b=" + blue + ",a=" + alpha + "]"
}

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
    assert(background.alpha == 255) { "expect editor background color to have alpha=255, but: ${background.toStringWithAlpha()}" }
    assert(color.alpha != 255) { "expect indent color to have alpha<255, but: ${color.toStringWithAlpha()}" }

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = getAlpha(colorF[3])

    val resultF = (0..2).map { i -> colorF[i] * alpha + backgroundF[i] * (1 - alpha) }
        .map { (it * 255).roundToInt() }
    return Color(resultF[0], resultF[1], resultF[2], MARKER_ALPHA_VALUE)
}

object IrColors {

    private val ERROR_TA = TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_ERROR")
    private val INDENTS_TA = (1..4)
        .map { TextAttributesKey.createTextAttributesKey("INDENT_RAINBOW_COLOR_${it}") }
        .toTypedArray()

    private val COLORS: Map<TextAttributesKey, Color> = mapOf(
        ERROR_TA to 0x4D802020,  // argb
        INDENTS_TA[0] to 0x12FFFF40,
        INDENTS_TA[1] to 0x127FFF7F,
        INDENTS_TA[2] to 0x12FF7FFF,
        INDENTS_TA[3] to 0x124FECEC
    ).mapValues { Color(it.value, true) }

    init {
        onSchemeChange()
    }

    fun getErrorTextAttributes() = ERROR_TA

    fun getTextAttributes(tabIndex: Int): TextAttributesKey = INDENTS_TA[tabIndex % INDENTS_TA.size]

    fun onSchemeChange() = updateTextAttributesForAllSchemes()

    fun updateTextAttributesForAllSchemes(forceUpdate: Boolean = false) {
        val allSchemes = EditorColorsManager.getInstance().allSchemes
        for (scheme in allSchemes) {
            for ((taKey, color) in COLORS) {
                val ta = scheme.getAttributes(taKey)
                if (!forceUpdate && ta.backgroundColor?.alpha == 255) continue

                val backgroundColor = ta.backgroundColor
                // через настройки нельзя установить alpha не равную 255
                // а наш плагин всегда устанавливает MARKER_ALPHA_VALUE
                assert(
                    backgroundColor == null || backgroundColor.alpha == MARKER_ALPHA_VALUE
                            || (forceUpdate && ta.backgroundColor?.alpha == 255)
                ) { "unexpected TextAttributes value: $ta (${backgroundColor.toStringWithAlpha()})" }

                val taNew = ta.clone()
                val colorMixed = applyAlpha(color, scheme.defaultBackground)
                taNew.backgroundColor = colorMixed
                scheme.setAttributes(taKey, taNew)
            }

            if (scheme is AbstractColorsScheme) {
                scheme.setSaveNeeded(true)
            }
        }

        ApplicationManager.getApplication().saveSettings()
//        or
//        (SchemeManagerFactory.getInstance() as SchemeManagerFactoryBase).save()
    }

    fun refreshEditorIndentColors() {
        (EditorColorsManager.getInstance() as EditorColorsManagerImpl).schemeChangedOrSwitched(null)
    }
}
