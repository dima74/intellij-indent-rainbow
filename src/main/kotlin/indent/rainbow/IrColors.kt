package indent.rainbow

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl
import java.awt.Color
import kotlin.math.abs
import kotlin.math.pow

fun Color?.toStringWithAlpha(): String {
    if (this == null) return "null"
    return this.javaClass.name + "[r=" + red + ",g=" + green + ",b=" + blue + ",a=" + alpha + "]"
}

fun interpolate(a: Float, b: Float, qa: Float): Float = a * qa + b * (1 - qa)

fun getAlpha(alpha: Float): Float {
    var opacityMultiplier = IrConfig.instance.opacityMultiplier  // [-1, +1]
    val needMoreOpacity = opacityMultiplier > 0F

    opacityMultiplier = abs(opacityMultiplier)
    // чтобы при изменении opacity возле стандартного значения цвета не сильно менялись
    opacityMultiplier = opacityMultiplier.pow(2)
    // чтобы например при opacityMultiplier == 0 цвета оставались видны
    opacityMultiplier *= 0.7F

    val targetOpacity = if (needMoreOpacity) {
        1F
    } else {
        0F
    }
    return interpolate(targetOpacity, alpha, opacityMultiplier)
}

fun applyAlpha(color: Color, background: Color): Color {
    assert(background.alpha == 255)
    { "expect editor background color to have alpha=255, but: ${background.toStringWithAlpha()}" }
    assert(color.alpha != 255)
    { "expect indent color to have alpha<255, but: ${color.toStringWithAlpha()}" }

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = getAlpha(colorF[3])

    val resultF = (0..2).map { i -> interpolate(colorF[i], backgroundF[i], alpha) }
    return Color(resultF[0], resultF[1], resultF[2])
}

object IrColors {

    // base TextAttributes are computed by plugin based on scheme and settings
    private val ERROR_TA = createTextAttributesKey("INDENT_RAINBOW_ERROR")
    private val INDENTS_TA = (1..4)
        .map { createTextAttributesKey("INDENT_RAINBOW_COLOR_${it}") }
        .toTypedArray()
    // derived TextAttributes are set by user
    private val ERROR_TA_DERIVED = createTextAttributesKey("INDENT_RAINBOW_ERROR_DERIVED", ERROR_TA)
    private val INDENTS_TA_DERIVED = (1..4)
        .map { createTextAttributesKey("INDENT_RAINBOW_COLOR_${it}_DERIVED", INDENTS_TA[it - 1]) }
        .toTypedArray()

    private val COLORS: Map<TextAttributesKey, Color> = mapOf(
        ERROR_TA to 0x4D802020,  // argb
        INDENTS_TA[0] to 0x12FFFF40,
        INDENTS_TA[1] to 0x127FFF7F,
        INDENTS_TA[2] to 0x12FF7FFF,
        INDENTS_TA[3] to 0x124FECEC
    ).mapValues { Color(it.value, true) }

    fun getErrorTextAttributes() = ERROR_TA_DERIVED

    fun getTextAttributes(tabIndex: Int): TextAttributesKey = INDENTS_TA_DERIVED[tabIndex % INDENTS_TA_DERIVED.size]

    fun onSchemeChange() = updateTextAttributesForAllSchemes()

    private fun updateTextAttributesForAllSchemes() {
        val allSchemes = EditorColorsManager.getInstance().allSchemes
        for (scheme in allSchemes) {
            for ((taKey, color) in COLORS) {
                val ta = scheme.getAttributes(taKey)

                val backgroundColor = ta.backgroundColor
                assert(backgroundColor == null || backgroundColor.alpha == 255)
                { "unexpected TextAttributes value: $ta (${backgroundColor.toStringWithAlpha()})" }

                val taNew = ta.clone()
                val colorMixed = applyAlpha(color, scheme.defaultBackground)
                taNew.backgroundColor = colorMixed
                if (taNew.backgroundColor != ta.backgroundColor) {
                    LOG.info(
                        "Changing color of $taKey in scheme $scheme " +
                                "from ${ta.backgroundColor.toStringWithAlpha()} to ${taNew.backgroundColor.toStringWithAlpha()}"
                    )
                    scheme.setAttributes(taKey, taNew)
                }
            }

            if (scheme is AbstractColorsScheme) {
                scheme.setSaveNeeded(true)
            }
        }
    }

    fun refreshEditorIndentColors() {
        (EditorColorsManager.getInstance() as EditorColorsManagerImpl).schemeChangedOrSwitched(null)
    }
}

private val LOG: Logger = Logger.getInstance(IrColors::class.java)
