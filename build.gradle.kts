plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("6.1.0")
    id("io.github.slimjar").version("1.3.0")
}

group = "us.ajg0702"
version = "2.5.2-pre1"

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.ajg0702.us/") }
    maven { url = uri("https://repo.codemc.io/repository/nms/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("https://repo.citizensnpcs.co/") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
}

dependencies {
    testImplementation("junit:junit:4.12")

    implementation("io.github.slimjar:slimjar:1.2.7")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    //compileOnly(group = "org.spigotmc", name = "spigot", version = "1.18.2-R0.1-SNAPSHOT")
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.18.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("org.xerial:sqlite-jdbc:3.32.3.2")
    compileOnly("org.spongepowered:configurate-yaml:4.0.0")

    implementation("org.bstats:bstats-bukkit:1.7")

    implementation("net.kyori:adventure-api:4.10.0")
    implementation("net.kyori:adventure-text-minimessage:4.10.0")
    implementation("net.kyori:adventure-platform-bukkit:4.0.1")

    implementation("us.ajg0702:ajUtils:1.1.36")
    implementation("us.ajg0702.commands.platforms.bukkit:bukkit:1.0.0")
    implementation("us.ajg0702.commands.api:api:1.0.0")

    implementation(project(":nms"))


    slim("com.zaxxer:HikariCP:3.4.5")
    slim("com.h2database:h2:2.1.212")
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
