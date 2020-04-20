package indent.rainbow.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsScheme
import indent.rainbow.IrColors

class IrEditorColorsListener : EditorColorsListener {
    override fun globalSchemeChange(scheme: EditorColorsScheme?) {
        ApplicationManager.getApplication().invokeLater({ IrColors.onSchemeChange() }, ModalityState.NON_MODAL)
    }
}
