package indent.rainbow

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class IrApplicationService {

    @Volatile
    private var isInitialized: Boolean = false

    fun init() {
        synchronized(this) {
            if (isInitialized) {
                return
            } else {
                isInitialized = true
            }
        }

        IrColors.onSchemeChange()
        IrAnnotatorsManager.initAnnotators()
    }

    companion object {
        val INSTANCE: IrApplicationService get() = service()
    }
}
