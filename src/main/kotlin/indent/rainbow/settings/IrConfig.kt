package indent.rainbow.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import indent.rainbow.annotators.IrAnnotatorType

@Service
@State(name = "IndentRainbowConfig", storages = [Storage("IndentRainbowConfig.xml")])
class IrConfig : PersistentStateComponent<IrConfig> {

    var enabled: Boolean = true
    var annotatorType: IrAnnotatorType = IrAnnotatorType.FORMATTER_INCREMENTAL
    var disableErrorHighlighting: Boolean = false
    var isEnabledForReadOnlyFiles: Boolean = false
    var opacityMultiplier: Float = 0F  // [-1, +1]

    override fun getState(): IrConfig = this

    override fun loadState(state: IrConfig) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val INSTANCE: IrConfig get() = service()
        val isInitialized: Boolean get() = serviceOrNull<IrConfig>() != null
    }
}
