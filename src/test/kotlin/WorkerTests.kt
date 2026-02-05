import dev.nikdi.periodicworker.PeriodicWorker
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.configLoaders
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkerTests {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `no tasks registered does not cause errors`() = testScope.runTest {
        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = testDispatcher
                // no tasks registered
            }

            startApplication()

            testScheduler.advanceTimeBy(10.seconds)
            testScheduler.runCurrent()

            // exit normally
        }
    }

    @Test
    fun `runs periodically`() = testScope.runTest {
        var counter = 0

        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = testDispatcher
                every(1.seconds) {
                    counter++
                }
            }

            startApplication()

            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()
            assertEquals(1, counter, "Counter was incorrect: $counter")

            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()
            assertEquals(2, counter, "Counter was incorrect: $counter")

            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()
            assertEquals(3, counter, "Counter was incorrect: $counter")
        }
    }

    @Test
    fun `tasks execute independently`() = testScope.runTest {
        var fastCounter = 0
        var slowCounter = 0

        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = testDispatcher
                every(1.seconds) {
                    fastCounter++
                }
                every(5.seconds) {
                    slowCounter++
                }
            }

            startApplication()

            testScheduler.advanceTimeBy(5.seconds)
            testScheduler.runCurrent()

            assertEquals(5, fastCounter, "Fast counter was incorrect: $fastCounter")
            assertEquals(1, slowCounter, "Slow counter was incorrect: $slowCounter")
        }
    }

    @Test
    fun `task exceptions do not stop periodic execution`() = testScope.runTest {
        var counter = 0
        var failureCount = 0

        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = testDispatcher
                every(1.seconds) {
                    failureCount++
                    if (failureCount < 3) {
                        throw RuntimeException("Simulated failure")
                    }
                    counter++
                }
            }

            startApplication()

            testScheduler.advanceTimeBy(5.seconds)
            testScheduler.runCurrent()

            assertEquals(3, counter, "Counter was incorrect: $counter") // 5 - 2 failures = 3
            assertEquals(5, failureCount, "Failure count was incorrect: $failureCount")
        }
    }

    @Test
    fun `tasks stop when application stops`() = testScope.runTest {
        var counter = 0

        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = testDispatcher
                every(1.seconds) {
                    counter++
                }
            }

            startApplication()

            testScheduler.advanceTimeBy(3.seconds)
            testScheduler.runCurrent()
            assertEquals(3, counter, "Counter was incorrect: $counter")

            application.monitor.raise(ApplicationStopping, application) // stop the application

            testScheduler.advanceTimeBy(5.seconds) // advance further, should not increase
            testScheduler.runCurrent()
            assertEquals(3, counter, "Counter was incorrect: $counter") // 3, not 8
        }
    }

    @Test
    fun `tasks run on specified dispatcher`() = testScope.runTest {
        val executionThreads = mutableSetOf<String>()

        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = Dispatchers.Default
                every(100.milliseconds) {
                    executionThreads.add(Thread.currentThread().name)
                }
            }

            startApplication()

            delay(500.milliseconds) // actual delay

            assertTrue(executionThreads.any { it.contains("DefaultDispatcher") }, "Set wasn't populated: ${executionThreads.joinToString(", ")}")
        }
    }

    @Test
    fun `task suspension does not block other tasks`() = testScope.runTest {
        var quickTaskCounter = 0
        var longTaskStarted = false
        var longTaskCompleted = false

        testApplication {
            application.install(PeriodicWorker) {
                dispatcher = testDispatcher

                every(1.seconds) {
                    longTaskStarted = true
                    delay(10.seconds) // long "running" task
                    longTaskCompleted = true
                }

                every(1.seconds) {
                    quickTaskCounter++
                }
            }

            startApplication()

            testScheduler.advanceTimeBy(5.seconds)
            testScheduler.runCurrent()

            assertTrue(longTaskStarted, "Long started was wrong: $longTaskStarted")
            assertFalse(longTaskCompleted, "Long completed was wrong: $longTaskCompleted") // long task still "running"
            assertEquals(5, quickTaskCounter, "Counter was incorrect: $quickTaskCounter") // but quick task continues
        }
    }

    @Test
    fun `task can access application context`() = testScope.runTest {
        var capturedEnvironmentProperty: String? = null

        testApplication {
            environment {
                config = ApplicationConfig("application.yaml")
                configLoaders
            }

            application.install(PeriodicWorker) {
                dispatcher = testDispatcher
                every(1.seconds) {
                    capturedEnvironmentProperty = application.environment.config.propertyOrNull("ktor.test.values.test_value")?.getString()
                }
            }

            startApplication()

            testScheduler.advanceTimeBy(1.seconds)
            testScheduler.runCurrent()

            assertNotNull(capturedEnvironmentProperty, "Environment property was wrong: $capturedEnvironmentProperty")
        }
    }
}