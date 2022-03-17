package indent.rainbow.listeners

import com.intellij.ide.plugins.CannotUnloadPluginException
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.editor.markup.RangeHighlighter
import indent.rainbow.IrAnnotatorsManager
import indent.rainbow.IrApplicationService
import indent.rainbow.highlightingPass.IndentDescriptor

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

    override fun checkUnloadPlugin(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.isOurPlugin()) {
            /** We store in editors user data [IndentDescriptor]s and [RangeHighlighter]s */
            throw CannotUnloadPluginException("Indent Rainbow doesn't support dynamic unload")
        }
    }
}

private fun IdeaPluginDescriptor.isOurPlugin(): Boolean = pluginId.idString == PLUGIN_ID
private const val PLUGIN_ID: String = "indent-rainbow.indent-rainbow"
