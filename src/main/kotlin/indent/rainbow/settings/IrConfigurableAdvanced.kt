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
            checkBox("Enable in read only files", config::isEnabledForReadOnlyFiles)
        }
        row {
            createSimpleHighlighterInFiles()
        }
        row {
            createEnableInFiles()
        }
    }

    private fun Row.createSimpleHighlighterInFiles() {
        val checkbox = checkBox("Use simple highlighter in these files:", config::useSimpleHighlighter)
        createFilesMask(config::simpleHighlighterFileMasks)
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

    override fun apply() {
        super.apply()
        IrColors.onSchemeChange()
        IrColors.refreshEditorIndentColors()
    }

    companion object {
        private val config = IrConfig.INSTANCE
    }
}
