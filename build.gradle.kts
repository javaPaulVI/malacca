import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Exec
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.javapaulvi"
version = "0.1.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    archiveBaseName.set("malacca") // keep Maven coordinates standard
}



tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
}
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// -------------------------------------------------------------------------
// Publishing to Maven Central
// -------------------------------------------------------------------------

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            //artifact(tasks["sourcesJar"])
            //artifact(tasks["javadocJar"])

            pom {
                name.set("Malacca")
                description.set("A lightweight Java API framework inspired by FastAPI")
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
                        email.set("paul@be-hip.eu")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/javaPaulVI/malacca.git")
                    developerConnection.set("scm:git:ssh://github.com:javaPaulVI/malacca.git")
                    url.set("https://github.com/javaPaulVI/malacca")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH" // this should match <id> in settings.xml if using Maven, not required for Gradle env vars
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

// -------------------------------------------------------------------------
// Signing artifacts
// -------------------------------------------------------------------------

signing {
    useGpgCmd() // or useInMemoryPgpKeys() for CI
    sign(publishing.publications["mavenJava"])
}

// -------------------------------------------------------------------------
// Custom release task
// -------------------------------------------------------------------------

tasks.register("releaseToMavenCentral") {
    group = "publishing"
    description = "Commits, tags, pushes, and publishes to Maven Central"

    doLast {
        // ------------- Helper function -------------
        fun run(vararg cmd: String) {
            println("→ ${cmd.joinToString(" ")}")
            val process = ProcessBuilder(*cmd)
                .directory(projectDir)
                .redirectErrorStream(true) // merge stdout + stderr
                .start()

            val output = process.inputStream.bufferedReader().readText()
            println(output)

            val exit = process.waitFor()
            if (exit != 0) throw GradleException("Command failed: ${cmd.joinToString(" ")}\n$output")
        }

        // ------------- Parameters -------------
        val commitMessage = findProperty("message")?.toString() ?: "Release version ${project.version}"
        val versionString = project.version.toString()
        val tagName = "v$versionString"
        val gradlewCmd =
            if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "gradlew.bat" else "./gradlew"

        // ------------- Git commit -------------
        val status = ProcessBuilder("git", "status", "--porcelain")
            .directory(projectDir)
            .start()
            .inputStream.bufferedReader().readText().trim()

        if (status.isNotEmpty()) {
            run("git", "add", ".")
            run("git", "commit", "-m", commitMessage)
        } else {
            println("→ No changes to commit")
        }

        // ------------- Delete existing tags -------------
        val tagExists = runCatching {
            ProcessBuilder("git", "rev-parse", "--verify", tagName)
                .directory(projectDir)
                .start()
                .waitFor() == 0
        }.getOrElse { false }

        if (tagExists) {
            println("→ Deleting existing local tag $tagName")
            run("git", "tag", "-d", tagName)
            println("→ Deleting existing remote tag $tagName")
            run("git", "push", "origin", ":refs/tags/$tagName")
        }

        // ------------- Create new tag -------------
        println("→ Creating Git tag $tagName")
        run("git", "tag", "-a", tagName, "-m", "Version $versionString")

        // ------------- Push commits and tags -------------
        println("→ Pushing commits and tags")
        run("git", "push", "origin", "main")
        run("git", "push", "--tags")

        // ------------- Publish to Maven Central -------------
        println("→ Publishing version $versionString to Maven Central")
        run(gradlewCmd, "publish")

        println("✅ Release $versionString completed successfully!")
    }
}