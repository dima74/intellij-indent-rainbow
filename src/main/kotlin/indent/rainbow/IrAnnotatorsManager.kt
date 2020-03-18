package indent.rainbow

import com.intellij.lang.*
import indent.rainbow.annotators.IrFormatterIncrementalAnnotator
import indent.rainbow.annotators.IrFormatterSequentialAnnotator
import indent.rainbow.annotators.IrSimpleAnnotator

object IrAnnotatorsManager {

    private val IGNORED_LANGUAGES = listOf("Plain text")

    private val registeredLanguages = HashSet<Language>()

    fun initAnnotators() {
        val simpleAnnotator = IrSimpleAnnotator.INSTANCE
        val formatterSequentialAnnotator = IrFormatterSequentialAnnotator.INSTANCE
        val formatterIncrementalAnnotator = IrFormatterIncrementalAnnotator.INSTANCE

        val languages = Language.getRegisteredLanguages()
        val languagesNew = languages.filterNot { registeredLanguages.contains(it) }
        registeredLanguages.addAll(languagesNew)

        for (language in languagesNew) {
            if (shouldIgnoreLanguage(language)) continue

            LOG.info("Add language: ${language.displayName}")
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, simpleAnnotator)
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, formatterIncrementalAnnotator)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, formatterSequentialAnnotator)
        }
    }

    private fun shouldIgnoreLanguage(language: Language): Boolean {
        if (language is MetaLanguage) {
            LOG.info("Ignore MetaLanguage: ${language.displayName}")
            return true
        }

        val baseLanguage = language.baseLanguage
        if (baseLanguage != null) {
            LOG.info("Ignore language ${language.displayName} which has baseLanguage: ${baseLanguage.displayName}")
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
