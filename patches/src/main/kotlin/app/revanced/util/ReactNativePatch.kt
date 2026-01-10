package app.revanced.util

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

object ReactNativePatch {
    fun patch(context: BytecodePatchContext, jsCode: String, executeFirst: Boolean) {
        context.resources.add("assets/revanced-plugin.js", jsCode)

        val catalystInstanceImpl = context.classes.find { it.name == "Lcom/facebook/react/bridge/CatalystInstanceImpl;" } ?: return
        val runJSBundle = catalystInstanceImpl.methods.find { it.name == "runJSBundle" } ?: return

        // Smali code to load the script from assets
        // v0: AssetManager
        // v1: String (filename)
        // v2: boolean (loadSynchronously)
        val loadScriptSmali = """
            invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
            move-result-object v0
            invoke-virtual {v0}, Landroid/app/Application;->getAssets()Landroid/content/res/AssetManager;
            move-result-object v0
            const-string v1, "revanced-plugin.js"
            const/4 v2, 0x0
            invoke-virtual {p0, v0, v1, v2}, Lcom/facebook/react/bridge/CatalystInstanceImpl;->loadScriptFromAssets(Landroid/content/res/AssetManager;Ljava/lang/String;Z)V
        """

        if (executeFirst) {
            runJSBundle.addInstructions(0, loadScriptSmali)
        } else {
            // Find the index of the return-void instruction
            val instructions = runJSBundle.implementation?.instructions ?: return
            var returnIndex = -1
            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode == Opcode.RETURN_VOID) {
                    returnIndex = index
                }
            }

            if (returnIndex != -1) {
                runJSBundle.addInstructions(returnIndex, loadScriptSmali)
            }
        }
    }
}
