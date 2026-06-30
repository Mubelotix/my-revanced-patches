group = "app.revanced"

patches {
    about {
        name = "My ReVanced Patches"
        description = "Custom patches by Mubelotix"
        source = "git@github.com:Mubelotix/my-revanced-patches.git"
        author = "Mubelotix"
        contact = "https://github.com/Mubelotix"
        website = "https://github.com/Mubelotix/my-revanced-patches"
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
