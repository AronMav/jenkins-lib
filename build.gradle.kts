import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication

plugins {
    java
    groovy
    jacoco
    id("com.mkobit.jenkins.pipelines.shared-library") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jenkins-ci.jpi") version "0.56.1" apply false
}

repositories {
    mavenCentral()
}

tasks {

    register<org.jenkinsci.gradle.plugins.jpi.TestDependenciesTask>("resolveIntegrationTestDependencies") {
        into {
            val javaExtension = project.extensions.getByType<JavaPluginExtension>()
            File("${javaExtension.sourceSets.getByName("integrationTest").output.resourcesDir}/test-dependencies")
        }
        configuration = configurations.integrationTestRuntimeClasspath.get()
    }
    processIntegrationTestResources {
        dependsOn("resolveIntegrationTestDependencies")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val junitVersion = "5.11.0"
val spockVersion = "1.3-groovy-2.4"
val groovyVersion = "2.4.21"
val slf4jVersion = "2.0.16"
val jsonschemaVersion = "4.38.0"

dependencies {
    implementation("org.codehaus.groovy", "groovy-all", groovyVersion)

    // Jakarta Servlet API for Jenkins 2.479+
    implementation("jakarta.servlet", "jakarta.servlet-api", "5.0.0")

    // jsonschema-generator
    implementation("com.github.victools", "jsonschema-generator", jsonschemaVersion)
    implementation("com.github.victools", "jsonschema-module-jackson", jsonschemaVersion)

    // unit-tests
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)

    testImplementation("org.assertj", "assertj-core", "3.26.3")
    testImplementation("org.mockito", "mockito-core", "5.21.0")

    testImplementation("org.slf4j", "slf4j-api", slf4jVersion)
    testImplementation("org.slf4j", "slf4j-simple", slf4jVersion)
    
    // integration-tests (Jakarta EE 9 compatible for Jenkins 2.479+)
    integrationTestImplementation("org.jenkins-ci.main", "jenkins-test-harness", "2307.v10e5d0701b_e5")

    integrationTestImplementation("org.spockframework", "spock-core", spockVersion)
    integrationTestImplementation("org.codehaus.groovy", "groovy-all", groovyVersion)

    integrationTestImplementation("org.springframework.security", "spring-security-core", "6.4.2")

    integrationTestImplementation("org.slf4j", "slf4j-api", slf4jVersion)
    integrationTestImplementation("org.slf4j", "slf4j-simple", slf4jVersion)

}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }

    reports {
        html.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.integrationTest)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacoco.xml"))
    }
}

jenkinsIntegration {
    baseUrl.set(uri("http://localhost:5050").toURL())
    authentication.set(providers.provider { AnonymousAuthentication })
    downloadDirectory.set(layout.projectDirectory.dir("jenkinsResources"))
}

sharedLibrary {
    // TODO: this will need to be altered when auto-mapping functionality is complete
    coreVersion.set(jenkinsIntegration.downloadDirectory.file("core-version.txt").map { it.asFile.readText().trim() })
    // TODO: retrieve downloaded plugin resource
    pluginDependencies {
        dependency("org.jenkins-ci.plugins", "pipeline-build-step", "551.v178956c49ef8")
        dependency("org.jenkins-ci.plugins", "pipeline-utility-steps", "2.18.0")
        dependency("org.jenkins-ci.plugins", "git", "5.8.1")
        dependency("org.jenkins-ci.plugins", "http_request", "1.20")
        dependency("org.jenkins-ci.plugins", "timestamper", "1.30")
        dependency("org.jenkins-ci.plugins", "credentials", "1393.v6017143c1763")
        dependency("org.jenkins-ci.plugins", "token-macro", "477.vd4f0dc3cb_cf1")
        dependency("org.jenkins-ci.plugins.workflow", "workflow-step-api", "686.v603d058a_e148")
        dependency("org.jenkins-ci.plugins.workflow", "workflow-job", "1500.v29502eb_5182e")

        dependency("org.jenkins-ci.modules", "sshd", "3.350.v1080103a_10fd")

        dependency("org.6wind.jenkins", "lockable-resources", "1349.v8b_ccb_c5487f7")
        dependency("ru.yandex.qatools.allure", "allure-jenkins-plugin", "2.34.0")
        dependency("io.jenkins.blueocean", "blueocean-pipeline-api-impl", "1.27.23")
        dependency("sp.sd", "file-operations", "353.vf3b_9b_a_f1f7f7")

        val declarativePluginsVersion = "2.2247.va_423189a_7dff"

        dependency("org.jenkinsci.plugins", "pipeline-model-api", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-definition", declarativePluginsVersion)
        dependency("org.jenkinsci.plugins", "pipeline-model-extensions", declarativePluginsVersion)
    }
}
