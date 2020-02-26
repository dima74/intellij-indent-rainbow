package indent.rainbow

import com.intellij.lang.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

// todo split into two classes
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
            if (language is MetaLanguage) {
                LOG.info("[Indent Rainbow] Ignore MetaLanguage: $language")
                continue
            }
            if (language.baseLanguage != null) {
                LOG.info("[Indent Rainbow] Ignore language $language which has baseLanguage: ${language.baseLanguage}")
                continue
            }
            if (
                language == Language.ANY
                || language.displayName in IGNORED_LANGUAGES
                // e.g. "JVM" meta-language contains languages [JAVA, kotlin, Groovy]
                || language is MetaLanguage
                // DependentLanguage is suspicious class, lets ignore it
                || language is DependentLanguage
            ) continue

            LOG.info("[Indent Rainbow] Add language: ${language.displayName}")
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, annotator)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, externalAnnotator)
        }
    }

    companion object {
        private val LOG: Logger = Logger.getInstance(IrPostStartupActivity::class.java)
        private val IGNORED_LANGUAGES = listOf("Plain text")
    }
}
