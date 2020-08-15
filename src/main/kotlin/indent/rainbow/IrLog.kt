package indent.rainbow

import com.intellij.openapi.diagnostic.Logger

val LOG: Logger = Logger.getInstance("IndentRainbow")

private var lastLogTime: Long = 0
private const val logTimeThreshold: Long = 1000  // milliseconds

fun debug(message: () -> String) {
    if (!LOG.isDebugEnabled) return

    val currentTime = System.currentTimeMillis()
    if (currentTime - lastLogTime > logTimeThreshold) {
        LOG.debug("")
    }

    LOG.debug(message())

    lastLogTime = currentTime
}
