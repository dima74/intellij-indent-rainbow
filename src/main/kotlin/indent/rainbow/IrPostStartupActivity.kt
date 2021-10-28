package indent.rainbow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class IrPostStartupActivity : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {
        IrApplicationService.INSTANCE.init()
    }
}
