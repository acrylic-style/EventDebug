plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "xyz.acrylicstyle"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/public/") }
    maven { url = uri("https://repo.blueberrymc.net/repository/maven-public/") }
}

dependencies {
    implementation("net.blueberrymc:native-util:2.1.0")
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.4")
    implementation("org.ow2.asm:asm-tree:9.4")
    implementation("org.ow2.asm:asm-util:9.4")
    compileOnly("org.spigotmc:spigot-api:1.15.2-R0.1-SNAPSHOT")
}
