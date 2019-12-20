package indent.rainbow

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

class IrToggleAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean = IrConfig.instance.enabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        IrConfig.instance.enabled = state
    }
}
