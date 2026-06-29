package app.revanced.patches.creaastuce

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val offlineValidationPatch = bytecodePatch(
    name = "Offline Validation Fix",
    description = "Forces offline ticket validations to always succeed and displays them in the ongoing tickets list even when offline."
) {
    compatibleWith("fr.cityway.android.creaastuce"("5.8.74.1938-release-prod"))

    execute {
        // 1. Bypass offline validation constraint checks (solvency, ticket count, existing offline validations)
        val uw2Class = classes.find { it.type == "Luw2;" }
            ?: throw Exception("uw2 class not found")
        val mutableUw2 = proxy(uw2Class).mutableClass

        val hasAlreadyOfflineValidation = mutableUw2.methods.find { it.name == "hasAlreadyOfflineValidation" }
            ?: throw Exception("hasAlreadyOfflineValidation method not found")
        hasAlreadyOfflineValidation.returnEarly(false)

        val isTickerNumberValid = mutableUw2.methods.find { it.name == "isTickerNumberValid" }
            ?: throw Exception("isTickerNumberValid method not found")
        isTickerNumberValid.returnEarly(true)

        val isUserSolvent = mutableUw2.methods.find { it.name == "isUserSolvent" }
            ?: throw Exception("isUserSolvent method not found")
        isUserSolvent.returnEarly(true)

        // 2. Fix the offline ongoing tickets UI bug.
        //    WA0.f() loads OngoingServiceDataBaseObject into arrayList6, which is never
        //    populated offline. When cond_42 is reached, arrayList6.isEmpty() → NETWORK_ISSUE.
        //
        //    Two insertion points:
        //    a) BEFORE the first isEmpty check (line 4627): inject N4.a() into arrayList6
        //       so the check returns false → validation-aware block runs and attaches
        //       the active validation product to services (needed for mTicket tab to show it).
        //    b) AT cond_42 (line 5638): remove isEmpty + inject N4.a() as fallback for when
        //       validation == DEFAULT and we jump directly to cond_42.
        val wa0Class = classes.find { it.type == "LWA0;" }
            ?: throw Exception("WA0 class not found")
        val mutableWa0 = proxy(wa0Class).mutableClass
        val fMethod = mutableWa0.methods.find { it.name == "f" }
            ?: throw Exception("f method not found")

        // 2a. Inject N4.a() into arrayList6 BEFORE the FIRST isEmpty check so the
        //     validation-aware block runs with populated services.
        val firstEmptyIndex = fMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_VIRTUAL &&
            (this as? ReferenceInstruction)?.reference?.let { ref ->
                (ref as? MethodReference)?.let { methodRef ->
                    methodRef.name == "isEmpty" && methodRef.definingClass == "Ljava/util/ArrayList;"
                }
            } == true
        }
        fMethod.addInstructions(
            firstEmptyIndex,
            """
                invoke-virtual/range {v64 .. v64}, LN4;->a()Ljava/util/ArrayList;
                move-result-object v5
                invoke-virtual {v6, v5}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z
            """
        )

        // 2b. At cond_42 (SECOND isEmpty, found via reverse search), remove isEmpty check
        //     and inject N4.a() as fallback for the validation==DEFAULT path.
        val cond42EmptyIndex = fMethod.indexOfFirstInstructionReversedOrThrow {
            opcode == Opcode.INVOKE_VIRTUAL &&
            (this as? ReferenceInstruction)?.reference?.let { ref ->
                (ref as? MethodReference)?.let { methodRef ->
                    methodRef.name == "isEmpty" && methodRef.definingClass == "Ljava/util/ArrayList;"
                }
            } == true
        }
        fMethod.removeInstruction(cond42EmptyIndex)
        fMethod.removeInstruction(cond42EmptyIndex)
        fMethod.removeInstruction(cond42EmptyIndex)
        fMethod.addInstructions(
            cond42EmptyIndex,
            """
                invoke-virtual/range {v64 .. v64}, LN4;->a()Ljava/util/ArrayList;
                move-result-object v5
                invoke-virtual {v6, v5}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z
            """
        )

        // 3. Fix validation expiry in Lw2.g() (ValidateTicketUseCase).
        //
        //    g() computes endDate via:
        //      v3 = calculateEndDate(now, durationSecs)
        //      if (offerEndAcceptationDate != null) v3 = min(offerEndAcceptationDate, v3)
        //    Then for PASS offers: if (offerEndAcceptationDate.after(now) == false) → EXPIRED.
        //
        //    If offerEndAcceptationDate is stale/in-past, both checks fail:
        //    min() shrinks endDate to past, and after() returns false → "Expired".
        //
        //    Fix: insert const/4 v10, 0x0 before if-eqz v10, :cond_c.
        //    This nulls v10 (offerEndAcceptationDate), so:
        //    a) if-eqz v10 → JUMP to :cond_c → skip getMinimum → v3 stays as calculatedEndDate
        //    b) if-eqz v10 (later at :cond_d) → JUMP to :cond_e → skip PASS expiry check entirely
        //    One instruction kills both bugs. No label references needed.
        //
        //    Also extend the calculated end date from ~5min to 24h via const v6, 0x15180.
        val lw2Class = classes.find { it.type == "LLw2;" }
            ?: throw Exception("Lw2 class not found")
        val mutableLw2 = proxy(lw2Class).mutableClass
        val gMethod = mutableLw2.methods.find { it.name == "g" }
            ?: throw Exception("g method not found")

        // 3b. Null offerEndAcceptationDate to skip min() and PASS expiry check.
        //     Find DateTimeManager.getMinimum(), then insert const/4 v10, 0x0 before
        //     the if-eqz v10 guard (getMinIndex - 1).
        val getMinIndex = gMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_INTERFACE &&
            (this as? ReferenceInstruction)?.reference?.let { ref ->
                (ref as? MethodReference)?.let { methodRef ->
                    methodRef.name == "getMinimum" &&
                    methodRef.definingClass == "Lfr/cityway/product/domain/infrastructure/date/DateTimeManager;"
                }
            } == true
        }
        val ifEqzMinIndex = getMinIndex - 1
        gMethod.addInstructions(ifEqzMinIndex, "const/4 v10, 0x0")

        // 3a. Extend offline validation end date from 5 minutes to 24 hours (86400 seconds).
        //     Insert const v6, 0x15180 before calculateEndDate to override duration.
        val calcEndDateIndex = gMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_INTERFACE &&
            (this as? ReferenceInstruction)?.reference?.let { ref ->
                (ref as? MethodReference)?.let { methodRef ->
                    methodRef.name == "calculateEndDate" &&
                    methodRef.definingClass == "Lfr/cityway/product/domain/infrastructure/date/DateTimeManager;"
                }
            } == true
        }
        gMethod.addInstructions(
            calcEndDateIndex,
            "const v6, 0x15180"
        )
    }
}
