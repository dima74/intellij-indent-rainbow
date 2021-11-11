package indent.rainbow

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl
import indent.rainbow.IrColorsPalette.Companion.DEFAULT_ERROR_COLOR
import indent.rainbow.settings.IrColorsPaletteType
import indent.rainbow.settings.IrConfig
import java.awt.Color
import kotlin.math.abs
import kotlin.math.pow

fun Color?.toStringWithAlpha(): String {
    if (this == null) return "null"
    return "Color[r=$red,g=$green,b=$blue,a=$alpha]"
}

fun interpolate(a: Float, b: Float, qa: Float): Float = a * qa + b * (1 - qa)

fun getAlpha(alpha: Float): Float {
    var opacityMultiplier = IrConfig.INSTANCE.opacityMultiplier  // [-1, +1]
    val needMoreOpacity = opacityMultiplier > 0F

    opacityMultiplier = abs(opacityMultiplier)
    // чтобы при изменении opacity возле стандартного значения цвета не сильно менялись
    opacityMultiplier = opacityMultiplier.pow(2)
    // чтобы например при `targetOpacity == 0` цвета оставались видны
    opacityMultiplier *= 0.7F

    val targetOpacity = if (needMoreOpacity) {
        1F
    } else {
        0F
    }
    return interpolate(targetOpacity, alpha, opacityMultiplier)
}

fun applyAlpha(color: Color, background: Color, increaseOpacity: Boolean): Color {
    check(background.alpha == 255)
    { "expect editor background color to have alpha=255, but got: ${background.toStringWithAlpha()}" }
    check(color.alpha != 255)
    { "expect indent color to have alpha<255, but got: ${color.toStringWithAlpha()}" }

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = getAlpha(colorF[3] + if (increaseOpacity) 0.05F else 0F)

    val resultF = (0..2).map { i -> interpolate(colorF[i], backgroundF[i], alpha) }
    val result = Color(resultF[0], resultF[1], resultF[2])
    debug {
        "[applyAlpha] " +
                "input: ${color.toStringWithAlpha()}, " +
                "output: ${result.toStringWithAlpha()}, " +
                "alpha: $alpha, " +
                "opacityMultiplier: ${IrConfig.INSTANCE.opacityMultiplier}"
    }
    return result
}

interface IrColorsPalette {
    val errorTextAttributes: TextAttributesKey
    val indentsTextAttributes: Array<TextAttributesKey>

    companion object {
        const val DEFAULT_ERROR_COLOR: Int = 0x4D802020
    }
}

class IrBuiltinColorsPalette(errorColor: Int, indentColors: Array<Int>) : IrColorsPalette {

    // Base TextAttributes are computed by plugin based on color scheme and settings
    private val errorTaBase = createTextAttributesKey("INDENT_RAINBOW_ERROR")
    private val indentsTaBase = (1..indentColors.size)
        .map { createTextAttributesKey("INDENT_RAINBOW_COLOR_$it") }
        .toTypedArray()

    // Derived TextAttributes are set by user
    private val errorTaDerived = createTextAttributesKey("INDENT_RAINBOW_ERROR_DERIVED", errorTaBase)
    private val indentsTaDerived = indentsTaBase
        .mapIndexed { i, ta -> createTextAttributesKey("INDENT_RAINBOW_COLOR_${i + 1}_DERIVED", ta) }
        .toTypedArray()

    val colorsBase: Map<TextAttributesKey, Color> = mapOf(
        errorTaBase to errorColor,
        *(indentsTaBase zip indentColors).toTypedArray()
    ).mapValues { Color(it.value, true) }

    override val errorTextAttributes: TextAttributesKey get() = errorTaDerived
    override val indentsTextAttributes: Array<TextAttributesKey> get() = indentsTaDerived

    companion object {
        val DEFAULT = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            arrayOf(0x12FFFF40, 0x127FFF7F, 0x12FF7FFF, 0x124FECEC)
        )
        val PASTEL = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            // https://github.com/oderwat/vscode-indent-rainbow/pull/64
            arrayOf(0x26C7CEEA, 0x26B5EAD7, 0x26E2F0CB, 0x26FFDAC1, 0x26FFB7B2, 0x26FF9AA2)
        )
    }
}

class IrCustomColorsPalette(private val numberColors: Int) : IrColorsPalette {

