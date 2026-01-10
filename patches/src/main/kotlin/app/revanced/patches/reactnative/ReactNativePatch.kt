package app.revanced.patches.reactnative

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode

val reactNativePatch = bytecodePatch(
    name = "React Native Injection",
    description = "Injects a JavaScript bundle into React Native applications."
) {
    compatibleWith("com.none.by.default"("0.0"));

    val jsCode = stringOption(
        key = "jsCode",
        description = "The JavaScript code to inject.",
        default = "console.log('ReVanced: Hello from injected JS!');"
    )

    dependsOn(rawResourcePatch {
        execute {
            val file = this.get("assets/revanced-plugin.js", true)
            file.parentFile?.mkdirs()
            file.writeText(jsCode.value ?: "")
        }
    })

    execute {
        val catalystInstanceImpl = classes.find { it.type == "Lcom/facebook/react/bridge/CatalystInstanceImpl;" } ?: return@execute
        val mutableMethod = proxy(catalystInstanceImpl).mutableClass.methods.find { it.name == "runJSBundle" } ?: return@execute

        val loadScriptSmali = """
            invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
            move-result-object v0
            invoke-virtual {v0}, Landroid/app/Application;->getAssets()Landroid/content/res/AssetManager;
            move-result-object v0
            const-string v1, "Revanced"
            const-string v2, "REVANCED: About to load React Native patch: revanced-plugin.js"
            invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
            const-string v1, "assets://revanced-plugin.js"
            const/4 v2, 0x0
            invoke-direct {p0, v0, v1, v2}, Lcom/facebook/react/bridge/CatalystInstanceImpl;->jniLoadScriptFromAssets(Landroid/content/res/AssetManager;Ljava/lang/String;Z)V
            const-string v0, "Revanced"
            const-string v1, "REVANCED: React Native patch applied successfully"
            invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
        """

        val implementation = mutableMethod.implementation ?: return@execute
        val returnInstructionIndex = implementation.instructions.indexOfFirst { it.opcode == Opcode.RETURN_VOID }
        if (returnInstructionIndex != -1) {
            mutableMethod.addInstructions(returnInstructionIndex, loadScriptSmali)
        }
    }
}
