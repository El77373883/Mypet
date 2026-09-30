plugins {
    java
}

group = "com.soyadrianyt001"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveFileName.set("Mypet-1.0.jar")
    destinationDirectory.set(file("$buildDir/libs"))
    manifest {
        attributes["Main-Class"] = "com.soyadrianyt001.mypet.Mypet"
    }
}
