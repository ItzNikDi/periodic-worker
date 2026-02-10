# Periodic Worker

[![version](https://img.shields.io/github/v/release/ItzNikDi/PeriodicWorker?label=version)](https://github.com/ItzNikDi/PeriodicWorker/releases)

A lightweight Ktor plugin for running _periodic background_ tasks.

----

## Installation
0. Add the JitPack Maven repository:
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
```

### A. Using the version catalog

----
1. Add the plugin to `libs.versions.toml`:

```toml
[versions]
periodic-worker = "0.2.0"

[libraries]
periodic-worker = { module = "com.github.ItzNikDi:periodic-worker", version.ref = "periodic-worker" }
```

2. Add the plugin to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.periodic.worker)
}
```

----
### B. Adding directly to `build.gradle.kts`
```kotlin
dependencies {
    implementation("com.github.ItzNikDi:periodic-worker:0.2.0")
}
```
----

## Usage
```kotlin
install(PeriodicWorker) {
    every(period = 30.seconds) {
        println("how time flies by...")
    }
    
    every(period = 5.minutes, runsOnStart = true) { // this will also execute on startup
        clearOldSessions() // or other tasks to run sometimes...
    }
}
```

**Coroutine dispatchers**, **on-startup execution** and **periods of time** are configurable on a _per-task_ level.

----

## Testing

Uses virtual time in tests - no actual* delays.

\* One of the tests has a short delay :)