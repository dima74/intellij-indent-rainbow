package indent.rainbow.highlightingPass

import com.intellij.codeHighlighting.*
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar.Anchor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import indent.rainbow.annotators.IrAnnotatorType
import indent.rainbow.annotators.isAnnotatorEnabled
import indent.rainbow.settings.IrConfig

class IrHighlightingPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory, DumbAware {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(
            this,
            Anchor.FIRST,
            Pass.LOCAL_INSPECTIONS,  // makes sense only for `Anchor.AFTER`
            false,
            false
        )
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass =
        IrHighlightingPass(file, editor)
}

class IrHighlightingPass(
    private val file: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(file.project, editor.document, false), DumbAware {

    private val config: IrConfig = IrConfig.INSTANCE

    override fun doCollectInformation(progress: ProgressIndicator) {
        // Potentially we can use `FileStatusMap.getDirtyTextRange` here.
        // Note that it can be null even when file was changed, so we should also check `file.modificationStamp`

        if (!config.isAnnotatorEnabled(file, IrAnnotatorType.SIMPLE_HIGHLIGHTING_PASS)) return
        val indents = LineIndentsCalculator(file, document).compute()
        val descriptors = createDescriptors(document, indents)
        editor.putUserData(IR_DESCRIPTORS, descriptors)
    }

    override fun doApplyInformationToEditor() {
        val descriptors = editor.getUserData(IR_DESCRIPTORS) ?: return
        if (!config.isAnnotatorEnabled(file, IrAnnotatorType.SIMPLE_HIGHLIGHTING_PASS)) {
            removeAllHighlighters()
            return
        }
        addHighlighters(descriptors)
    }

    private fun addHighlighters(descriptors: List<IndentDescriptor>) {
        val markupModel = editor.markupModel
        val existingHighlighters = editor.getUserData(IR_RANGE_HIGHLIGHTERS) ?: emptyList()
        val newHighlighters = mutableListOf<RangeHighlighter>()

        /**
         * Optimization to reduce highlighters creation.
         * Uses same approach as in merge sort.
         */
        val existingIterator = existingHighlighters.iterator()
        val descriptorsIterator = descriptors.iterator()
        while (existingIterator.hasNext() || descriptorsIterator.hasNext()) {
            if (!existingIterator.hasNext()) {
                newHighlighters += addHighlighter(markupModel, descriptorsIterator.next())
            } else if (!descriptorsIterator.hasNext()) {
                existingIterator.next().dispose()
            } else {
                val existing = existingIterator.next()
                val descriptor = descriptorsIterator.next()
                val canReuseHighlighter = existing.startOffset == descriptor.startOffset && existing.endOffset == descriptor.endOffset
                newHighlighters += if (canReuseHighlighter) {
                    (existing.customRenderer as IrHighlighterRenderer).level = descriptor.level
                    existing
                } else {
                    existing.dispose()
                    addHighlighter(markupModel, descriptor)
                }
            }
        }
        check(descriptors.size == newHighlighters.size)
        editor.putUserData(IR_RANGE_HIGHLIGHTERS, newHighlighters)
    }

    private fun addHighlighter(markupModel: MarkupModel, descriptor: IndentDescriptor): RangeHighlighter {
        val renderer = IrHighlighterRenderer(descriptor.level, descriptor.indentSize)
        return markupModel.addRangeHighlighter(descriptor.startOffset, descriptor.endOffset, renderer)
    }

    private fun removeAllHighlighters() {
        editor.getUserData(IR_RANGE_HIGHLIGHTERS)?.forEach { it.dispose() }
        editor.putUserData(IR_RANGE_HIGHLIGHTERS, null)
    }
}

private fun MarkupModel.addRangeHighlighter(
    startOffset: Int,
    endOffset: Int,
    renderer: CustomHighlighterRenderer
): RangeHighlighter {
    val layer = -1  // doesn't matter for highlighters with custom renderers
    val highlighter = addRangeHighlighter(null, startOffset, endOffset, layer, EXACT_RANGE)
    highlighter.customRenderer = renderer
    return highlighter
}

private val IR_DESCRIPTORS: Key<List<IndentDescriptor>> = Key.create("IR_DESCRIPTORS")
private val IR_RANGE_HIGHLIGHTERS: Key<List<RangeHighlighter>> = Key.create("IR_RANGE_HIGHLIGHTER")
