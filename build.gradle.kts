import java.io.File

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "io.github.javapaulvi"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.5.13")

    // Testing
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.javapaulvi"
            artifactId = "malacca"
            version = project.version.toString()
            from(components["java"])

            pom {
                name = "Malacca"
                description = "A lightweight Java API framework inspired by FastAPI"
                url = "https://github.com/javaPaulVI/malacca"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        name = "Paul Hipper"
                        email = "paul@be-hip.eu"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/javaPaulVI/malacca.git"
                    developerConnection = "scm:git:ssh://github.com:javaPaulVI/malacca.git"
                    url = "https://github.com/javaPaulVI/malacca/tree/main"
                }
            }
        }
    }
}

signing {
    val keyFile = File(System.getProperty("user.home") + "/.gpg/keyfile")

    if (keyFile.exists()) {
        val keyContent = keyFile.readText(Charsets.UTF_8)  // read raw ASCII-armored key
        val password = System.getenv("SIGNING_PASSWORD") ?: error("SIGNING_PASSWORD is missing")

        useInMemoryPgpKeys(keyContent, password)
        println("Using in-memory signing key from ${keyFile.absolutePath}")
    } else {
        println("No key file found, using gpg command if available")
        useGpgCmd()  // fallback
    }

    sign(publishing.publications["maven"])
}
// -------------------------------------------------------------------------
// Release task — commit, tag and push to trigger GitHub Actions publish
// -------------------------------------------------------------------------

tasks.register("release") {
    group = "publishing"
    description = "Commits all changes, creates a tag and pushes to GitHub. " +
            "Usage: ./gradlew release -Pmessage=\"commit message\" -Ptag=\"v0.1.0\""

    doLast {
        val commitMessage = project.findProperty("message") as String?
            ?: error("Commit message required — run with -Pmessage=\"your message\"")
        val tag = project.findProperty("tag") as String?
            ?: error("Tag required — run with -Ptag=\"v0.1.0\"")

        fun run(vararg cmd: String) {
            val result = ProcessBuilder(*cmd)
                .directory(projectDir)
                .inheritIO()
                .start()
                .waitFor()
            if (result != 0) error("Command failed: ${cmd.joinToString(" ")}")
        }

        run("git", "add", ".")
        run("git", "commit", "-m", "\"$commitMessage $tag\"")
        run("git", "tag", tag)
        run("git", "push", "origin", "main")
        run("git", "push", "origin", tag)

        println("Released $tag — GitHub Actions will publish to Maven Central")
    }
}