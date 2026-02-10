package dev.nikdi.periodicworker

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration

/**
 * Configuration for the [PeriodicWorker] plugin.
 *
 * @see every
 */
class PeriodicWorkerConfig {
    internal val tasks = mutableListOf<PeriodicTask>()

    /**
     * Registers a periodic background task.
     *
     * @param period The time interval between task executions.
     * @param runsOnStart Whether the task will be executed on app startup. Defaults to `false`.
     * @param dispatcher The dispatcher to be used for executing the task. Defaults to [Dispatchers.IO].
     * @param block The suspending function to execute periodically.
     */
    fun every(
        period: Duration,
        runsOnStart: Boolean = false,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit
    ) {
        tasks.add(PeriodicTask(period, runsOnStart, dispatcher, block))
    }
}

/**
 * Represents a periodic task configuration.
 *
 * @property period The time interval between executions.
 * @property runsOnStart Whether to run the task on startup.
 * @property dispatcher The dispatcher to used for the execution.
 * @property block The suspending function to execute.
 */
data class PeriodicTask(
    val period: Duration,
    val runsOnStart: Boolean,
    val dispatcher: CoroutineDispatcher,
    val block: suspend () -> Unit
)