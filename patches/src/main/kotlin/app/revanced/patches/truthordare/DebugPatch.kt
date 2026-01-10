package app.revanced.patches.truthordare

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reactnative.reactNativePatch

@Suppress("unused")
val debugPatch = bytecodePatch(
    name = "Debug React Native",
) {
    // compatibleWith("com.antoinehabert.truthordategame"("2.9"));

    // This patch depends on the generic React Native patch
    // which handles the injection.
    dependsOn(reactNativePatch)
}
