version = "1.0.14"

val paperApiVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}
