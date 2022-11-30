package indent.rainbow.settings

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.builder.RowLayout.PARENT_GRID
import indent.rainbow.IrColors
import kotlin.reflect.KMutableProperty0

class IrConfigurableAdvanced : BoundConfigurable("Advanced Settings") {

    override fun createPanel(): DialogPanel = panel {
        row {
            createDisableOnBigFilesCheckBox()
        }
        row {
            checkBox("Enable in read only files").bindSelected(config::isEnabledForReadOnlyFiles)
        }
        row {
            checkBox("Highlight only lines with incorrect indentation").bindSelected(config::highlightOnlyIncorrectIndent)
        }
        row {
            checkBox("Highlight empty lines").bindSelected(config::highlightEmptyLines)
        }
        row {
            createSimpleHighlighterInFiles()
        }.layout(PARENT_GRID)
        row {
            createEnableInFiles()
        }.layout(PARENT_GRID)
        row {
            createIgnoreLinesStartingWith()
        }.layout(PARENT_GRID)
        row {
            createDisableErrorHighlightingForLanguages()
        }.layout(PARENT_GRID)
        groupRowsRange("Round Corners") {
            row {
                label("Corner radius")
                intTextField()
                    .bindIntText(config::cornerRadius)
                    .columns(COLUMNS_MEDIUM)
            }.layout(PARENT_GRID)
            row {
                checkBox("Apply to both left and right side").bindSelected(config::applyRadiusToBothSides)
            }
        }
    }

    private fun Row.createDisableOnBigFilesCheckBox() {
        val checkbox = checkBox("Disable on files with more than")
            .bindSelected(config::disableOnBigFiles)
            .gap(RightGap.SMALL)
        intTextField(range = 1..Int.MAX_VALUE)
            .bindIntText(config::bigFilesLineThreshold)
            .columns(5)
            .enabledIf(checkbox.selected)
            .gap(RightGap.SMALL)
        @Suppress("DialogTitleCapitalization")
        label("lines")
    }

    private fun Row.createSimpleHighlighterInFiles() {
        val checkbox = checkBox("Use formatter highlighter in these files:").bindSelected(config::useFormatterHighlighter)
        createFilesMask(config::formatterHighlighterFileMasks)
            .enabledIf(checkbox.selected)
    }

    private fun Row.createEnableInFiles() {
        label("Enable only in these files:")
        createFilesMask(config::fileMasks)
    }

    private fun Row.createFilesMask(property: KMutableProperty0<String>): Cell<JBTextField> {
        val emptyTextString = ApplicationBundle.message("soft.wraps.file.masks.empty.text")
        val commentString = ApplicationBundle.message("soft.wraps.file.masks.hint")

        return textField()
            .bindText(property)
            .columns(COLUMNS_MEDIUM)
            .applyToComponent { emptyText.text = emptyTextString }
            .comment(commentString)
    }

    private fun Row.createIgnoreLinesStartingWith() {
        label("Ignore lines starting with:")
        val emptyTextString = "No patterns"
        val commentString = "Regex syntax <br>Use | to separate patterns and \\ to escape chars <br>By default lines with comments are ignored"

        textField()
            .bindText(config::ignoreLinesStartingWith)
            .columns(COLUMNS_MEDIUM)
            .applyToComponent { emptyText.text = emptyTextString }
            .comment(commentString)
    }

    private fun Row.createDisableErrorHighlightingForLanguages() {
        label("Never highlight indent as error for languages:")
        textField()
            .bindText(config::disableErrorHighlightingLanguageMasks)
            .columns(COLUMNS_MEDIUM)
            .applyToComponent { emptyText.text = "No languages" }
            .comment("Use * to disable for all languages")
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
