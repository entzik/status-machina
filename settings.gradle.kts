pluginManagement {
    // Single source of truth for the Spring Boot build baseline. Defaults to the springBootVersion
    // property in gradle.properties and can be overridden on the command line with
    // -PspringBootVersion=<x.y.z> (the -P value takes precedence over the gradle.properties default).
    val springBootVersion: String by settings
    plugins {
        id("org.springframework.boot") version springBootVersion
    }
}

include("status-machina-core")
include("status-machina-spring")
//include("status-machina-atomix")
