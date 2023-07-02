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

        val SPECTRUM = IrColorsPaletteNew(
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

        val NIGHTFALL = IrColorsPaletteNew(
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

        val AQUAFLOW = IrColorsPaletteNew(
                0x1222237D, // Dark Blue
                0x122A3A88, // Prussian Blue
                0x12355A97, // Medium Blue
                0x12417CA7, // Cerulean
                0x124C9CB6, // Light Blue
                0x1281C8d8  // Sky Blue
        )

        val LUMINARIUM = IrColorsPaletteNew(
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

        val MONOCHROME = IrColorsPaletteNew(
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

        val PRISM = IrColorsPaletteNew(
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

        val SOLARIZED = IrColorsPaletteNew(
                0x12CFA000, // Green-Gold
                0x12E1661C, // Pumpkin
                0x12F03A37, // Fresh Blood
                0x12F15098, // Pink
                0x127884D8, // Cornflower Blue
                0x123095E6, // Dodger Blue
                0x1238B2A2, // Sea Green
                0x1298B500  // Chartreuse
        )
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
        IrColorsPaletteType.SPECTRUM -> IrColorsPaletteNew.SPECTRUM
        IrColorsPaletteType.NIGHTFALL -> IrColorsPaletteNew.NIGHTFALL
        IrColorsPaletteType.AQUAFLOW -> IrColorsPaletteNew.AQUAFLOW
        IrColorsPaletteType.LUMINARIUM -> IrColorsPaletteNew.LUMINARIUM
        IrColorsPaletteType.MONOCHROME -> IrColorsPaletteNew.MONOCHROME
        IrColorsPaletteType.PRISM -> IrColorsPaletteNew.PRISM
        IrColorsPaletteType.SOLARIZED -> IrColorsPaletteNew.SOLARIZED
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
