package app.revanced.patches.pinterest

import com.android.tools.smali.dexlib2.AccessFlags
import app.revanced.patcher.fingerprint

internal val adsGmaExperimentFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Z")
    strings("android_ad_gma_new", "enabled")
}
