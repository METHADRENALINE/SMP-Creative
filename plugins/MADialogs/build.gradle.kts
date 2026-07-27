version = "1.20.2"

val paperApiVersion: String by project
val fancyNpcsVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("de.oliver:FancyNpcs:$fancyNpcsVersion")
}
