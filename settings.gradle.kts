pluginManagement {
  repositories {
    maven("https://repo.spongepowered.org/repository/maven-public") {
      name = "Sponge"
    }
    mavenCentral()
    gradlePluginPortal()
  }

  plugins {
    val indraVersion = "2.1.1"
    id("net.kyori.blossom") version "1.3.+"
    id("net.kyori.indra.publishing") version indraVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.checkerframework") version "0.6.15"
    id("org.cadixdev.licenser") version "0.6.1"
  }
}

dependencyResolutionManagement {
  repositories {
    maven("https://repo.spongepowered.org/repository/maven-public") {
      name = "Sponge"
    }
  }
}

rootProject.name = "HaruAPI"
