package dev.nikdi.periodicworker

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import kotlinx.coroutines.*

/**
 * Ktor plugin for running periodic background tasks.
 *
 * Tasks are executed on a _**per-task** configurable_ coroutine dispatcher and _continue_ running for the **lifetime of the application**.
 * Each task runs **independently** - exceptions in one do **NOT** affect others.
 *
 * Example usage:
 * ```
 * install(PeriodicWorker) {
 *     every(period = 30.seconds) { // this will use the default task configuration
 *         clearCache()
 *     }
 *
 *     every(period = 5.minutes, runsOnStart = true) { // this will also run the task on startup
 *         val deleted = cleanupOldSessions()
 *         application.log.info("Deleted $deleted sessions.")
 *     }
 * }
 * ```
 *
 * @see PeriodicWorkerConfig.every
 */
val PeriodicWorker = createApplicationPlugin(
    name = "PeriodicWorker",
    createConfiguration = ::PeriodicWorkerConfig
) {
    val tasks = pluginConfig.tasks

    if (tasks.isEmpty()) {
        application.log.warn("No tasks registered!")
    }

    val jobs = mutableSetOf<Job>()

    on(MonitoringEvent(ApplicationStarted)) { app ->
        tasks.forEach { task ->
            val job = CoroutineScope(task.dispatcher).launch {
                if (task.runsOnStart) {
                    runCatching { task.block() }
                        .onFailure { ex -> app.log.error("Task failed on start", ex) }
                }
                while (isActive) {
                    delay(task.period)
                    runCatching { task.block() }
                        .onFailure { ex -> app.log.error("Periodic task failed", ex) }
                }
            }
            jobs.add(job)
        }
    }

    on(MonitoringEvent(ApplicationStopping)) {
        jobs.forEach { it.cancel() }
    }
}