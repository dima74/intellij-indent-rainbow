package indent.rainbow.settings

import com.intellij.util.PatternUtil
import indent.rainbow.IrColorsPaletteNew
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

val IrConfig.cachedData: IrCachedData
    get() = IrCachedData.get(this)

class IrCachedData(config: IrConfig) {
    val fileMasks: List<Pattern>? = createFilePatterns(config.fileMasks)
    val formatterHighlighterFileMasks: List<Pattern>? = createFilePatterns(config.formatterHighlighterFileMasks)
    val customColorPalette: IrColorsPaletteNew? = IrColorsPaletteNew.parse(config.customPalette)
    val ignoreLinesStartingWith: Pattern = try {
        config.ignoreLinesStartingWith.toPattern()
    } catch (e: PatternSyntaxException) {
        IrConfig.DEFAULT_IGNORE_LINES_STARTING_WITH.toPattern()
    }

    companion object {
        @Volatile
        private var cachedValue: IrCachedData? = null

        fun get(config: IrConfig): IrCachedData =
            cachedValue ?: update(config)

        fun update(config: IrConfig): IrCachedData =
            IrCachedData(config).also { cachedValue = it }
    }
}

fun createFilePatterns(masks: String): List<Pattern>? {
    return masks.trim()
        .also { if (it == "*") return null }
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map(PatternUtil::fromMask)
}
