package indent.rainbow.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(name = "IndentRainbowConfig", storages = [Storage("IndentRainbowConfig.xml")])
data class IrConfig(
    var enabled: Boolean = true,
    var disableErrorHighlightingLanguageMasks: String = "Markdown; Haskell",
    var highlightOnlyIncorrectIndent: Boolean = false,
    var isEnabledForReadOnlyFiles: Boolean = false,
    var highlightEmptyLines: Boolean = true,
    var opacityMultiplier: Float = 0F,  // [-1, +1]
    var fileMasks: String = "*",

    var paletteType: IrColorsPaletteType = DEFAULT_PALETTE_TYPE,
    var customPalette: String = DEFAULT_CUSTOM_COLORS,
    // BACKCOMPAT: Remove it
    var customPaletteNumberColors: Int = 7,

    var useFormatterHighlighter: Boolean = false,
    var formatterHighlighterFileMasks: String = "*",

    var disableOnBigFiles: Boolean = true,
    var bigFilesLineThreshold: Int = 1000,

    var ignoreLinesStartingWith: String = DEFAULT_IGNORE_LINES_STARTING_WITH,

    var cornerRadius: Int = 0,
    var applyRadiusToBothSides: Boolean = false
) : PersistentStateComponent<IrConfig> {

    override fun getState(): IrConfig = this

    override fun loadState(state: IrConfig) {
        XmlSerializerUtil.copyBean(state, this)
        IrCachedData.update(this)
    }

    companion object {
        val INSTANCE: IrConfig get() = service()
        val isInitialized: Boolean get() = serviceOrNull<IrConfig>() != null

        val DEFAULT_PALETTE_TYPE: IrColorsPaletteType = IrColorsPaletteType.PASTEL
        const val DEFAULT_CUSTOM_COLORS = "4D802020, ..."
        const val DEFAULT_IGNORE_LINES_STARTING_WITH: String = "//|\\*"
    }
}

enum class IrColorsPaletteType { DEFAULT, PASTEL, SPECTRUM, NIGHTFALL, AQUAFLOW, LUMINARIUM, MONOCHROME, PRISM, SOLARIZED, CUSTOM }
