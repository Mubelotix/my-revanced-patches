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
            const-string v1, "revanced-plugin.js"
            const/4 v2, 0x0
            invoke-virtual {p0, v0, v1, v2}, Lcom/facebook/react/bridge/CatalystInstanceImpl;->loadScriptFromAssets(Landroid/content/res/AssetManager;Ljava/lang/String;Z)V
        """

        val implementation = mutableMethod.implementation ?: return@execute
        val instructions = implementation.instructions.toList()
        val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }

        if (returnIndex != -1) {
            mutableMethod.addInstructions(returnIndex, loadScriptSmali)
        }
    }
}
