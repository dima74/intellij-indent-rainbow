package indent.rainbow

import com.intellij.diagnostic.ITNReporter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent

class IrExceptionAnalyzerReporter : ITNReporter() {
    override fun showErrorInRelease(event: IdeaLoggingEvent): Boolean = true
}
