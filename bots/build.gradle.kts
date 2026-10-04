plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":engine"))
    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// B4: ./gradlew :bots:sim --args="--games=10000"
tasks.register<JavaExec>("sim") {
    group = "verification"
    description = "Plays headless matches and reports win rates, crashes and stuck matches."
    mainClass.set("com.kingofxitter.bots.SimulatorCliKt")
    classpath = sourceSets.main.get().runtimeClasspath
}
