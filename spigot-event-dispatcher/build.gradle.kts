plugins {
    id("java-library")
}

group = "me.erano.com.spigot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // api: gercek spigot-api de bu annotasyonlari plugin'lere transitive olarak sunar.
    api("org.jetbrains:annotations:26.1.0")
    // Gercek spigot-api-26.2 pom'unda compile scope (Preconditions, ImmutableList...).
    api("com.google.guava:guava:33.6.0-jre")
}
