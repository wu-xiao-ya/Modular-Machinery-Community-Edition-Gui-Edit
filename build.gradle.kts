plugins {
    id("java-library")
    id("maven-publish")
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.0"
}

group = "com.fushu.mmce"
version = "1.0.1-beta"

java {
    withSourcesJar()
}

minecraft {
    mcVersion.set("1.12.2")
    username.set("Developer")
    injectedTags.put("VERSION", project.version.toString())
}

repositories {
    mavenCentral()
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven {
        url = uri("https://cfa2.cursemaven.com")
    }
    maven {
        url = uri("https://cursemaven.com")
    }
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    }
}

dependencies {
    patchedMinecraft("me.eigenraven.java8unsupported:java-8-unsupported-shim:1.0.0")

    implementation(rfg.deobf("curse.maven:modular-machinery-community-edition-817377:7372953"))
    implementation("com.google.code.gson:gson:2.8.9")
    compileOnly(rfg.deobf("curse.maven:the-one-probe-245211:2667280"))
    compileOnly(rfg.deobf("curse.maven:ae2-extended-life-570458:6302098"))
    compileOnly(rfg.deobf("curse.maven:ae2-fluid-crafting-rework-623955:5504001"))
    compileOnly(rfg.deobf("curse.maven:Mekanism-268560:2835175"))
    compileOnly(rfg.deobf("curse.maven:mekanism-energistics-1027681:5408319"))
    compileOnly(rfg.deobf("curse.maven:had-enough-items-557549:4810661"))
    compileOnly("software.bernie.geckolib:geckolib-forge-1.12.2:3.0.31")

    testImplementation("junit:junit:4.13.2")
}

tasks.processResources.configure {
    inputs.property("version", project.version)
    inputs.property("mcversion", minecraft.mcVersion.get())

    filesMatching("mcmod.info") {
        expand(
            mapOf(
                "version" to project.version,
                "mcversion" to minecraft.mcVersion.get()
            )
        )
    }
}

tasks.compileJava.configure {
    sourceCompatibility = "1.8"
    options.encoding = "UTF-8"
    targetCompatibility = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

