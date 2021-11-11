package indent.rainbow.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(name = "IndentRainbowConfig", storages = [Storage("IndentRainbowConfig.xml")])
data class IrConfig(
    var enabled: Boolean = true,
    var disableErrorHighlighting: Boolean = false,
    var highlightOnlyIncorrectIndent: Boolean = false,
    var isEnabledForReadOnlyFiles: Boolean = false,
    var opacityMultiplier: Float = 0F,  // [-1, +1]
    var fileMasks: String = "*",

    var paletteType: IrColorsPaletteType = IrColorsPaletteType.DEFAULT,
    var customPaletteNumberColors: Int = 7,

    var useSimpleHighlighter: Boolean = false,
    var simpleHighlighterFileMasks: String = "*",

    var disableOnBigFiles: Boolean = true,
    var bigFilesLineThreshold: Int = 1000,
) : PersistentStateComponent<IrConfig> {

    override fun getState(): IrConfig = this

    override fun loadState(state: IrConfig) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val INSTANCE: IrConfig get() = service()
        val isInitialized: Boolean get() = serviceOrNull<IrConfig>() != null
    }
}

enum class IrColorsPaletteType { DEFAULT, PASTEL, CUSTOM }
