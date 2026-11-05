plugins {
    id("fabric-loom")
}

fun prop(key: String): String = sc.properties.get<String>(key)

version = "${prop("mod.version")}+${sc.current.version}"
base.archivesName = prop("mod.id")

repositories {
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" }
    maven("https://maven.isxander.dev/releases") { name = "Xander" }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${prop("deps.parchment_mc")}:${prop("deps.parchment")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runConfigs.all {
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.add("-Xlint:all,-processing,-serial,-classfile")
    }

    processResources {
        val props = mapOf(
            "id" to prop("mod.id"),
            "name" to prop("mod.name"),
            "version" to prop("mod.version"),
            "minecraft" to prop("mod.mc_compat"),
            "loader" to Regex("\\d+\\.\\d+").find(prop("deps.fabric_loader"))!!.value
        )
        props.forEach { (k, v) -> inputs.property(k, v) }
        filesMatching("fabric.mod.json") { expand(props) }
    }

    test {
        useJUnitPlatform()
    }

    withType<Jar> {
        from(rootProject.file("../LICENSE")) { rename { "${it}_${prop("mod.id")}" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${prop("mod.version")}"))
        dependsOn(build)
    }
}
