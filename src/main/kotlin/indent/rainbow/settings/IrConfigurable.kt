package indent.rainbow.settings

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.BuildNumber
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.*
import com.intellij.util.ui.UIUtil
import indent.rainbow.IrColors
import indent.rainbow.annotators.IrAnnotatorType
import java.awt.Component
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*
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
            row("Color palette:") {
                createPaletteTypeButtonGroup()
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

    private fun Row.createPaletteTypeButtonGroup() {
        val binding = PropertyBinding(
            { config.paletteType },
            { config.paletteType = it }
        )
        buttonGroup(binding) {
            row {
                radioButton("Default (4 colors)", IrColorsPaletteType.DEFAULT)
            }
            row {
                radioButton("Pastel (6 colors)", IrColorsPaletteType.PASTEL)
            }
            lateinit var radioButton: CellBuilder<JBRadioButton>
            row {
                cell {
                    radioButton = this@row.radioButton("Custom with", IrColorsPaletteType.CUSTOM)
                    intTextField(
                        getter = { config.customPaletteNumberColors },
                        setter = { config.customPaletteNumberColors = it },
                        columns = 1,
                        range = 1..99
                    ).enableIf(radioButton.selected)
                    label("colors")
                }
            }
            // BACKCOMPAT: 2020.1 - [ActionLink] machinery was added in 2020.2
            if (ApplicationInfo.getInstance().build >= BuildNumber.fromString("202")!!) {
                row {
                    cell {
                        label("To set custom colors, visit")
                        ActionLink("Settings | Editor | Color Scheme | Indent Rainbow") {
                            // BACKCOMPAT: 2019.3 - use component returned by `label()`
                            // openColorSettings(label.component)
                            openColorSettings(radioButton.component)
                        }()
                    }
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

/** Similar to [com.intellij.ui.components.ActionLink] introduced in 2020.2, but without `autoHideOnDisable` */
private class ActionLink(text: String, listener: ActionListener) : JButton() {
    init {
        this.text = text
        addActionListener(listener)
    }

    override fun getUIClassID(): String = "LinkButtonUI"
}
