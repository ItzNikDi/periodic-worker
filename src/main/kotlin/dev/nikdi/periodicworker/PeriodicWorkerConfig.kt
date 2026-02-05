package dev.nikdi.periodicworker

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration

/**
 * Configuration for the [PeriodicWorker] plugin.
 *
 * @property dispatcher The coroutine dispatcher to be used for executing the tasks. Defaults to [Dispatchers.IO].
 */
class PeriodicWorkerConfig {
    var dispatcher: CoroutineDispatcher = Dispatchers.IO
    internal val tasks = mutableListOf<PeriodicTask>()

    /**
     * Registers a periodic background task.
     *
     * @param period The time interval between task executions.
     * @param block The suspending function to execute periodically.
     */
    fun every(period: Duration, block: suspend () -> Unit) {
        tasks.add(PeriodicTask(period, block))
    }
}

/**
 * Represents a periodic task configuration.
 *
 * @property period The time interval between executions.
 * @property block The suspending function to execute.
 */
data class PeriodicTask(
    val period: Duration,
    val block: suspend () -> Unit
)