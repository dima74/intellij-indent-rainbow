package indent.rainbow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager

class IrApplicationComponent : BaseComponent {
    override fun initComponent() {
        IrColors.onSchemeChange()
        initSchemeChangeListener()

        IrAnnotatorsManager.initAnnotators()
    }

    private fun initSchemeChangeListener() {
        val application = ApplicationManager.getApplication()
        val listener = EditorColorsListener {
            application.invokeLater({ IrColors.onSchemeChange() }, ModalityState.NON_MODAL)
        }
        application.messageBus.connect().subscribe(EditorColorsManager.TOPIC, listener)
    }
}
