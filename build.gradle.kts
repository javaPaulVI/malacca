plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.sonatype.central.publish") version "0.6.0"
}

group = "dev.javapaul"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}


dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:2.17.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.yourusername"
            artifactId = "malacca"
            version = "0.1.0"
            from(components["java"])

            pom {
                name = "Malacca"
                description = "A lightweight Java API framework, inspired by FastAPI  and ExpressJS"
                url = "https://github.com/yourusername/malacca"

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
                    connection = "scm:git:git://github.com/yourusername/malacca.git"
                    developerConnection = "scm:git:ssh://github.com:yourusername/malacca.git"
                    url = "https://github.com/yourusername/malacca/tree/main"
                }
            }
        }
    }
}
