package indent.rainbow.settings

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import indent.rainbow.IrColors
import kotlin.reflect.KMutableProperty0

class IrConfigurableAdvanced : BoundConfigurable("Advanced Settings") {

    override fun createPanel(): DialogPanel = panel {
        row {
            cell {
                createDisableOnBigFilesCheckBox()
            }
        }
        row {
            checkBox("Align with indent guides", config::alignWithIndentGuides)
        }
        row {
            checkBox("Enable in read only files", config::isEnabledForReadOnlyFiles)
        }
        row {
            checkBox("Highlight only lines with incorrect indentation", config::highlightOnlyIncorrectIndent)
        }
        row {
            checkBox("Highlight empty lines", config::highlightEmptyLines)
        }
        row {
            createSimpleHighlighterInFiles()
        }
        row {
            createEnableInFiles()
        }
        row {
            createIgnoreLinesStartingWith()
        }
        row {
            createDisableErrorHighlightingForLanguages()
        }
        titledRow("Round Corners") {
            row {
                label("Corner radius")
                intTextField(config::cornerRadius)
                    .growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row {
                checkBox("Apply to both left and right side", config::applyRadiusToBothSides)
            }
        }
    }

    private fun InnerCell.createDisableOnBigFilesCheckBox() {
        val checkbox = checkBox("Disable on files with more than", config::disableOnBigFiles)
        intTextField(
            prop = config::bigFilesLineThreshold,
            columns = 5,
            range = 1..Int.MAX_VALUE
        ).enableIf(checkbox.selected)
        label("lines")
    }

    private fun Row.createSimpleHighlighterInFiles() {
        val checkbox = checkBox("Use formatter highlighter in these files:", config::useFormatterHighlighter)
        createFilesMask(config::formatterHighlighterFileMasks)
            .enableIf(checkbox.selected)
    }

    private fun Row.createEnableInFiles() {
        label("Enable only in these files:")
        createFilesMask(config::fileMasks)
    }

    private fun Row.createFilesMask(property: KMutableProperty0<String>): CellBuilder<JBTextField> {
        val emptyTextString = ApplicationBundle.message("soft.wraps.file.masks.empty.text")
        val commentString = ApplicationBundle.message("soft.wraps.file.masks.hint")

        return textField(property)
            .growPolicy(GrowPolicy.MEDIUM_TEXT)
            .applyToComponent { emptyText.text = emptyTextString }
            .comment(commentString, forComponent = true)
    }

    private fun Row.createIgnoreLinesStartingWith() {
        label("Ignore lines starting with:")
        val emptyTextString = "No patterns"
        val commentString = "Regex syntax <br>Use | to separate patterns and \\ to escape chars <br>By default lines with comments are ignored"

        textField(config::ignoreLinesStartingWith)
            .growPolicy(GrowPolicy.MEDIUM_TEXT)
            .applyToComponent { emptyText.text = emptyTextString }
            .comment(commentString, forComponent = true)
    }

    private fun Row.createDisableErrorHighlightingForLanguages() {
        label("Never highlight indent as error for languages:")
        textField(config::disableErrorHighlightingLanguageMasks)
            .growPolicy(GrowPolicy.MEDIUM_TEXT)
            .applyToComponent { emptyText.text = "No languages" }
    }

    override fun apply() {
        super.apply()
        IrCachedData.update(config)
        IrColors.onSchemeChange()
        IrColors.refreshEditorIndentColors()
    }

    companion object {
        private val config = IrConfig.INSTANCE
    }
}
