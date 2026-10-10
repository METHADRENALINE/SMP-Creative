version = "1.4.2"

val velocityApiVersion: String by project
val postgresqlVersion: String by project
val hikariCpVersion: String by project
val shade by configurations.creating

dependencies {
    testImplementation("com.velocitypowered:velocity-api:$velocityApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityApiVersion")
    shade("org.postgresql:postgresql:$postgresqlVersion")
    shade("com.zaxxer:HikariCP:$hikariCpVersion") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:$hikariCpVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(shade.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
