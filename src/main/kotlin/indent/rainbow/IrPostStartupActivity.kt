package indent.rainbow

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class IrPostStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        IrAnnotatorsManager.initAnnotators()
    }
}
