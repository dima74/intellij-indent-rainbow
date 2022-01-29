package indent.rainbow

import com.intellij.openapi.editor.Editor
import indent.rainbow.settings.IrColorsPaletteType
import indent.rainbow.settings.IrConfig
import indent.rainbow.settings.cachedData
import java.awt.Color
import kotlin.math.abs
import kotlin.math.round

class IrColorsPaletteNew(val errorColor: Color, val indentColors: Array<Color>) {

    private constructor(vararg indentColors: Int) : this(DEFAULT_ERROR_COLOR, indentColors.map { Color(it, true) }.toTypedArray())

    companion object {
        val DEFAULT_ERROR_COLOR: Color = Color(0x4D802020, true)

        val CLASSIC = IrColorsPaletteNew(0x12FFFF40, 0x127FFF7F, 0x12FF7FFF, 0x124FECEC)

        // https://github.com/oderwat/vscode-indent-rainbow/pull/64
        val PASTEL = IrColorsPaletteNew(0x26C7CEEA, 0x26B5EAD7, 0x26E2F0CB, 0x26FFDAC1, 0x26FFB7B2, 0x26FF9AA2)

        fun parse(palette: String): IrColorsPaletteNew? {
            val colors = palette
                .split(',')
                .map { it.trim() }
                .map {
                    if (it.length != 8) return null  // AARRGGBB
                    it.toLongOrNull(16) ?: return null
                }
                .map { Color(it.toInt(), true) }
            if (colors.size < 3) return null
            return IrColorsPaletteNew(colors.first(), colors.drop(1).toTypedArray())
        }
    }
}

val IrConfig.currentPalette: IrColorsPaletteNew
    get() = when (paletteType) {
        IrColorsPaletteType.DEFAULT -> IrColorsPaletteNew.CLASSIC
        IrColorsPaletteType.PASTEL -> IrColorsPaletteNew.PASTEL
        IrColorsPaletteType.CUSTOM -> cachedData.customColorPalette ?: IrColorsPaletteNew.PASTEL
    }

private fun IrConfig.getPaletteColor(level: Int): Color {
    val palette = currentPalette
    if (level == -1) return palette.errorColor
    return palette.indentColors[level % palette.indentColors.size]
}

fun IrConfig.getColorWithAdjustedAlpha(level: Int, editor: Editor): Color {
    val increaseOpacity = level != -1 && isColorLight(editor.colorsScheme.defaultBackground)
    return getColorWithAdjustedAlpha(level, increaseOpacity)
}

private fun IrConfig.getColorWithAdjustedAlpha(level: Int, increaseOpacity: Boolean): Color {
    val color = getPaletteColor(level)
    if (opacityMultiplier == 0F && !increaseOpacity) return color
    val alpha0 = color.alpha / 255.0F
    val alpha1 = alpha0 + if (increaseOpacity) 0.05F else 0F
    val alpha2 = adjustAlpha(alpha1, opacityMultiplier)
    val alpha3 = round(255 * alpha2).toInt().coerceIn(0, 255)
    return Color(color.red, color.green, color.blue, alpha3)
}

fun adjustAlpha(alpha: Float, opacityMultiplier0: Float /* [-1, +1] */): Float {
    var opacityMultiplier = opacityMultiplier0
    val needMoreOpacity = opacityMultiplier > 0F

    opacityMultiplier = abs(opacityMultiplier)
    // чтобы при изменении opacity возле стандартного значения цвета не сильно менялись
    opacityMultiplier *= opacityMultiplier  // `.pow(2)`
    // чтобы например при `targetOpacity == 0` цвета оставались видны
    opacityMultiplier *= 0.7F

    val targetOpacity = if (needMoreOpacity) {
        1F
    } else {
        0F
    }
    return interpolate(targetOpacity, alpha, opacityMultiplier)
}

fun interpolate(a: Float, b: Float, qa: Float): Float = a * qa + b * (1 - qa)

fun isColorLight(color: Color): Boolean {
    val lightness = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255
    return lightness >= 0.5
}
