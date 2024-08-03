dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.version.toml"))
        }
    }
}

rootProject.name = "dynamodb-kotlin-module"
