package indent.rainbow.settings

import com.intellij.application.options.colors.ColorAndFontOptions
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
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
            createHighlightingOptions()
            row {
                checkBox(
                    "Enable in read only files",
                    getter = { config.isEnabledForReadOnlyFiles },
                    setter = { config.isEnabledForReadOnlyFiles = it }
                )
            }
            row {
                cell {
                    createFileMasksField()
                }
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
            row {
                cell {
                    val radioButton = this@row.radioButton("Custom with", IrColorsPaletteType.CUSTOM)
                    intTextField(
                        getter = { config.customPaletteNumberColors },
                        setter = { config.customPaletteNumberColors = it },
                        columns = 1,
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

    private fun Row.createHighlightingOptions() {
        lateinit var disableErrorHighlightingCheckbox: CellBuilder<JBCheckBox>
        lateinit var highlightOnlyIncorrectIndentCheckbox: CellBuilder<JBCheckBox>
        row {
            disableErrorHighlightingCheckbox = checkBox(
                "Never highlight indent as error (in red color)",
                getter = { config.disableErrorHighlighting },
                setter = { config.disableErrorHighlighting = it }
            )
        }
        row {
            highlightOnlyIncorrectIndentCheckbox = checkBox(
                "Highlight only lines with incorrect indentation",
                getter = { config.highlightOnlyIncorrectIndent },
                setter = { config.highlightOnlyIncorrectIndent = it }
            )
        }
        disableErrorHighlightingCheckbox.enableIf(highlightOnlyIncorrectIndentCheckbox.selected.not())
        highlightOnlyIncorrectIndentCheckbox.enableIf(disableErrorHighlightingCheckbox.selected.not())
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

    private fun InnerCell.createFileMasksField() {
        val emptyTextString = ApplicationBundle.message("soft.wraps.file.masks.empty.text")
        val commentString = ApplicationBundle.message("soft.wraps.file.masks.hint")

        label("Enable for files:")
        textField({ config.fileMasks }, { config.fileMasks = it })
            .growPolicy(GrowPolicy.MEDIUM_TEXT)
            .applyToComponent { emptyText.text = emptyTextString }
            .comment(commentString)
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

fun ComponentPredicate.not(): ComponentPredicate = NotPredicate(this)

private class NotPredicate(private val predicate: ComponentPredicate) : ComponentPredicate() {
    override fun invoke(): Boolean = !predicate.invoke()
    override fun addListener(listener: (Boolean) -> Unit) = predicate.addListener { listener(!it) }
}
