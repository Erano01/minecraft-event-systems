plugins {
    id("java-library")
}

group = "me.erano.com.fabric"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Gercek fabric-api-base de Guava'nin MapMaker'ini kullanir (EventFactoryImpl.ARRAY_BACKED_EVENTS).
    implementation("com.google.guava:guava:33.5.0-jre")
    // fabric.mod.json okumak icin. Gercek loader kendi icine gommeli bir JSON okuyucu kullanir.
    implementation("com.google.code.gson:gson:2.13.2")
}
