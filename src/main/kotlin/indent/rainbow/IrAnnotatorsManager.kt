package indent.rainbow

import com.intellij.lang.*
import indent.rainbow.annotators.IrAnnotatorProxy
import indent.rainbow.annotators.IrFormatterSequentialAnnotator

object IrAnnotatorsManager {

    private val IGNORED_LANGUAGES = listOf("Plain text")

    private val registeredLanguages = HashSet<Language>()

    fun initAnnotators() {
        val annotatorFacade = IrAnnotatorProxy.INSTANCE
        val formatterSequentialAnnotator = IrFormatterSequentialAnnotator.INSTANCE

        val languages = Language.getRegisteredLanguages()
        val languagesNew = languages.filterNot { registeredLanguages.contains(it) }
        registeredLanguages.addAll(languagesNew)

        for (language in languagesNew) {
            if (shouldIgnoreLanguage(language)) continue

            debug { "Add language: ${language.displayName}" }
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, annotatorFacade)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, formatterSequentialAnnotator)
        }
    }

    private fun shouldIgnoreLanguage(language: Language): Boolean {
        if (language is MetaLanguage) {
            debug { "Ignore MetaLanguage: ${language.displayName}" }
            return true
        }

        val baseLanguage = language.baseLanguage
        if (baseLanguage != null) {
            debug { "Ignore language ${language.displayName} which has baseLanguage: ${baseLanguage.displayName}" }
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
