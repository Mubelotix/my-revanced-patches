package app.revanced.patches.pinterest

import com.android.tools.smali.dexlib2.AccessFlags
import app.revanced.patcher.fingerprint

internal val adsGmaExperimentFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Z")
    strings("android_ad_gma_new", "enabled")
}

internal val shopTheLookFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Lcom/pinterest/api/model/me;", "Z")
    returns("V")
    strings("closeupPin", "product_tagged_shopping_module_upsell")
}
