plugins {
    id("java")
    id("application")
}

group = "me.erano.com.bukkit.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":spigot-event-dispatcher"))
}

application {
    mainClass.set("me.erano.com.bukkit.example.Main")
}

tasks.test {
    useJUnitPlatform()
}
