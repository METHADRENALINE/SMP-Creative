version = "1.20.3"

val paperApiVersion: String by project
val fancyNpcsVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("maven.modrinth:fancynpcs:$fancyNpcsVersion")
}
