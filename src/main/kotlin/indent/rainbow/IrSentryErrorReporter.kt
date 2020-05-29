package indent.rainbow

import com.intellij.CommonBundle
import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.diagnostic.ReportMessages
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.idea.IdeaLogger
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import io.sentry.DefaultSentryClientFactory
import io.sentry.SentryClient
import io.sentry.dsn.Dsn
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import io.sentry.event.interfaces.SentryException
import java.awt.Component
import java.util.*

// https://plugin-dev.com/intellij/general/error-reporting/
class SentryErrorReporter : ErrorReportSubmitter() {

    override fun getReportActionText(): String = "Report to Author"

    override fun submit(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<SubmittedReportInfo>
    ): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(context)
        object : Backgroundable(project, "Sending Error Report") {
            override fun run(indicator: ProgressIndicator) {
                val sentryEvent = createEvent(events)
                sentryEvent.withMessage(additionalInfo)
                attachExtraInfo(sentryEvent)

                // by default, Sentry is sending async in a background thread
                sentryClient.sendEvent(sentryEvent)
                ApplicationManager.getApplication().invokeLater {
                    // we're a bit lazy here.
                    // Alternatively, we could add a listener to the sentry client
                    // to be notified if the message was successfully send
                    ReportMessages.GROUP
                        .createNotification("Thank you for submitting your report!", NotificationType.INFORMATION)
                        .setImportant(false)
                        .notify(project)
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
                }
            }
        }.queue()
        return true
    }

    private fun createEvent(events: Array<IdeaLoggingEvent>): EventBuilder {
        // this is the tricky part
        // ideaEvent.throwable is a com.intellij.diagnostic.IdeaReportingEvent.TextBasedThrowable
        // This is a wrapper and is only providing the original stacktrace via 'printStackTrace(...)',
        // but not via 'getStackTrace()'.
        //
        // Sentry accesses Throwable.getStackTrace(),
        // So, we workaround this by retrieving the original exception from the data property
        val errors = events
            .filterIsInstance<IdeaReportingEvent>()
            .mapTo(ArrayDeque(events.size)) {
                val throwable = it.data.throwable
                SentryException(throwable, throwable.stackTrace)
            }

        return EventBuilder()
            .withLevel(Event.Level.ERROR)
            .withSentryInterface(ExceptionInterface(errors))
    }

    private fun attachExtraInfo(event: EventBuilder) {
        (pluginDescriptor as? IdeaPluginDescriptor)?.let {
            event.withRelease(it.version)
        }
        event.withExtra("last_action", IdeaLogger.ourLastActionId)

        event.withTag("OS Name", SystemInfo.OS_NAME)
        event.withTag("Java Version", SystemInfo.JAVA_VERSION)

        val namesInfo = ApplicationNamesInfo.getInstance()
        event.withTag("App Name", namesInfo.productName)
        event.withTag("App Full Name", namesInfo.fullProductName)

        val appInfo = ApplicationInfoEx.getInstanceEx()
        event.withTag("App Version name", appInfo.versionName)
        event.withTag("Is EAP", appInfo.isEAP.toString())
        event.withTag("App Build", appInfo.build.asString())
        event.withTag("App Version", appInfo.fullVersion)
    }
}

private val sentryClient: SentryClient = initSentryClient()

private fun initSentryClient(): SentryClient {
    val factory = object : DefaultSentryClientFactory() {
        override fun getInAppFrames(dsn: Dsn): Collection<String> = listOf("indent.rainbow")
    }

    val dsn = CommonBundle.messageOrNull(ResourceBundle.getBundle("sentry"), "dsn")
    return factory.createClient(dsn)
}
