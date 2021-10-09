plugins {
    java
    id("com.github.johnrengelman.shadow").version("6.1.0")
    `maven-publish`
}

group = "us.ajg0702"
version = "1.3.3"

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://gitlab.com/api/v4/projects/19978391/packages/maven") }
    maven { url = uri("https://repo.codemc.io/repository/nms/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("http://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("https://repo.citizensnpcs.co/") }
}

dependencies {
    testImplementation("junit:junit:4.12")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly(group = "org.spigotmc", name = "spigot", version = "1.16.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.10.4")
    compileOnly("org.xerial:sqlite-jdbc:3.32.3.2")

    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.bstats:bstats-bukkit:1.7")
    implementation("us.ajg0702:ajUtils:1.0.0")
}

tasks.withType<ProcessResources> {
    include("**/*.yml")
    filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf(
                    "VERSION" to project.version.toString()
            )
    )
}

tasks.shadowJar {
    relocate("org.bstats", "us.ajg0702.leaderboards.libs")
    relocate("us.ajg0702.utils", "us.ajg0702.leaderboards.libs")
    relocate("com.zaxxer.hikari", "us.ajg0702.leaderboards.libs.hikari")
    archiveFileName.set("${baseName}-${version}.${extension}")
    exclude("junit/**/*")
    exclude("org/junit/**/*")
    exclude("org/hamcrest/**/*")
    exclude("LICENSE-junit.txt")
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


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
