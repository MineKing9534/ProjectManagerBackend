plugins {
    kotlin("jvm") version "1.9.23"

	idea
	id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "de.mineking"
version = "2.0.0"

repositories {
    mavenCentral()
	maven("https://maven.mineking.dev/releases")
	maven("https://maven.mineking.dev/snapshots")
}

dependencies {
	implementation("de.mineking:JavaUtils:1.9.0")
	implementation("de.mineking.KORMite:KORMite-core:master")
	implementation("de.mineking.KORMite:KORMite-postgres:master")

	implementation("io.javalin:javalin:6.4.0")
	implementation("org.simplejavamail:simple-java-mail:8.10.1")

	implementation("com.auth0:java-jwt:4.4.0")
	implementation("com.password4j:password4j:1.8.1")

	implementation("io.github.cdimascio:java-dotenv:5.2.2")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("commons-io:commons-io:2.16.1")
	implementation("org.apache.tika:tika-core:2.9.2")

	implementation("ch.qos.logback:logback-classic:1.5.15")

	implementation("org.postgresql:postgresql:42.7.3")
	implementation("org.jdbi:jdbi3-postgres:3.45.1")
	implementation("org.jdbi:jdbi3-kotlin:3.45.1")

    testImplementation(kotlin("test"))
	implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		javaParameters = true
	}
}

tasks.compileKotlin {
	dependsOn("compileEmail")
}

tasks.jar {
	manifest {
		attributes(mapOf("Main-Class" to "de.mineking.manager.main.MainKt"))
	}
}

tasks.register<Exec>("compileEmail") {
	dependsOn("prepareEmail")

	delete(layout.buildDirectory.dir("resources/main/email"))
	mkdir(layout.buildDirectory.dir("resources/main/email"))
	commandLine("bootstrap-email", "-p", "build/email/*.html", "-d", "build/resources/main/email")
}

tasks.register<Copy>("prepareEmail") {
	delete(layout.buildDirectory.dir("email"))

	from("$projectDir/bootstrap_email_templates") {
		include("*.html")

		expand(
			"name" to project.findProperty("NAME"),
			"phone" to project.findProperty("PHONE"),
			"email" to project.findProperty("EMAIL"),
			"url" to project.findProperty("URL")
		)
	}

	into(layout.buildDirectory.dir("email"))

	includeEmptyDirs = false
}