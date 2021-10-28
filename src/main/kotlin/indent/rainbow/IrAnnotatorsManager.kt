package indent.rainbow

import com.intellij.lang.*
import indent.rainbow.annotators.IrAnnotatorProxy
import indent.rainbow.annotators.IrFormatterSequentialAnnotator

object IrAnnotatorsManager {

    private val IGNORED_LANGUAGES: List<String> = listOf("Plain text")

    // https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html#do-not-use-filetypelanguage-as-map-key
    private val registeredLanguages: MutableSet<String> = hashSetOf()

    fun initAnnotators() {
        val languages = Language.getRegisteredLanguages()

        for (language in languages) {
            if (!registeredLanguages.add(language.id)) continue
            if (shouldIgnoreLanguage(language)) continue

            debug { "Add language: ${language.displayName}" }
            LanguageAnnotators.INSTANCE.addExplicitExtension(language, IrAnnotatorProxy.INSTANCE)
            ExternalLanguageAnnotators.INSTANCE.addExplicitExtension(language, IrFormatterSequentialAnnotator.INSTANCE)
        }
    }

    fun disposeAnnotators() {
        for (languageId in registeredLanguages) {
            val language = Language.findLanguageByID(languageId) ?: continue
            LanguageAnnotators.INSTANCE.removeExplicitExtension(language, IrAnnotatorProxy.INSTANCE)
            ExternalLanguageAnnotators.INSTANCE.removeExplicitExtension(language, IrFormatterSequentialAnnotator.INSTANCE)
        }
        registeredLanguages.clear()
    }

    private fun shouldIgnoreLanguage(language: Language): Boolean {
        if (language is MetaLanguage) {
            // e.g. "JVM" meta-language contains languages [JAVA, kotlin, Groovy]
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
                // DependentLanguage is suspicious class, lets ignore it
                || language is DependentLanguage
    }
}
