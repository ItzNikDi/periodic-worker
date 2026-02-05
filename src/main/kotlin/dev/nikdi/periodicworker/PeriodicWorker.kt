package dev.nikdi.periodicworker

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import kotlinx.coroutines.*

/**
 * Ktor plugin for running periodic background tasks.
 *
 * Tasks are executed on a _configurable_ coroutine dispatcher and _continue_ running for the **lifetime of the application**.
 * Each task runs **independently** - exceptions in one do **NOT** affect others.
 *
 * Example usage:
 * ```
 * install(PeriodicWorker) {
 *     dispatcher = Dispatchers.Default // or whichever you prefer
 *
 *     every(30.seconds) {
 *         clearCache()
 *     }
 *
 *     every(5.minutes) {
 *         val deleted = cleanupOldSessions()
 *         application.log.info("Deleted $deleted sessions.")
 *     }
 * }
 * ```
 */
val PeriodicWorker = createApplicationPlugin(
    name = "PeriodicWorker",
    createConfiguration = ::PeriodicWorkerConfig
) {
    val dispatcher = pluginConfig.dispatcher
    val tasks = pluginConfig.tasks

    if (tasks.isEmpty()) {
        application.log.warn("No tasks registered!")
    }

    val jobs = mutableSetOf<Job>()

    on(MonitoringEvent(ApplicationStarted)) { app ->
        tasks.forEach { task ->
            val job = CoroutineScope(dispatcher).launch {
                while (isActive) {
                    delay(task.period)
                    try {
                        task.block()
                    } catch (e: Exception) {
                        app.log.error("Task failed", e)
                    }
                }
            }
            jobs.add(job)
        }
    }

    on(MonitoringEvent(ApplicationStopping)) {
        jobs.forEach { it.cancel() }
    }
}