plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.duopoly.ai.simulation.ExperimentalEvaluatorKt")
}

tasks.register<JavaExec>("runBenchmark") {
    group = "application"
    mainClass.set("com.duopoly.ai.simulation.PerformanceProfiler")
    classpath = sourceSets["main"].runtimeClasspath
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.tensorflow:tensorflow-lite:2.14.0") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
