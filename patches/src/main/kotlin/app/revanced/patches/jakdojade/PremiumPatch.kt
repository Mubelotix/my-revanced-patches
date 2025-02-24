package app.revanced.patches.jakdojade

import app.revanced.patcher.patch.bytecodePatch;
import app.revanced.util.returnEarly;
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions;

@Suppress("unused")
val premiumPatch = bytecodePatch(
    name = "Activates premium features",
) {
    compatibleWith("com.citynav.jakdojade.pl.android"("6.9.5"));

    execute {
        bannerAdManagerFingerprint.method.returnEarly();
    }
}
