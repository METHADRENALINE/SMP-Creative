version = "1.3.1"

val velocityApiVersion: String by project

dependencies {
    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityApiVersion")
}
