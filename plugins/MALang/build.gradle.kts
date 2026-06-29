version = "1.2.10"

val paperApiVersion: String by project
val protocolLibVersion: String by project
val fancyNpcsVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("com.comphenix.protocol:ProtocolLib:$protocolLibVersion")
    compileOnly("de.oliver:FancyNpcs:$fancyNpcsVersion")
}
