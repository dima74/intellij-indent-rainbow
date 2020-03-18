package indent.rainbow.settings

import com.intellij.openapi.options.Configurable
import indent.rainbow.IrColors
import indent.rainbow.annotators.IrAnnotatorType
import java.util.*
import javax.swing.*
import kotlin.math.abs
import kotlin.math.roundToInt

class IrConfigurable : Configurable {
    lateinit var rootPanel: JPanel

    private lateinit var isEnabled: JCheckBox

    private lateinit var highlighterTypePanel: JPanel
    private lateinit var highlighterFormatterIncremental: JRadioButton
    private lateinit var highlighterFormatterSequential: JRadioButton
    private lateinit var highlighterSimple: JRadioButton
    private val highlighterTypeGroup: Map<IrAnnotatorType, JRadioButton>

    private lateinit var disableErrorHighlighting: JCheckBox
    private lateinit var isEnabledForReadOnlyFiles: JCheckBox

    private lateinit var opacityMultiplierLabel: JLabel
    private lateinit var opacityMultiplier: JSlider

    init {
        isEnabled.addActionListener { updateEnabled() }

        highlighterTypeGroup = mapOf(
            IrAnnotatorType.FORMATTER_INCREMENTAL to highlighterFormatterIncremental,
            IrAnnotatorType.FORMATTER_SEQUENTIAL to highlighterFormatterSequential,
            IrAnnotatorType.SIMPLE to highlighterSimple
        )

        val labelTable = Hashtable<Int, JLabel>()
        labelTable[opacityMultiplier.maximum] = JLabel("More opacity")
        labelTable[opacityMultiplier.minimum] = JLabel("Less opacity")
        labelTable[0] = JLabel("Default")
        opacityMultiplier.labelTable = labelTable
        opacityMultiplier.paintLabels = true
    }

    override fun getDisplayName(): String = "Indent Rainbow"

    override fun isModified(): Boolean {
        return isEnabled.isSelected != config.enabled
                || annotatorType != config.annotatorType
                || disableErrorHighlighting.isSelected != config.disableErrorHighlighting
                || isEnabledForReadOnlyFiles.isSelected != config.isEnabledForReadOnlyFiles
                || opacityMultiplier.value != opacityMultiplierValue
    }

    override fun apply() {
        config.enabled = isEnabled.isSelected
        config.annotatorType = annotatorType
        config.disableErrorHighlighting = disableErrorHighlighting.isSelected
        config.isEnabledForReadOnlyFiles = isEnabledForReadOnlyFiles.isSelected
        opacityMultiplierValue = opacityMultiplier.value
        IrColors.onSchemeChange()
        IrColors.refreshEditorIndentColors()
    }

    override fun createComponent(): JComponent {
        isEnabled.isSelected = config.enabled
        highlighterTypeGroup.getValue(config.annotatorType).isSelected = true
        disableErrorHighlighting.isSelected = config.disableErrorHighlighting
        isEnabledForReadOnlyFiles.isSelected = config.isEnabledForReadOnlyFiles
        opacityMultiplier.value = opacityMultiplierValue
        updateEnabled()
        return rootPanel
    }

    private fun updateEnabled() {
        val enabled = isEnabled.isSelected
        highlighterTypePanel.isEnabled = enabled
        highlighterTypeGroup.values.forEach { it.isEnabled = enabled }
        disableErrorHighlighting.isEnabled = enabled
        isEnabledForReadOnlyFiles.isEnabled = enabled
        opacityMultiplierLabel.isEnabled = enabled
        opacityMultiplier.isEnabled = enabled
    }

    private val annotatorType: IrAnnotatorType
        get() = highlighterTypeGroup.entries
            .find { it.value.isSelected }!!
            .key

    companion object {
        private val config = IrConfig.INSTANCE

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
