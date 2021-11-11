package indent.rainbow.settings

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.*
import indent.rainbow.IrColors
import indent.rainbow.annotators.IrAnnotatorType

class IrConfigurableAdvanced : BoundConfigurable("Advanced Settings") {

    override fun createPanel(): DialogPanel = panel {
        row("Highlighter type:") {
            createHighlighterTypeButtonGroup()
        }
        row {
            checkBox("Enable in read only files", config::isEnabledForReadOnlyFiles)
        }
        row {
            cell {
                createFileMasksField()
            }
        }
    }

    private fun Row.createHighlighterTypeButtonGroup() {
        buttonGroup(config::annotatorType) {
            row {
                radioButton("Formatter-based, incremental (recommended)", IrAnnotatorType.FORMATTER_INCREMENTAL)
            }
            row {
                radioButton("Formatter-based, sequential (deprecated)", IrAnnotatorType.FORMATTER_SEQUENTIAL)
            }
            row {
                radioButton("Simple, incremental (not recommended)", IrAnnotatorType.SIMPLE)
            }
        }
    }

    private fun InnerCell.createFileMasksField() {
        val emptyTextString = ApplicationBundle.message("soft.wraps.file.masks.empty.text")
        val commentString = ApplicationBundle.message("soft.wraps.file.masks.hint")

        label("Enable for files:")
        textField({ config.fileMasks }, { config.fileMasks = it })
            .growPolicy(GrowPolicy.MEDIUM_TEXT)
            .applyToComponent { emptyText.text = emptyTextString }
            .comment(commentString)
    }

    override fun apply() {
        super.apply()
        IrColors.onSchemeChange()
        IrColors.refreshEditorIndentColors()
    }

    companion object {
        private val config = IrConfig.INSTANCE
    }
}
