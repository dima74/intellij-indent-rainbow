package indent.rainbow.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.*
import com.intellij.util.ui.UIUtil
import indent.rainbow.IrColors
import indent.rainbow.annotators.IrAnnotatorType
import java.util.*
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JSlider
import kotlin.math.abs
import kotlin.math.roundToInt

class IrConfigurable : BoundConfigurable("Indent Rainbow") {

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(
                "Enable Indent Rainbow",
                getter = { config.enabled },
                setter = { config.enabled = it }
            ).apply { attachSubRowsEnabled(component) }
            row("Highlighter type:") {
                createHighlighterTypeButtonGroup()
            }
            row {
                checkBox(
                    "Never highlight indent as error (in red color)",
                    getter = { config.disableErrorHighlighting },
                    setter = { config.disableErrorHighlighting = it }
                )
            }
            row {
                checkBox(
                    "Enable in read only files",
                    getter = { config.isEnabledForReadOnlyFiles },
                    setter = { config.isEnabledForReadOnlyFiles = it }
                )
            }
            row("Indent colors opacity") {
                row {
                    createOpacitySlider()
                }
            }
        }
    }

    private fun Row.createHighlighterTypeButtonGroup() {
        val binding = PropertyBinding(
            { config.annotatorType },
            { config.annotatorType = it }
        )
        buttonGroup(binding) {
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

    private fun Row.createOpacitySlider() {
        val min = -100
        val max = +100
        val slider = slider(min, max, 0, 0, CCFlags.growX)
        slider.labelTable {
            put(min, JLabel("Less opacity"))
            put(max, JLabel("More opacity"))
            put(0, JLabel("Default"))
        }
        slider.withValueBinding(PropertyBinding(
            { opacityMultiplierValue },
            {
                opacityMultiplierValue = it
                // to prevent "Apply" button remain active is value is close to zero
                slider.component.value = opacityMultiplierValue
            }
        ))
        // BACKCOMPAT: 2019.3
        // slider.constraints(CCFlags.growX)
    }

    override fun apply() {
        super.apply()
        IrColors.onSchemeChange()
        IrColors.refreshEditorIndentColors()
    }

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

// BACKCOMPAT: 2020.1
private fun Row.attachSubRowsEnabled(component: AbstractButton) {
    subRowsEnabled = component.isSelected
    component.addChangeListener {
        subRowsEnabled = component.isSelected
    }
}

// BACKCOMPAT: 2019.3
private fun Cell.slider(min: Int, max: Int, minorTick: Int, majorTick: Int, constraints: CCFlags): CellBuilder<JSlider> {
    val slider = JSlider()
    UIUtil.setSliderIsFilled(slider, true)
    slider.paintLabels = true
    slider.paintTicks = true
    slider.paintTrack = true
    slider.minimum = min
    slider.maximum = max
    slider.minorTickSpacing = minorTick
    slider.majorTickSpacing = majorTick
    return slider(constraints)
}

// BACKCOMPAT: 2019.3
private fun <T : JSlider> CellBuilder<T>.labelTable(table: Hashtable<Int, JComponent>.() -> Unit): CellBuilder<T> {
    component.labelTable = Hashtable<Int, JComponent>().apply(table)
    return this
}

// BACKCOMPAT: 2019.3
private fun <T : JSlider> CellBuilder<T>.withValueBinding(modelBinding: PropertyBinding<Int>): CellBuilder<T> {
    return withBinding(JSlider::getValue, JSlider::setValue, modelBinding)
}
