plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("io.github.slimjar").version("1.3.0")
}

group = "us.ajg0702"
version = "2.7.0"

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.ajg0702.us/releases/") }
    maven { url = uri("https://repo.codemc.io/repository/nms/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("https://repo.citizensnpcs.co/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation("io.github.slimjar:slimjar:1.2.7")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("org.xerial:sqlite-jdbc:3.42.0.0")
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")

    implementation("org.bstats:bstats-bukkit:3.0.2")

    implementation("net.kyori:adventure-api:4.14.0")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.0")

    implementation("us.ajg0702:ajUtils:1.2.12")
    implementation("us.ajg0702.commands.platforms.bukkit:bukkit:1.0.0")
    implementation("us.ajg0702.commands.api:api:1.0.0")

    compileOnly("net.luckperms:api:5.4")

    implementation(project(":nms:nms-legacy"))
    implementation(project(":nms:nms-19"))


    slim("com.zaxxer:HikariCP:5.0.1")
    slim("com.h2database:h2:2.1.214")
    //implementation("io.prometheus", "simpleclient", "0.9.0")
}

tasks.withType<ProcessResources> {
    include("**/*.yml")
    include("**/*.prop")
    include("**/*.zip")
    filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf(
                    "VERSION" to project.version.toString()
            )
    )
}

tasks.slimJar {
    relocate("org.h2", "us.ajg0702.leaderboards.libs.h2")
    relocate("com.zaxxer.hikari", "us.ajg0702.leaderboards.libs.hikari")
}

tasks.shadowJar {
    relocate("us.ajg0702.utils", "us.ajg0702.leaderboards.libs.utils")
    relocate("us.ajg0702.commands", "us.ajg0702.leaderboards.commands.base")
    relocate("io.github.slimjar", "us.ajg0702.leaderboards.libs.slimjar")
    relocate("net.kyori", "us.ajg0702.leaderboards.libs.kyori")
    relocate("org.bstats", "us.ajg0702.leaderboards.libs.bstats")
    relocate("org.spongepowered", "us.ajg0702.leaderboards.libs")
    relocate("org.yaml", "us.ajg0702.leaderboards.libs")
    relocate("io.leangen", "us.ajg0702.leaderboards.libs")

    archiveBaseName.set("ajLeaderboards")
    archiveClassifier.set("")
    exclude("junit/**/*")
    exclude("org/junit/**/*")
    exclude("org/slf4j/**/*")
    exclude("org/hamcrest/**/*")
    exclude("LICENSE-junit.txt")

    minimize {
        exclude(project(":nms:nms-19"))
    }
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
