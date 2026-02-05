# Periodic Worker

A lightweight Ktor plugin for running _periodic background_ tasks.

## Usage:
```kotlin
install(PeriodicWorker) {
    every(30.seconds) {
        println("how time flies by...")
    }
    
    every(5.minutes) {
        clearOldSessions()
    }
}
```

## Testing

Uses virtual time in tests - no actual* delays.

\* One of the tests has a short delay :)