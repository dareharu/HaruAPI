import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

plugins {
  `java-library`
  `maven-publish`
  id("net.kyori.blossom")
  id("net.kyori.indra.git")
  id("com.github.johnrengelman.shadow")
  id("org.checkerframework")
  id("org.cadixdev.licenser")
}

apply(plugin = "com.github.johnrengelman.shadow")

val organization: String by project
val projectUrl: String by project
val projectDescription: String by project

repositories {
  mavenCentral()
}

dependencies {
  // Adventure, Powered by Kyori
  // Note that adventure modules ALWAYS FOLLOW THE LATEST VERSION.
  api("net.kyori:adventure-api")
  api("net.kyori:adventure-text-serializer-gson") {
    exclude(module = "gson")
    exclude(module = "adventure-api")
  }
  api("net.kyori:adventure-text-serializer-plain") {
    exclude(module = "adventure-api")
  }
  api("net.kyori:adventure-text-minimessage") {
    exclude(module = "adventure-api")
  }
  api("com.google.code.gson:gson:2.8.0") // dependency of "adventure-text-serializer-gson"

  implementation("com.google.guava:guava")

  // Dependency injection
  api("com.google.dagger:dagger:2.43.2")
  annotationProcessor("com.google.dagger:dagger-compiler:2.43.2")

  // High performance cache + guava - shaded guava
  api("com.github.ben-manes.caffeine:caffeine") {
    exclude(module = "checker-qual")
    exclude(module = "error_prone_annotations")
  }
  implementation("com.github.ben-manes.caffeine:guava") {
    exclude(module = "guava")
    exclude(module = "checker-qual")
    exclude(module = "error_prone_annotations")
  }

  // Configuration
  api("org.spongepowered:configurate-core") {
    exclude(module = "checker-qual")
    exclude(module = "error_prone_annotations")
  }
  api("org.spongepowered:configurate-hocon") {
    exclude(module = "configurate-core")
    exclude(module = "checker-qual")
    exclude(module = "error_prone_annotations")
  }

  // Database stuffs
  implementation("com.zaxxer:HikariCP")
  implementation("mysql:mysql-connector-java:8.0.30")
}

dependencies {
  constraints {
    api("net.kyori:adventure-api:4.11.0")
    api("net.kyori:adventure-text-serializer-gson:4.11.0")
    api("net.kyori:adventure-text-serializer-plain:4.11.0")
    api("net.kyori:adventure-text-minimessage:4.11.0")

    api("com.github.ben-manes.caffeine:caffeine:2.+") {
      because("@^3 requires Java 11 or adove")
    }
    implementation("com.github.ben-manes.caffeine:guava:2.+") {
      because("@^3 require Java 11 or adove")
    }

    api("org.spongepowered:configurate-core:4.1.2")
    api("org.spongepowered:configurate-hocon:4.1.2")

    implementation("com.google.guava:guava:21.0") {
      because("Soft tied to what's available from Minecraft")
    }

    implementation("com.zaxxer:HikariCP:4.+") {
      because("HikariCP@^5 requires Java 11 or adove")
    }
  }
}

blossom {
  replaceTokenIn("src/main/java/dareharu/haru/api/Haru.java")
  replaceToken("@version@", version)
}

val targetJavaVersion: Int = 8
java {
  val javaVersion = JavaVersion.toVersion(targetJavaVersion)
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
  if (JavaVersion.current() < javaVersion) {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
  }
}

tasks {
  jar {
    dependsOn(shadowJar)
  }

  withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
  }

  withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.WARN
  }

  withType(JavaCompile::class).configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
      options.release.set(targetJavaVersion)
    }

    options.apply {
      fork()
      compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-path", "-Xlint:-processing"))
      encoding = "UTF-8"
    }
  }

  shadowJar {
    archiveBaseName.set("HaruAPI")
    archiveVersion.set(version)
    archiveClassifier.set("universal")

    minimize {
      dependencies {
        exclude("**/*.proto")
        includeEmptyDirs = false
      }

      // Nah, we hate those.
      exclude("META-INF/**")
      exclude("**/package-info.java")
    }

    manifest {
      attributes(
        mapOf(
          "Specification-Title" to project.name,
          "Specification-Vendor" to organization,
          "Specification-Version" to project.version,
          "Implementation-Title" to project.name,
          "Implementation-Vendor" to organization,
          "Implementation-Version" to project.version,
          "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        )
      )

      if (indraGit.isPresent) {
        indraGit.applyVcsInformationToManifest(this)
      }
    }

    relocate("net.kyori", "dareharu.haru.dependency.kyori")

    relocate("com.google.common", "dareharu.haru.dependency.guava")
    relocate("com.google.thirdparty", "dareharu.haru.dependency.thirdparty")
    relocate("com.google.gson", "dareharu.haru.dependency.gson")
    relocate("com.google.protobuf", "dareharu.haru.dependency.protobuf")

    relocate("org.spongepowered.configurate", "dareharu.haru.dependency.spongepowered.configurate")
    relocate("io.leangen.geantyref", "dareharu.haru.dependency.geantyref")
    relocate("com.typesafe.config", "dareharu.haru.dependency.typesafe")

    relocate("com.github.benmanes.caffeine", "dareharu.haru.dependency.caffeine")

    relocate("com.zaxxer.hikari", "dareharu.haru.dependency.hikari")
    relocate("com.mysql", "dareharu.haru.dependency.mysql")
    relocate("org.slf4j", "dareharu.haru.dependency.slf4j")

    relocate("dagger", "dareharu.haru.dependency.dagger")
    relocate("javax.inject", "dareharu.haru.dependency.inject")
  }
}

license {
  header(rootProject.file("LICENSE_HEADER.txt"))
  properties {
    this["name"] = project.name
    this["organization"] = organization
    this["url"] = projectUrl
  }
  include("**/*.java")
  include("**/*.kt")
  include("**/*.groovy")
  include("**/*.scala")

  newLine(false)
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/dareharu/haruapi")
      credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
        password = project.findProperty("gpr.password") as String? ?: System.getenv("TOKEN")
      }
    }
  }

  publications {
    register<MavenPublication>("gpr") {
      from(components["java"])

      pom {
        artifactId = project.name.toLowerCase()
        this.name.set(project.name)
        this.description.set(projectDescription)
        this.url.set(projectUrl)

        licenses {
          license {
            this.name.set("MIT")
            this.url.set("https://opensource.org/licenses/MIT")
          }
        }

        scm {
          connection.set("scm:git:git://github.com/Dareharu/HaruAPI.git")
          developerConnection.set("scm:git:git://github.com/Dareharu/HaruAPI.git")
          this.url.set(projectUrl)
        }
      }
    }
  }
}