    private val errorTaCustom = createTextAttributesKey("INDENT_RAINBOW_ERROR_CUSTOM")
    private val indentsTaCustom = (1..numberColors)
        .map { createTextAttributesKey("INDENT_RAINBOW_COLOR_${it}_CUSTOM") }
        .toTypedArray()

    init {
        for (scheme in EditorColorsManager.getInstance().allSchemes) {
            scheme.setTaBackground(errorTaCustom, Color(DEFAULT_ERROR_COLOR, true))

            val indentsColor = scheme.defaultBackground.darker()
            for (taKey in indentsTaCustom) {
                scheme.setTaBackground(taKey, indentsColor)
            }
        }
    }

    private fun EditorColorsScheme.setTaBackground(taKey: TextAttributesKey, background: Color) {
        val ta = getAttributes(taKey)
        if (ta.backgroundColor != null) return
        val taCopy = ta.clone()
        taCopy.backgroundColor = background
        setAttributes(taKey, taCopy)
    }

    override val errorTextAttributes: TextAttributesKey get() = errorTaCustom
    override val indentsTextAttributes: Array<TextAttributesKey> get() = indentsTaCustom

    companion object {
        private var cachedValue: IrCustomColorsPalette? = null
        fun getInstance(config: IrConfig): IrCustomColorsPalette {
            cachedValue
                ?.takeIf { it.numberColors == config.customPaletteNumberColors }
                ?.let { return it }
            return IrCustomColorsPalette(config.customPaletteNumberColors)
                .also { this.cachedValue = it }
        }
    }
}

object IrColors {

    val currentPalette: IrColorsPalette
        get() {
            val config = serviceOrNull<IrConfig>() ?: return IrBuiltinColorsPalette.DEFAULT
            return when (config.paletteType) {
                IrColorsPaletteType.DEFAULT -> IrBuiltinColorsPalette.DEFAULT
                IrColorsPaletteType.PASTEL -> IrBuiltinColorsPalette.PASTEL
                IrColorsPaletteType.CUSTOM -> IrCustomColorsPalette.getInstance(config)
            }
        }

    fun getErrorTextAttributes(): TextAttributesKey = currentPalette.errorTextAttributes

    fun getTextAttributes(tabIndex: Int): TextAttributesKey {
        val indentsTa = currentPalette.indentsTextAttributes
        return indentsTa[tabIndex % indentsTa.size]
    }

    fun onSchemeChange() = updateTextAttributesForAllSchemes()

    private fun updateTextAttributesForAllSchemes() {
        val allSchemes = EditorColorsManager.getInstance().allSchemes
        val currentPalette = currentPalette as? IrBuiltinColorsPalette ?: return
        for (scheme in allSchemes) {
            debug { "[updateTextAttributesForAllSchemes] scheme: $scheme, defaultBackground: ${scheme.defaultBackground}" }
            var anyColorChanged = false
            for ((taKey, color) in currentPalette.colorsBase) {
                val ta = scheme.getAttributes(taKey)

                val backgroundColor = ta.backgroundColor
                check(backgroundColor == null || backgroundColor.alpha == 255)
                { "unexpected TextAttributes value: $ta (${backgroundColor.toStringWithAlpha()})" }

                val taNew = ta.clone()
                val increaseOpacity = isColorLight(scheme.defaultBackground) && !taKey.externalName.contains("ERROR")
                val colorMixed = applyAlpha(color, scheme.defaultBackground, increaseOpacity)
                taNew.backgroundColor = colorMixed
                if (taNew.backgroundColor != ta.backgroundColor) {
                    debug {
                        "Changing color of $taKey in scheme $scheme " +
                                "from ${ta.backgroundColor.toStringWithAlpha()} to ${taNew.backgroundColor.toStringWithAlpha()}"
                    }
                    scheme.setAttributes(taKey, taNew)
                    anyColorChanged = true
                }
            }

            if (anyColorChanged && scheme is AbstractColorsScheme) {
                scheme.setSaveNeeded(true)
            }
        }
    }

    fun refreshEditorIndentColors() {
        (EditorColorsManager.getInstance() as EditorColorsManagerImpl).schemeChangedOrSwitched(null)
    }
}

private fun isColorLight(color: Color): Boolean {
    val lightness = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255
    return lightness >= 0.5
}
