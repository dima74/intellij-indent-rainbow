package indent.rainbow

import com.intellij.application.options.editor.CheckboxDescriptor
import com.intellij.application.options.editor.checkBox
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected

class IrConfigurable : BoundConfigurable("Indent Rainbow Plugin") {
    override fun createPanel(): DialogPanel {
        return panel {
            var isEnabledCheckbox: CellBuilder<JBCheckBox>? = null
            row {
                isEnabledCheckbox = checkBox(isEnabled)
            }
            row {
                checkBox(useFormatterBasedAnnotator).enableIf(isEnabledCheckbox!!.selected)
            }
        }
    }

    companion object {
        private val config = IrConfig.instance

        private val isEnabled = CheckboxDescriptor("Enabled", config::enabled)
        private val useFormatterBasedAnnotator = CheckboxDescriptor("Use Formatter Based Highlighting", config::useFormatterBasedAnnotator)
    }
}
