package indent.rainbow

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

class IndentRainbowToggleAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean = IndentRainbowConfig.instance.enabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        IndentRainbowConfig.instance.enabled = state
    }
}
