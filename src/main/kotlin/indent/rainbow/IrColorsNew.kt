package indent.rainbow

import indent.rainbow.settings.IrColorsPaletteType
import indent.rainbow.settings.IrConfig
import java.awt.Color

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
                    it.toIntOrNull(16) ?: return null
                }
                .map { Color(it, true) }
            if (colors.size < 3) return null
            return IrColorsPaletteNew(colors.first(), colors.drop(1).toTypedArray())
        }
    }
}

object IrCustomColorsPaletteNew {
    private var cachedValue: Pair<String, IrColorsPaletteNew>? = null
    fun get(colorsString: String): IrColorsPaletteNew? {
        cachedValue
            ?.takeIf { it.first == colorsString }
            ?.let { return it.second }
        val palette = IrColorsPaletteNew.parse(colorsString) ?: return null
        cachedValue = colorsString to palette
        return palette
    }
}

val IrConfig.currentPalette: IrColorsPaletteNew
    get() = when (paletteType) {
        IrColorsPaletteType.DEFAULT -> IrColorsPaletteNew.CLASSIC
        IrColorsPaletteType.PASTEL -> IrColorsPaletteNew.PASTEL
        IrColorsPaletteType.CUSTOM -> IrCustomColorsPaletteNew.get(customPalette) ?: IrColorsPaletteNew.PASTEL
    }

fun IrConfig.getColor(level: Int): Color {
    val palette = currentPalette
    if (level == -1) return palette.errorColor
    return palette.indentColors[level % palette.indentColors.size]
}
