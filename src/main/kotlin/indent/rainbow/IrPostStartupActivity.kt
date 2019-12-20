package indent.rainbow

import com.intellij.lang.ExternalLanguageAnnotators
import com.intellij.lang.Language
import com.intellij.lang.LanguageAnnotators
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class IrPostStartupActivity : StartupActivity {
    private val registeredLanguages = HashSet<Language>()

    override fun runActivity(project: Project) {
        initAnnotators()
    }

    private fun initAnnotators() {
        val annotator = IrAnnotator.instance
        val externalAnnotator = IrExternalAnnotator.instance

        val languages = Language.getRegisteredLanguages()
        val languagesNew = languages.filterNot { registeredLanguages.contains(it) }
        registeredLanguages.addAll(languagesNew)

        for (language in languagesNew) {
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, annotator)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, externalAnnotator)
        }
    }
}
