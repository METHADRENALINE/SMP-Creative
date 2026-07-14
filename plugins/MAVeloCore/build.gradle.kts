version = "1.4.0"

val velocityApiVersion: String by project
val postgresqlVersion: String by project
val hikariCpVersion: String by project
val shade by configurations.creating

dependencies {
    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityApiVersion")
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
