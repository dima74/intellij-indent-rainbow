package indent.rainbow.listeners

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import indent.rainbow.IrAnnotatorsManager
import indent.rainbow.IrApplicationService

class IrDynamicPluginListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.isOurPlugin()) {
            IrApplicationService.INSTANCE.init()
        } else {
            IrAnnotatorsManager.initAnnotators()
        }
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.isOurPlugin()) {
            IrAnnotatorsManager.disposeAnnotators()
        }
    }
}

private fun IdeaPluginDescriptor.isOurPlugin(): Boolean = pluginId.idString == PLUGIN_ID
private const val PLUGIN_ID: String = "indent-rainbow.indent-rainbow"
