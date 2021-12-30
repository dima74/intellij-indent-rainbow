package indent.rainbow.settings

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.ide.DataManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import indent.rainbow.IrColors
import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.JButton
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
                cell {
                    val radioButton = this@row.radioButton("Custom with", IrColorsPaletteType.CUSTOM)
                    intTextField(
                        prop = config::customPaletteNumberColors,
                        columns = 3,
                        range = 1..99
                    ).enableIf(radioButton.selected)
                    label("colors")
                }
            }
            row {
                cell {
                    val label = label("To set custom colors, visit")
                    ActionLink("Settings | Editor | Color Scheme | Indent Rainbow") {
                        openColorSettings(label.component)
                    }()
                }
            }
        }
    }

    private fun openColorSettings(context: Component) {
        val dataContext = DataManager.getInstance().getDataContext(context)
        val settings = Settings.KEY.getData(dataContext) ?: return
        try {
            // try to select related configurable in the current Settings dialog
            val configurable = settings.find("reference.settingsdialog.IDE.editor.colors") ?: return
            val configurables = (configurable as? ColorAndFontOptions)?.configurables ?: return
            val irConfigurable = configurables.find { it.displayName == "Indent Rainbow" } ?: return
            settings.select(irConfigurable)
        } catch (ignored: IllegalStateException) {
            // see ScopeColorsPageFactory.java:74
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

/** Similar to [com.intellij.ui.components.ActionLink] introduced in 2020.2, but without `autoHideOnDisable` */
private class ActionLink(text: String, listener: ActionListener) : JButton() {
    init {
        this.text = text
        addActionListener(listener)
    }

    override fun getUIClassID(): String = "LinkButtonUI"
}
