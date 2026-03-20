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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Logging: only API, no implementation
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
// Maven Publishing (STANDARD + CORRECT)
// -------------------------------------------------------------------------

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

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
            name = "central"

            val releasesRepoUrl = uri("https://central.sonatype.com/repository/maven-releases/")
            val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")

            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = findProperty("centralUsername") as String?
                    ?: System.getenv("MAVEN_USERNAME")

                password = findProperty("centralPassword") as String?
                    ?: System.getenv("MAVEN_PASSWORD")
            }
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
// SAFE RELEASE TASK
// -------------------------------------------------------------------------

// -------------------------------------------------------------------------
// SAFE RELEASE TASK — PRODUCTION READY
// -------------------------------------------------------------------------
tasks.register("release") {
    group = "publishing"
    description = "Safe local release: test → publish → commit → tag → push"

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

        fun output(vararg cmd: String): String {
            val process = ProcessBuilder(*cmd)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            return process.inputStream.bufferedReader().readText().trim()
        }

        println("════════════════════════════════════════")
        println("Releasing Malacca $version")
        println("Tag: $tag")
        println("════════════════════════════════════════")

        // 1️⃣ Ensure git working directory is clean
        val status = output("git", "status", "--porcelain")
        if (status.isNotBlank()) {
            error("Git working directory is not clean. Commit or stash changes first.")
        }

        // 2️⃣ Check if tag exists remotely
        val remoteTags = output("git", "ls-remote", "--tags", "origin")
        if (remoteTags.contains(tag)) {
            error("Tag $tag already exists remotely.")
        }

        // 3️⃣ Run tests
        run("./gradlew.bat", "test")

        // 4️⃣ Stage and commit relevant files (safe with .gitignore)
        if (pCommitMessage.isNotBlank()) {
            run("git", "add", "*")
            run("git", "commit", "-m", commitMessage)
        } else {
            println("No changes to commit, proceeding with release.")
        }

        // 5️⃣ Create annotated tag pointing to the release commit
        run("git", "tag", "-a", tag, "-m", "Release $version")

        // 6️⃣ Push commit and tag
        run("git", "push", "origin", "main")
        run("git", "push", "origin", tag)

        // 7️⃣ Publish to Maven Central
        run("./gradlew.bat", "publish")

        println("════════════════════════════════════════")
        println("✓ Release successful: $version")
        println("════════════════════════════════════════")
    }
}