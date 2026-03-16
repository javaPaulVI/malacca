plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.36.0"
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

mavenPublishing {
    coordinates("io.github.javapaulvi", "malacca", "0.1.0")

    pom {
        name.set("Malacca")
        description.set("A lightweight Java API framework, inspired by FastAPI and ExpressJS")
        inceptionYear.set("2026")
        url.set("https://github.com/javaPaulVI/malacca#/")
        licenses {
            license {
                name.set("MIT Licence")
                url.set("https://opensource.org/license/MIT")
                distribution.set("https://opensource.org/license/MIT")
            }
        }
        developers {
            developer {
                id.set("javaPaulVI")
                name.set("Paul Hipper")
                url.set("https://github.com/javaPaulVI/")
            }
        }
        scm {
            url.set("https://github.com/javaPaulVI/malacca#/")
            connection.set("scm:git:git://github.com/javaPaulVI/malacca.git")
            developerConnection.set("scm:git:ssh://git@github.com/javaPaulVI/malacca.git")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()
}
