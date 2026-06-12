package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.requirement.LongAmountRequirement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeGas;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fml.common.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RequirementTypeGas.class, remap = false)
public abstract class MixinRequirementTypeGas {
    /**
     * @author MMCEGE
     * @reason Parse gas amount as long for custom long-capacity hatches.
     */
    @Overwrite
    @Optional.Method(modid = "mekanism")
    public RequirementGas createRequirement(IOType machineIoType, JsonObject requirement) {
        if (!requirement.has("gas") || !requirement.get("gas").isJsonPrimitive()
            || !requirement.get("gas").getAsJsonPrimitive().isString()) {
            throw new JsonParseException("The ComponentType 'gas' expects an 'gas'-entry that defines the type of gas!");
        }
        if (!requirement.has("amount") || !requirement.get("amount").isJsonPrimitive()
            || !requirement.get("amount").getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("The ComponentType 'gas' expects an 'amount'-entry that defines the type of gas!");
        }
        String gasName = requirement.getAsJsonPrimitive("gas").getAsString();
        long mbAmount = Math.max(0L, requirement.getAsJsonPrimitive("amount").getAsLong());
        Gas gas = GasRegistry.getGas(gasName);
        if (gas == null) {
            throw new JsonParseException("The gas specified in the 'gas'-entry (" + gasName + ") doesn't exist!");
        }
        GasStack gasStack = new GasStack(gas, downcastAmount(mbAmount));
        RequirementGas req = new RequirementGas(machineIoType, gasStack);
        ((LongAmountRequirement) req).mmceguiext$setRequiredAmountLong(mbAmount);

        if (requirement.has("chance")) {
            if (!requirement.get("chance").isJsonPrimitive() || !requirement.getAsJsonPrimitive("chance").isNumber()) {
                throw new JsonParseException("'chance', if defined, needs to be a chance-number between 0 and 1!");
            }
            float chance = requirement.getAsJsonPrimitive("chance").getAsFloat();
            if (chance >= 0 && chance <= 1) {
                req.setChance(chance);
            }
        }
        return req;
    }

    private static int downcastAmount(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }
}
