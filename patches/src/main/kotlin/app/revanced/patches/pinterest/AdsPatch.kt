package app.revanced.patches.pinterest

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.util.returnEarly

val adsPatch = bytecodePatch(
    name = "Remove ads",
) {
    compatibleWith("com.pinterest"("14.23.0"));

    execute {
        // Block GMA (Google Mobile Ads) experiment gate
        adsGmaExperimentFingerprint.method.returnEarly(false);

        // Block promoted pin detection by making is_promoted always false
        val meClass = classes.find { it.type == "Lcom/pinterest/api/model/me;" }
            ?: throw Exception("me class not found")
        val mutableMe = proxy(meClass).mutableClass

        mutableMe.methods.find { it.name == "I5" }!!.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """
        )

        mutableMe.methods.find { it.name == "q5" }!!.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """
        )

        // Block "Shop the Pin" module by making is_shop_the_look always false
        val ueClass = classes.find { it.type == "Lcom/pinterest/api/model/ue;" }
            ?: throw Exception("ue class not found")
        proxy(ueClass).mutableClass.methods.find { it.name == "J0" }!!.returnEarly(false);
    }
}
