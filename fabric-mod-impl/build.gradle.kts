plugins {
    id("application")
}

group = "me.erano.com.fabric.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":fabric-event-callback-api"))
}

application {
    mainClass = "me.erano.com.fabric.example.Main"
}
