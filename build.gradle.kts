plugins {
    `java-library`
    id ("org.danilopianini.publish-on-central") version "9.1.14"
    signing
}

group = "io.github.javapaulvi"
version = "0.1.1"
description = "A lightweight Java API framework, inspired by FastAPI and ExpressJS"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("org.slf4j:slf4j-api:2.0.9")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// ------------------- Publish-on-Central Setup -------------------
publishOnCentral {
    repoOwner.set("javaPaulVI")
    projectDescription.set("A lightweight Java API framework, inspired by FastAPI and ExpressJS")
    projectLongName.set("Malacca")
    projectUrl.set("https://github.com/javaPaulVI/malacca")
    scmConnection.set("scm:git:https://github.com/javaPaulVI/malacca.git")
    licenseName.set("MIT License")
    licenseUrl.set("https://opensource.org/licenses/MIT")
}

// ------------------- Maven metadata -------------------
publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Malacca")
                description.set("A lightweight Java API framework, inspired by FastAPI and ExpressJS")
                url.set("https://github.com/javaPaulVI/malacca")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("paul")
                        name.set("Paul Hipper")
                        url.set("https://github.com/javaPaulVI")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/javaPaulVI/malacca.git")
                    developerConnection.set("scm:git:ssh://git@github.com/javaPaulVI/malacca.git")
                    url.set("https://github.com/javaPaulVI/malacca")
                }
            }
        }
    }
}

// ------------------- Signing -------------------
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

// ------------------- Dokka/Javadoc integration -------------------





// ------------------- Optional: Git Release Task -------------------
tasks.register("release") {
    group = "publishing"
    description = "Commit, tag, push, and upload to Maven Central Portal"

    doLast {
        val commitMessage: String by project
        val versionString = project.version.toString()
        val tagName = "v$versionString"
        val gradlewCmd = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "gradlew.bat" else "./gradlew"
        fun run(vararg cmd: String) {
            println("→ ${cmd.joinToString(" ")}")
            val process = ProcessBuilder(*cmd).directory(projectDir).inheritIO().start()
            val exit = process.waitFor()
            if (exit != 0) throw GradleException("Command failed: ${cmd.joinToString(" ")}")
        }

        // Git commit
        val status = ProcessBuilder("git", "status", "--porcelain").directory(projectDir)
            .start().inputStream.bufferedReader().readText().trim()
        if (status.isNotEmpty()) {
            run("git", "add", ".")
            run("git", "commit", "-m", "Release $versionString")
        } else println("→ No changes to commit")

        // Git tag
        run("git", "tag", "-a", tagName, "-m", "Version $versionString")
        run("git", "push", "origin", "main")
        run("git", "push", "--tags")

        // Maven Central Portal upload
        run(gradlewCmd, "publishAllPublicationsToProjectLocalRepository")
        run(gradlewCmd, "zipMavenCentralPortalPublication")
        run(gradlewCmd, "releaseMavenCentralPortalPublication")
    }
}