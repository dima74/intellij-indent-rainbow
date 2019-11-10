package indent.rainbow

import com.intellij.lang.Language
import com.intellij.lang.LanguageAnnotators
import com.intellij.openapi.components.BaseComponent

class MyApplicationComponent : BaseComponent {
    override fun initComponent() {
        val annotator = MyAnnotator.instance
        for (language in Language.getRegisteredLanguages()) {
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, annotator)
        }
    }
}
