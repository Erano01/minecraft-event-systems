plugins {
    id("java")
}

group = "me.erano.com.forge.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":forge-event-bus"))
}
