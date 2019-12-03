package indent.rainbow

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "IndentRainbowConfig", storages = [(Storage("IndentRainbowConfig.xml"))])
class IndentRainbowConfig : PersistentStateComponent<IndentRainbowConfig> {

    var enabled: Boolean = true

    override fun getState(): IndentRainbowConfig = this

    override fun loadState(state: IndentRainbowConfig) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val instance: IndentRainbowConfig
            get() = ServiceManager.getService(IndentRainbowConfig::class.java)
    }
}
