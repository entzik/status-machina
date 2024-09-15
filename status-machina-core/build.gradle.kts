/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java Library project to get you started.
 * For more details take a look at the Java Libraries chapter in the Gradle
 * user guide available at https://docs.gradle.org/5.0/userguide/java_library_plugin.html
 */

plugins {
    // Apply the java-library plugin to add support for Java Library
    `java-library`
    `maven-publish`
    `idea`
    `eclipse`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    // api("org.apache.commons:commons-math3:3.6.1")
    api("com.google.guava:guava:28.1-jre")
    implementation("org.slf4j:slf4j-api:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    testImplementation("org.assertj:assertj-core:3.4.1")
}

tasks.getByName("jar") {
    enabled = true
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

configurations {
    compileClasspath {
        resolutionStrategy.activateDependencyLocking()
    }
}

val MAVEN_UPLOAD_USER: String by project
val MAVEN_UPLOAD_PWD: String by project

/*
val MAVEN_UPLOAD_USER: String by project.extra("defaultUsername")
project.extra["MAVEN_UPLOAD_USER"] = findProperty("MAVEN_UPLOAD_USER")?.toString() ?: "defaultUsername"

val MAVEN_UPLOAD_PWD: String  by project.extra("defaultpwd")
project.extra["MAVEN_UPLOAD_PWD"] = findProperty("MAVEN_UPLOAD_PWD")?.toString() ?: "defaultUsername"
*/


publishing {
    repositories {
        maven {
            name = "MavenCentral"
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = MAVEN_UPLOAD_USER
                password = MAVEN_UPLOAD_PWD
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                name.set("Status Machina Core")
                description.set("Core functionality for Status Machina, a small, simple and pragmatic state machine for resilient microservices orchestration.")
                url.set("https://github.com/entzik/status-machina")
/*
                properties.set(mapOf(
                        "myProp" to "value",
                        "prop.with.dots" to "anotherValue"
                ))
*/
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("entzik")
                        name.set("Emil Kirschner")
                        email.set("entzik@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/entzik/status-machina.git")
                    developerConnection.set("scm:git:https://github.com/entzik/status-machina.git")
                    url.set("https://github.com/entzik/status-machina")
                }

            }
        }
    }
}

signing {
    val PGP_SIGNING_KEY: String? by project
    val PGP_SIGNING_PASSWORD: String? by project
    useInMemoryPgpKeys(PGP_SIGNING_KEY, PGP_SIGNING_PASSWORD)
    sign(publishing.publications["mavenJava"])
}
