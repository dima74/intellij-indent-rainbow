package indent.rainbow.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import indent.rainbow.IrColors
import indent.rainbow.IrConfig

class IrResetColorsAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        IrConfig.instance.opacityMultiplier = 0F
        IrColors.updateTextAttributesForAllSchemes(true)
        IrColors.refreshEditorIndentColors()
    }
}
