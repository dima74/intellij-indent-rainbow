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

fun Color?.toStringWithAlpha(): String {
    if (this == null) return "null"
    return "Color[r=$red,g=$green,b=$blue,a=$alpha]"
}

fun applyAlpha(color: Color, background: Color, increaseOpacity: Boolean): Color {
    check(background.alpha == 255)
    { "expect editor background color to have alpha=255, but got: ${background.toStringWithAlpha()}" }
    check(color.alpha != 255)
    { "expect indent color to have alpha<255, but got: ${color.toStringWithAlpha()}" }

    val backgroundF = background.getRGBComponents(null)
    val colorF = color.getRGBComponents(null)
    val alpha = adjustAlpha(colorF[3] + if (increaseOpacity) 0.05F else 0F, IrConfig.INSTANCE.opacityMultiplier)

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
        val SPECTRUM = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            arrayOf(
                0x1200BFFF, // Deep Sky Blue
                0x121E90FF, // Dodger Blue
                0x127B68EE, // Medium Purple
                0x128A2BE2, // Dark Violet
                0x12C71585, // Magenta
                0x12FF1493, // Dark Pink
                0x12FF0000, // Red
                0x12FF8C00, // Dark Orange
                0x12FFD700, // Gold
                0x12ADFF2F, // Green-Yellow
                0x1232CD32, // Green
                0x1220B2AA, // Turquoise
                0x1200CED1  // Aquamarine
            )
        )
        val NIGHTFALL = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            arrayOf(
                0x120052A2, // Navy
                0x120065B4, // Dodger Blue
                0x1254589F, // Medium Purple
                0x12D47796, // Dusty Dark Rose
                0x12FFA3A1, // Salmon
                0x12FEE9D6, // Beige-ish
                0x12FFB9AD, // Pinkish Tan
                0x12FFDA8B, // Lemony Yellow
                0x12FFC07A, // Goldenrod
                0x12FFAC8A  // Salmon-Pink
            )
        )
        val AQUAFLOW = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            arrayOf(
                0x1222237D, // Dark Blue
                0x122A3A88, // Prussian Blue
                0x12355A97, // Medium Blue
                0x12417CA7, // Cerulean
                0x124C9CB6, // Light Blue
                0x1281C8d8  // Sky Blue
            )
        )
        val LUMINARIUM = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            arrayOf(
                0x12FE5768, // Coral Pink
                0x12FE817D, // Salmon
                0x12FEAE97, // Deep Peach
                0x12FEDDBC, // Beige
                0x12C4DED2, // Light Cyan
                0x12A3EBDE, // Powder Blue
                0x12FFFFFF, // White
                0x12E7E7E7, // Light Gray
                0x12E5D5C2, // Almond
                0x12A5CADC, // Light Steel Blue
                0x1285B3E5, // Cornflower Blue
                0x127CA0F0, // Sky Blue
                0x12C5B5C0, // Silver
                0x12E2ABA8, // Rose Gold
                0x12FDA692, // Sandy Brown
                0x12FECA90  // Light Peach
            )
        )
        val MONOCHROME = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
                arrayOf(
                0x12353535, // Charcoal
                0x12474747, // Oil
                0x12595959, // Outer Space
                0x126B6B6B, // Onyx
                0x127C7C7C, // Dim Gray
                0x128E8E8E, // Gray
                0x12A0A0A0, // Silver
                0x12B1B1B1, // Quick Silver
                0x12C3C3C3 //  Silver Chalice
            )
        )
        val PRISM = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
                arrayOf(
                0x12FF0101, // Red
                0x12FF4401, // Dark Orange
                0x12FE9400, // Orange
                0x12FFD701, // Gold
                0x12FFF903, // Yellow
                0x12ACF609, // Green-Yellow
                0x122ABE22, // Green
                0x1201C271, // Sea Green
                0x1202EAD2, // Light Sea Green
                0x1201DDFD, // Sky Blue
                0x120089D9, // Dark Blue
                0x1206269F, // Midnight Blue
                0x122C02A2, // Indigo
                0x12711FCC, // Blue-Violet
                0x129029EA, // Medium Purple
                0x12D52EF0, // Purple
                0x12BD1E62, // Fuchsia
                0x12BD010E  // Dark Red
            )
        )
        val SOLARIZED = IrBuiltinColorsPalette(
            DEFAULT_ERROR_COLOR,
            arrayOf(
                0x12CFA000, // Green-Gold
                0x12E1661C, // Pumpkin
                0x12F03A37, // Fresh Blood
                0x12F15098, // Pink
                0x127884D8, // Cornflower Blue
                0x123095E6, // Dodger Blue
                0x1238B2A2, // Sea Green
                0x1298B500  // Chartreuse
            )
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
                IrColorsPaletteType.SPECTRUM -> IrBuiltinColorsPalette.SPECTRUM
                IrColorsPaletteType.NIGHTFALL -> IrBuiltinColorsPalette.NIGHTFALL
                IrColorsPaletteType.AQUAFLOW -> IrBuiltinColorsPalette.AQUAFLOW
                IrColorsPaletteType.LUMINARIUM -> IrBuiltinColorsPalette.LUMINARIUM
                IrColorsPaletteType.MONOCHROME -> IrBuiltinColorsPalette.MONOCHROME
                IrColorsPaletteType.PRISM -> IrBuiltinColorsPalette.PRISM
                IrColorsPaletteType.SOLARIZED -> IrBuiltinColorsPalette.SOLARIZED
                IrColorsPaletteType.CUSTOM -> IrCustomColorsPalette.getInstance(config)
            }
        }

    fun getErrorTextAttributes(): TextAttributesKey = currentPalette.errorTextAttributes

    fun getTextAttributes(tabIndex: Int): TextAttributesKey {
        if (tabIndex == -1) return getErrorTextAttributes()
        val indentsTa = currentPalette.indentsTextAttributes
        return indentsTa[tabIndex % indentsTa.size]
    }

    fun onSchemeChange() = updateTextAttributesForAllSchemes()

    private fun updateTextAttributesForAllSchemes() {
        if (!IrConfig.INSTANCE.useFormatterHighlighter) return

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
