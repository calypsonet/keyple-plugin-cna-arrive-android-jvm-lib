rootProject.name = "keyple-plugin-cna-arrive-android-jvm-lib"
include(":plugin")
include(":plugin-mock")
include(":example-app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    }
}

