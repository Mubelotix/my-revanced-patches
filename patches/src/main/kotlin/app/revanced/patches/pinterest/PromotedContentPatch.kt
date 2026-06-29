package app.revanced.patches.pinterest

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.util.returnEarly

val promotedContentPatch = bytecodePatch(
    name = "Remove promoted content",
) {
    compatibleWith("com.pinterest"("14.23.0"));

    execute {
        isPromotedFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """
        );

        isDownstreamPromotionFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """
        );

        shopTheLookFingerprint.method.returnEarly();
    }
}
