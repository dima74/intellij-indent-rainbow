package indent.rainbow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager

class IrApplicationComponent : BaseComponent {
    override fun initComponent() {
        initSchemeChangeListener()
    }

    private fun initSchemeChangeListener() {
        val listener = EditorColorsListener { IrColors.onSchemeChange() }
        ApplicationManager.getApplication().messageBus.connect().subscribe(EditorColorsManager.TOPIC, listener)
    }
}
