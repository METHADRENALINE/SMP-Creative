version = "1.2.14"

val paperApiVersion: String by project
val protocolLibVersion: String by project
val fancyNpcsVersion: String by project
val postgresqlVersion: String by project
val hikariCpVersion: String by project
val shade by configurations.creating

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("net.dmulloy2:ProtocolLib:$protocolLibVersion")
    compileOnly("maven.modrinth:fancynpcs:$fancyNpcsVersion")
    shade("org.postgresql:postgresql:$postgresqlVersion")
    shade("com.zaxxer:HikariCP:$hikariCpVersion") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCpVersion")
}

tasks.jar {
    from(shade.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
