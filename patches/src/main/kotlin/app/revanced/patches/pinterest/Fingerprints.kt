package app.revanced.patches.pinterest

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val adsGmaExperimentFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Z")
    strings("android_ad_gma_new", "enabled")
}

internal val isPromotedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/Boolean")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.RETURN_OBJECT
    )
    custom { method, _ ->
        val impl = method.implementation ?: return@custom false
        impl.instructions.count() == 4 &&
        impl.instructions.any { insn ->
            (insn as? ReferenceInstruction)?.reference is FieldReference &&
            ((insn as ReferenceInstruction).reference as FieldReference).name == "w1"
        }
    }
}

internal val isDownstreamPromotionFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/Boolean")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.RETURN_OBJECT
    )
    custom { method, _ ->
        val impl = method.implementation ?: return@custom false
        impl.instructions.count() == 4 &&
        impl.instructions.any { insn ->
            (insn as? ReferenceInstruction)?.reference is FieldReference &&
            ((insn as ReferenceInstruction).reference as FieldReference).name == "Q0"
        }
    }
}

internal val shopTheLookFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Lcom/pinterest/api/model/me;", "Z")
    returns("V")
    strings("closeupPin", "product_tagged_shopping_module_upsell")
}
