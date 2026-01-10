package app.revanced.util

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.extensions.InstructionExtensions
import com.android.tools.smali.dexlib2.Opcode
import java.io.File

object ReactNativePatch {
    fun installRawFile(context: ResourcePatchContext, jsCode: String) {
        // Use ResourcePatchContext to write the file to assets
        // 'true' typically means create/overwrite
        val file = context.get("assets/revanced-plugin.js", true)
        file.parentFile?.mkdirs()
        file.writeText(jsCode)
    }

    fun patchBytecodeLoader(context: BytecodePatchContext, executeFirst: Boolean) {
        val catalystInstanceImpl = context.classes.find { it.type == "Lcom/facebook/react/bridge/CatalystInstanceImpl;" } ?: return
        val runJSBundle = catalystInstanceImpl.methods.find { it.name == "runJSBundle" } ?: return
        
        val mutableClass = context.proxy(catalystInstanceImpl).mutableClass
        // Find the mutable method corresponding to runJSBundle
        val mutableMethod = mutableClass.methods.find { 
            it.name == runJSBundle.name && 
            it.returnType == runJSBundle.returnType && 
            it.parameterTypes == runJSBundle.parameterTypes 
        } ?: return

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
            with(InstructionExtensions) {
                mutableMethod.addInstructions(0, loadScriptSmali)
            }
        } else {
            // Find the index of the return-void instruction
            // implementation is from MutableMethod (MethodImplementation)
            val implementation = mutableMethod.implementation
            if (implementation != null) {
                val instructions = implementation.instructions
                var returnIndex = -1
                for ((index, instruction) in instructions.withIndex()) {
                    if (instruction.opcode == Opcode.RETURN_VOID) {
                        returnIndex = index
                    }
                }

                if (returnIndex != -1) {
                    with(InstructionExtensions) {
                        mutableMethod.addInstructions(returnIndex, loadScriptSmali)
                    }
                }
            }
        }
    }
}

