package indent.rainbow

import com.intellij.openapi.options.Configurable
import java.util.*
import javax.swing.*
import kotlin.math.abs
import kotlin.math.roundToInt

class IrConfigurable : Configurable {
    lateinit var rootPanel: JPanel
    private lateinit var isEnabled: JCheckBox
    private lateinit var useFormatterBasedAnnotator: JCheckBox
    private lateinit var opacityMultiplier: JSlider

    init {
        isEnabled.addActionListener { updateEnabled() }
        opacityMultiplier.paintLabels = true

        val labelTable = Hashtable<Int, JLabel>()
        labelTable[opacityMultiplier.maximum] = JLabel("More opacity")
        labelTable[opacityMultiplier.minimum] = JLabel("Less opacity")
        labelTable[0] = JLabel("Default")
        opacityMultiplier.labelTable = labelTable
    }

    override fun getDisplayName(): String = "Indent Rainbow"

    override fun isModified(): Boolean {
        return isEnabled.isSelected != config.enabled
                || useFormatterBasedAnnotator.isSelected != config.useFormatterBasedAnnotator
                || opacityMultiplier.value != opacityMultiplierValue
    }

    override fun apply() {
        config.enabled = isEnabled.isSelected
        config.useFormatterBasedAnnotator = useFormatterBasedAnnotator.isSelected
        opacityMultiplierValue = opacityMultiplier.value
        IrColors.onSchemeChange()
        IrColors.refreshEditorIndentColors()
    }

    override fun createComponent(): JComponent {
        isEnabled.isSelected = config.enabled
        useFormatterBasedAnnotator.isSelected = config.useFormatterBasedAnnotator
        opacityMultiplier.value = opacityMultiplierValue
        updateEnabled()
        return rootPanel
    }

    private fun updateEnabled() {
        val enabled = isEnabled.isSelected
        useFormatterBasedAnnotator.isEnabled = enabled
        opacityMultiplier.isEnabled = enabled
    }

    companion object {
        private val config = IrConfig.instance

        private var opacityMultiplierValue: Int
            get() = (config.opacityMultiplier * 100).roundToInt()
            set(value) {
                // zero (default value) if value is almost default
                config.opacityMultiplier = if (abs(value) < 5) {
                    0F
                } else {
                    value / 100F
                }
            }
    }
}
