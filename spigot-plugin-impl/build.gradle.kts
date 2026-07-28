plugins {
    id("java")
}

group = "me.erano.com.bukkit.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":spigot-event-dispatcher"))
}

tasks.test {
    useJUnitPlatform()
}