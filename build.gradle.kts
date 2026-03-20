import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
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

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// -------------------------------------------------------------------------
// Maven Publishing (Local Only)
// -------------------------------------------------------------------------

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

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
            name = "localRepo"
            url = uri("${buildDir}/local-maven")
        }
    }
}

// -------------------------------------------------------------------------
// Signing
// -------------------------------------------------------------------------

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

// -------------------------------------------------------------------------
// SAFE RELEASE TASK — LOCAL UPLOAD ONLY
// -------------------------------------------------------------------------

tasks.register("release") {
    group = "publishing"
    description = "Local safe release: test → tag → push → publish locally"

    doLast {
        val version = project.version.toString()
        val tag = "v$version"
        val pCommitMessage = (project.findProperty("message") as String?).orEmpty()
        val commitMessage = if (pCommitMessage.isNotBlank()) "$pCommitMessage $tag" else "Release $tag"

        fun run(vararg cmd: String) {
            println("→ ${cmd.joinToString(" ")}")
            val process = ProcessBuilder(*cmd)
                .directory(projectDir)
                .inheritIO()
                .start()
            val exit = process.waitFor()
            if (exit != 0) error("Command failed: ${cmd.joinToString(" ")}")
        }

        println("════════════════════════════════════════")
        println("Releasing Malacca $version")
        println("Tag: $tag")
        println("════════════════════════════════════════")

        // 1️⃣ Run tests & build
        run("./gradlew.bat", "test")
        run("./gradlew.bat", "build")

        // 2️⃣ Safe Git commit (skip if nothing to commit)
        if (pCommitMessage.isNotBlank()) {
            try {
                run("git", "add", "*")
                run("git", "commit", "-m", commitMessage)
            } catch (e: Exception) {
                println("⚠ Git commit skipped: ${e.message}")
            }
        } else {
            println("No changes to commit, proceeding with release.")
        }

        // 3️⃣ Create annotated tag (will fail if tag exists)
        try {
            run("git", "tag", "-a", tag, "-m", "Release $version")
        } catch (e: Exception) {
            println("⚠ Tag creation skipped: ${e.message}")
        }

        // 4️⃣ Push commit and tag
        try {
            run("git", "push", "origin", "main")
            run("git", "push", "origin", tag)
        } catch (e: Exception) {
            println("⚠ Git push skipped: ${e.message}")
        }

        // 5️⃣ Publish locally
        run("./gradlew.bat", "publish")

        println("════════════════════════════════════════")
        println("✓ Local release completed: $version")
        println("Artifacts are in: build/local-maven")
        println("════════════════════════════════════════")
    }
}