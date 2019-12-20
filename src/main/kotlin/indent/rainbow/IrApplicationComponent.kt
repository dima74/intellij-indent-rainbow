package indent.rainbow

import com.intellij.lang.Language
import com.intellij.lang.LanguageAnnotators
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager

class IrApplicationComponent : BaseComponent {
    override fun initComponent() {
        initAnnotator()
        initSchemeChangeListener()
    }

    private fun initAnnotator() {
        val annotator = IrAnnotator.instance
        for (language in Language.getRegisteredLanguages()) {
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, annotator)
        }
    }

    private fun initSchemeChangeListener() {
        val listener = EditorColorsListener { IrColors.onSchemeChange() }
        ApplicationManager.getApplication().messageBus.connect().subscribe(EditorColorsManager.TOPIC, listener)
    }
}
