package indent.rainbow

import com.intellij.lang.ExternalLanguageAnnotators
import com.intellij.lang.Language
import com.intellij.lang.LanguageAnnotators
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager

class IrApplicationComponent : BaseComponent {
    override fun initComponent() {
        initAnnotators()
        initSchemeChangeListener()
    }

    private fun initAnnotators() {
        val annotator = IrAnnotator.instance
        val externalAnnotator = IrExternalAnnotator.instance
        for (language in Language.getRegisteredLanguages()) {
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, annotator)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, externalAnnotator)
        }
    }

    private fun initSchemeChangeListener() {
        val listener = EditorColorsListener { IrColors.onSchemeChange() }
        ApplicationManager.getApplication().messageBus.connect().subscribe(EditorColorsManager.TOPIC, listener)
    }
}
