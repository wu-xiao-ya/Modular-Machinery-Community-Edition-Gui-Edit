package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.requirement.LongAmountRequirement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.nbt.NBTJsonDeserializer;
import net.minecraft.nbt.NBTException;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RequirementTypeFluid.class, remap = false)
public abstract class MixinRequirementTypeFluid {
    /**
     * @author MMCEGE
     * @reason Parse fluid amount as long for custom long-capacity hatches.
     */
    @Overwrite
    public RequirementFluid createRequirement(IOType type, JsonObject requirement) {
        RequirementFluid req;

        if (!requirement.has("fluid") || !requirement.get("fluid").isJsonPrimitive()
            || !requirement.get("fluid").getAsJsonPrimitive().isString()) {
            throw new JsonParseException("The ComponentType 'fluid' expects an 'fluid'-entry that defines the type of fluid!");
        }
        if (!requirement.has("amount") || !requirement.get("amount").isJsonPrimitive()
            || !requirement.get("amount").getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("The ComponentType 'fluid' expects an 'amount'-entry that defines the type of fluid!");
        }
        String fluidName = requirement.getAsJsonPrimitive("fluid").getAsString();
        long mbAmount = Math.max(0L, requirement.getAsJsonPrimitive("amount").getAsLong());
        Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid == null) {
            throw new JsonParseException("The fluid specified in the 'fluid'-entry (" + fluidName + ") doesn't exist!");
        }
        FluidStack fluidStack = new FluidStack(fluid, downcastAmount(mbAmount));
        req = new RequirementFluid(type, fluidStack);
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
        if (requirement.has("nbt")) {
            if (!requirement.get("nbt").isJsonObject()) {
                throw new JsonParseException("The ComponentType 'nbt' expects a json compound that defines the NBT tag!");
            }
            String nbtString = requirement.getAsJsonObject("nbt").toString();
            try {
                req.setMatchNBTTag(NBTJsonDeserializer.deserialize(nbtString));
            } catch (NBTException exc) {
                throw new JsonParseException("Error trying to parse NBTTag! Rethrowing exception...", exc);
            }
            if (requirement.has("nbt-display")) {
                if (!requirement.get("nbt-display").isJsonObject()) {
                    throw new JsonParseException("The ComponentType 'nbt-display' expects a json compound that defines the NBT tag meant to be used for displaying!");
                }
                String nbtDisplayString = requirement.getAsJsonObject("nbt-display").toString();
                try {
                    req.setDisplayNBTTag(NBTJsonDeserializer.deserialize(nbtDisplayString));
                } catch (NBTException exc) {
                    throw new JsonParseException("Error trying to parse NBTTag! Rethrowing exception...", exc);
                }
            } else {
                req.setDisplayNBTTag(req.getTagMatch());
            }
        }
        return req;
    }

    private static int downcastAmount(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }
}
