tasks.register("test") {
    rootProject.subprojects.forEach {
        dependsOn(":${it.name}:test")
    }
}
