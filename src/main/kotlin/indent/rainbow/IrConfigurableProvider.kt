package indent.rainbow

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class IrConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable? = IrConfigurable()

    override fun canCreateConfigurable(): Boolean {
        val version = ApplicationInfo.getInstance().build.baselineVersion
        return version >= CONFIGURABLE_PLATFORM_VERSION
    }

    companion object {
        private const val CONFIGURABLE_PLATFORM_VERSION: Int = 193
    }
}
