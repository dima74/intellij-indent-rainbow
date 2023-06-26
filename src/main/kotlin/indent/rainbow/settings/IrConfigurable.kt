package indent.rainbow.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import indent.rainbow.IrColors
import javax.swing.JLabel
import kotlin.math.abs
import kotlin.math.roundToInt

class IrConfigurable : BoundConfigurable("Indent Rainbow") {

    override fun createPanel(): DialogPanel = panel {
        row {
            @Suppress("DialogTitleCapitalization")
            checkBox("Enable Indent Rainbow").bindSelected(config::enabled)
        }
        group("Color Palette") {
            createPaletteTypeButtonGroup()
        }
        group("Indent Colors Opacity") {
            row {
                createOpacitySlider()
            }
        }
    }

    private fun Panel.createPaletteTypeButtonGroup() {
        buttonsGroup {
            row {
                radioButton("Classic (4 colors)", IrColorsPaletteType.DEFAULT)
            }
            row {
                radioButton("Pastel (6 colors)", IrColorsPaletteType.PASTEL)
            }
            row {
                radioButton("Spectrum (10 colors)", IrColorsPaletteType.SPECTRUM)
            }
            row {
                radioButton("Nightfall (10 colors)", IrColorsPaletteType.NIGHTFALL)
            }
            row {
                val radioButton = radioButton("Custom with colors:", IrColorsPaletteType.CUSTOM)
                val commentText =
                    "Colors must be in AARRGGBB format <br>First color is error color, then indent colors <br>Use comma to separate colors"
                textField()
                    .bindText(config::customPalette)
                    .columns(COLUMNS_MEDIUM)
                    .comment(commentText)
                    .enabledIf(radioButton.selected)
            }
        }.bind(config::paletteType)
    }

    private fun Row.createOpacitySlider() {
        val min = -100
        val max = +100
        val slider = slider(min, max, 0, 0)
        slider.labelTable(
            hashMapOf(
                min to JLabel("Transparent"),
                max to JLabel("Opaque"),
                0 to JLabel("Default"),
            )
        )
        slider.bindValue(
            { opacityMultiplierValue },
            {
                opacityMultiplierValue = it
                // to prevent "Apply" button remain active if value is close to zero
                slider.component.value = opacityMultiplierValue
            }
        )
        slider.horizontalAlign(HorizontalAlign.FILL)
    }

    override fun apply() {
        super.apply()
        if (config.paletteType == IrColorsPaletteType.CUSTOM && config.customPalette == IrConfig.DEFAULT_CUSTOM_COLORS) {
            config.paletteType = IrConfig.DEFAULT_PALETTE_TYPE
        }
        IrCachedData.update(config)
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
