group = "app.revanced"

patches {
    about {
        name = "ReVanced Patches template"
        description = "Patches template for ReVanced"
        source = "git@github.com:revanced/revanced-patches-template.git"
        author = "ReVanced"
        contact = "contact@revanced.app"
        website = "https://revanced.app"
        license = "GNU General Public License v3.0"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/main/kotlin")
            include("**/*.js")
            include("**/*.py")
        }
    }
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
