plugins {
    java
    `maven-publish`
}

group = "us.ajg0702.leaderboards.nms-legacy"
version = project(":").version

repositories {
    mavenCentral()
    maven { url = uri("https://repo.codemc.io/repository/nms/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-releases/") }
    maven { url = uri("https://repo.ajg0702.us/releases/") }
    maven { url = uri("https://repo.minebench.de/") }
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot", version = "1.19-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    compileOnly("us.ajg0702:ajUtils:1.2.29")
    compileOnly("net.skinsrestorer:skinsrestorer-api:14.1.10")
    compileOnly("com.squareup.okhttp3:okhttp:4.11.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks["jar"])
        }
    }

    repositories {

        val mavenUrl = "https://repo.ajg0702.us/releases"

        if(!System.getenv("REPO_TOKEN").isNullOrEmpty()) {
            maven {
                url = uri(mavenUrl)
                name = "ajRepo"

                credentials {
                    username = "plugins"
                    password = System.getenv("REPO_TOKEN")
                }
            }
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}