plugins {
    `java-library`
    `maven-publish`
    `idea`
    `eclipse`
    signing
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":status-machina-core"))
    compileOnly("org.springframework.boot:spring-boot-starter")
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.springframework.retry:spring-retry")
    compileOnly("jakarta.validation:jakarta.validation-api")
    implementation("com.google.guava:guava:28.1-jre")

    testImplementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.retry:spring-retry")
    testImplementation("jakarta.validation:jakarta.validation-api")

    testImplementation("org.junit.jupiter:junit-jupiter") {
        version {
            prefer("5.10.3")
        }
    }
    testImplementation("org.postgresql:postgresql")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.5.1")
    testImplementation("io.zonky.test:embedded-postgres:1.2.10")
    testImplementation("org.liquibase:liquibase-core:4.27.0")
    testImplementation("org.assertj:assertj-core:3.4.1")
}

tasks.jar {
    enabled = true
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.map { it.destinationDir })
}

tasks.bootJar {
    enabled = false
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
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            pom {
                name.set("Status Machina Spring")
                description.set("Spring integration for Status Machina, a small, simple and pragmatic state machine for resilient microservices orchestration.")
                url.set("https://github.com/entzik/status-machina")
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