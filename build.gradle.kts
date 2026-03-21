import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Exec
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.33.0"
    signing
}

group = "io.github.javapaulvi"
version = "0.1.1"


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
mavenPublishing {
    publishToMavenCentral()

    signAllPublications()
}


mavenPublishing {
    coordinates("io.github.javapaulvi", "malacca", version.toString())

    pom {
        name.set("Malacca")
        description.set("A lightweight Java API framework, inspired by FastAPI and ExpressJS")
        inceptionYear.set("2026")
        url.set("https://github.com/javaPaulVI/malacca")
        licenses {
            license {
                name.set("MIT Licence")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
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
            url.set("https://github.com/javaPaulVI/malacca")
            connection.set("scm:git:git://github.com/javaPaulVI/malacca.git")
            developerConnection.set("scm:git:ssh://git@github.com/javaPaulVI/malacca.git")
        }
    }
}



// -------------------------------------------------------------------------
// Custom release task
// -------------------------------------------------------------------------

tasks.register("release") {
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
        run(gradlewCmd, "publishMavenPublicationToMavenCentral")

        println("✅ Release $versionString completed successfully!")
    }
}