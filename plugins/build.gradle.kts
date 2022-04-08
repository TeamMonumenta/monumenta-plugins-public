plugins {
    id("com.palantir.git-version") version "0.12.2"
}

description = "monumenta-plugins"
val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()
