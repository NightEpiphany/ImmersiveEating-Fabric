import java.net.URI

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

repositories {
	maven {
		name = "Modrinth"
		url = URI("https://api.modrinth.com/maven")
	}
	maven {
		name = "Fuzs Mod Resources"
		url = URI("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
	}
	maven {
		// location of the maven that hosts JEI files since January 2023
		name = "Jared's maven"
		url = URI("https://maven.blamejared.com/")
	}
	maven {
		url = URI("https://cursemaven.com")
	}
	exclusiveContent {
        forRepository {
            maven {
                name = "GeckoLib"
                url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
            }
        }
        filter { includeGroupAndSubgroups("com.geckolib") }
    }
}

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	implementation("com.geckolib:geckolib-fabric-${providers.gradleProperty("minecraft_version").get()}:${
		providers.gradleProperty(
			"geckolib_version"
		).get()
	}")
	implementation ("maven.modrinth:kaleidoscope-cookery-refabricated:${providers.gradleProperty("kaleidoscope_cookery_version").get()}-fabric+mc${providers.gradleProperty("minecraft_version").get()}")
	implementation ("maven.modrinth:kaleidoscope-tavern-refabricated:${providers.gradleProperty("kaleidoscope_tavern_version").get()}-fabric+mc${providers.gradleProperty("minecraft_version").get()}")
	implementation("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${providers.gradleProperty("forge_config_api_version").get()}")
	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
	// Punchy
	compileOnly("maven.modrinth:8aoMKplv:Ey8JJYWu")
}

tasks.test {
	useJUnitPlatform()
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
