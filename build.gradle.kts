plugins {
    base
}

allprojects {
    group = "net.methadrenaline.smpcreative"
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("javaVersion").get().toInt()))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(providers.gradleProperty("javaVersion").get().toInt())
    }

    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
    }

    tasks.withType<Jar>().configureEach {
        archiveBaseName.set(project.name)
    }
}
