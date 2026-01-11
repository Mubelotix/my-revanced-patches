package app.revanced.patches.reactrepl

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.util.reactnative.reactNativePatch

@Suppress("unused")
private object ReplResources

@Suppress("unused")
val replPatch = bytecodePatch(
    name = "React Native REPL",
    description = "Adds a REPL inside the VM of any React Native app for you to dig into",
    use = false,
) {
    val ws_server = stringOption(key = "ws_server", required = true)

    dependsOn(reactNativePatch("repl_client.js") {
        var serverUrl = ws_server.value
        if (serverUrl == null) throw IllegalStateException("ws_server option is required")

        if (serverUrl.startsWith("https://")) {
            serverUrl = serverUrl.replaceFirst("https://", "wss://")
        } else if (serverUrl.startsWith("http://")) {
            serverUrl = serverUrl.replaceFirst("http://", "ws://")
        } else if (!serverUrl.contains("://")) {
            serverUrl = "wss://$serverUrl"
        }

        val resource = ReplResources::class.java.getResourceAsStream("repl_client.js")
            ?: throw IllegalStateException("repl_client.js not found")
        
        resource.bufferedReader().use { it.readText() }
            .replace("REPLACED_BY_CODE", serverUrl)
    })
}
