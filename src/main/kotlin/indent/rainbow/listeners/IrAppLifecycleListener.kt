package indent.rainbow.listeners

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.project.Project
import indent.rainbow.IrApplicationService

class IrAppLifecycleListener : AppLifecycleListener {
    override fun appStarting(projectFromCommandLine: Project?) {
        IrApplicationService.INSTANCE.init()
    }
}
