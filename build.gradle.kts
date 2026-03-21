plugins {
    `java-library`
    id ("org.danilopianini.publish-on-central") version "9.1.14"
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

// --------------------- Publish-on-Central Config ---------------------

publishOnCentral {
    repoOwner.set("javaPaulVI")
    projectDescription.set("A lightweight Java API framework, inspired by FastAPI and ExpressJS")
    projectLongName.set("Malacca")
    licenseName.set("MIT License")
    licenseUrl.set("https://opensource.org/license/mit")
    projectUrl.set("https://github.com/javaPaulVI/malacca")
    scmConnection.set("scm:git:https://github.com/javaPaulVI/malacca.git")
}

// Signing config
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

// --------------------- Safe Release Task ---------------------

val commitMessage: String = findProperty("message")?.toString() ?: "Release version $version"
val tagName: String = "v$version"
val gradlewCmd: String = if (System.getProperty("os.name").contains("Windows", true)) "gradlew.bat" else "./gradlew"

tasks.register("release") {
    group = "publishing"
    description = "Commits, tags, pushes, and publishes to Maven Central"

    doLast {
        fun run(vararg cmd: String) {
            println("→ ${cmd.joinToString(" ")}")
            val process = ProcessBuilder(*cmd)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            println(output)
            if (process.waitFor() != 0) throw GradleException("Command failed: ${cmd.joinToString(" ")}\n$output")
        }

        // ---------- Git commit ----------
        val status = ProcessBuilder("git", "status", "--porcelain")
            .directory(projectDir)
            .start()
            .inputStream.bufferedReader().readText().trim()

        if (status.isNotEmpty()) {
            run("git", "add", ".")
            run("git", "commit", "-m", commitMessage)
        } else println("→ No changes to commit")

        // ---------- Delete existing tags ----------
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

        // ---------- Create new tag ----------
        println("→ Creating Git tag $tagName")
        run("git", "tag", "-a", tagName, "-m", "Version $version")

        // ---------- Push commits and tags ----------
        println("→ Pushing commits and tags")
        run("git", "push", "origin", "main")
        run("git", "push", "--tags")

        // ---------- Publish to Maven Central ----------
        println("→ Publishing version $version to Maven Central")
        run(gradlewCmd, "publishAllPublicationsToProjectLocalRepository")
        run(gradlewCmd, "zipMavenCentralPortalPublication")
        run(gradlewCmd, "releaseMavenCentralPortalPublication")

        println("✅ Release $version completed successfully!")
    }
}