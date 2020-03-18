package indent.rainbow.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import indent.rainbow.settings.IrConfig

class IrToggleAction : ToggleAction(), DumbAware {
    override fun isSelected(e: AnActionEvent): Boolean = IrConfig.INSTANCE.enabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        IrConfig.INSTANCE.enabled = state
    }
}
