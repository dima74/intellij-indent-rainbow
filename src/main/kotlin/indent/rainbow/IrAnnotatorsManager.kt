package indent.rainbow

import com.intellij.lang.*
import com.intellij.openapi.diagnostic.Logger

object IrAnnotatorsManager {

    private val LOG: Logger = Logger.getInstance(IrPostStartupActivity::class.java)
    private val IGNORED_LANGUAGES = listOf("Plain text")

    private val registeredLanguages = HashSet<Language>()

    fun initAnnotators() {
        val simpleAnnotator = IrSimpleAnnotator.instance
        val formatterAnnotator = IrFormatterAnnotator.instance

        val languages = Language.getRegisteredLanguages()
        val languagesNew = languages.filterNot { registeredLanguages.contains(it) }
        registeredLanguages.addAll(languagesNew)

        for (language in languagesNew) {
            if (shouldIgnoreLanguage(language)) continue

            LOG.info("[Indent Rainbow] Add language: ${language.displayName}")
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, simpleAnnotator)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, formatterAnnotator)
        }
    }

    private fun shouldIgnoreLanguage(language: Language): Boolean {
        if (language is MetaLanguage) {
            LOG.info("[Indent Rainbow] Ignore MetaLanguage: ${language.displayName}")
            return true
        }

        val baseLanguage = language.baseLanguage
        if (baseLanguage != null) {
            LOG.info("[Indent Rainbow] Ignore language ${language.displayName} which has baseLanguage: ${baseLanguage.displayName}")
            return true
        }

        return language == Language.ANY
                || language.displayName in IGNORED_LANGUAGES
                // e.g. "JVM" meta-language contains languages [JAVA, kotlin, Groovy]
                || language is MetaLanguage
                // DependentLanguage is suspicious class, lets ignore it
                || language is DependentLanguage
    }
}
