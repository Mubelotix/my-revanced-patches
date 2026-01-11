package app.revanced.patches.reactrepl

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.util.reactnative.reactNativePatch

@Suppress("unused")
private object ReplResources

@Suppress("unused")
val replPatch = bytecodePatch(
    name = "Adds a REPL into a React Native app",
) {
    val ws_server = stringOption(key = "ws_server", required = true)

    dependsOn(reactNativePatch("repl_client.js") {
        val serverUrl = ws_server.value
        if (serverUrl == null) throw IllegalStateException("ws_server option is required")

        val resource = ReplResources::class.java.getResourceAsStream("repl_client.js")
            ?: throw IllegalStateException("repl_client.js not found")
        
        resource.bufferedReader().use { it.readText() }
            .replace("REPLACED_BY_CODE", serverUrl)
    })
}
