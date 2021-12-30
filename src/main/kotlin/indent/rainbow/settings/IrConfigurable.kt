package indent.rainbow.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import indent.rainbow.IrColors
import javax.swing.JLabel
import kotlin.math.abs
import kotlin.math.roundToInt

class IrConfigurable : BoundConfigurable("Indent Rainbow") {

    override fun createPanel(): DialogPanel = panel {
        blockRow {
            row {
                checkBox("Enable Indent Rainbow", config::enabled)
            }
            createHighlightingOptions()
        }
        titledRow("Color Palette") {
            createPaletteTypeButtonGroup()
        }
        row("Indent colors opacity") {
            row {
                createOpacitySlider()
            }
        }
    }

    private fun Row.createPaletteTypeButtonGroup() {
        buttonGroup(config::paletteType) {
            row {
                radioButton("Classic (4 colors)", IrColorsPaletteType.DEFAULT)
            }
            row {
                radioButton("Pastel (6 colors)", IrColorsPaletteType.PASTEL)
            }
            row {
                val radioButton = radioButton("Custom with colors:", IrColorsPaletteType.CUSTOM)
                val commentText = "Colors must be in AARRGGBB format <br>First color is error color, then indent colors <br>Use comma to separate colors"
                textField(prop = config::customPalette)
                    .growPolicy(GrowPolicy.MEDIUM_TEXT)
                    .comment(commentText, forComponent = true)
                    .enableIf(radioButton.selected)
            }
        }
    }

    private fun Row.createOpacitySlider() {
        val min = -100
        val max = +100
        val slider = slider(min, max, 0, 0)
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
        slider.constraints(CCFlags.growX)
    }

    private fun Row.createHighlightingOptions() {
        lateinit var disableErrorHighlightingCheckbox: CellBuilder<JBCheckBox>
        lateinit var highlightOnlyIncorrectIndentCheckbox: CellBuilder<JBCheckBox>
        row {
            disableErrorHighlightingCheckbox = checkBox(
                "Never highlight indent as error (in red color)",
                config::disableErrorHighlighting
            )
        }
        row {
            highlightOnlyIncorrectIndentCheckbox = checkBox(
                "Highlight only lines with incorrect indentation",
                config::highlightOnlyIncorrectIndent
            )
        }
        disableErrorHighlightingCheckbox.enableIf(highlightOnlyIncorrectIndentCheckbox.selected.not())
        highlightOnlyIncorrectIndentCheckbox.enableIf(disableErrorHighlightingCheckbox.selected.not())
    }

    override fun apply() {
        super.apply()
        if (config.paletteType == IrColorsPaletteType.CUSTOM && config.customPalette == IrConfig.DEFAULT_CUSTOM_COLORS) {
            config.paletteType = IrConfig.DEFAULT_PALETTE_TYPE
        }
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
