package indent.rainbow.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import indent.rainbow.IrApplicationService

class IrFileEditorManagerListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        IrApplicationService.INSTANCE.init()
    }
}
