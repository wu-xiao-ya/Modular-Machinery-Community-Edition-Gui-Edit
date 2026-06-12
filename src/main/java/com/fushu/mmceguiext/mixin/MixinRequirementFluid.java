package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.requirement.LongAmountRequirement;
import com.fushu.mmceguiext.common.requirement.LongRequirementIO;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementTypeFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.List;

@Mixin(value = RequirementFluid.class, remap = false)
public abstract class MixinRequirementFluid extends ComponentRequirement.MultiCompParallelizable<Object, RequirementTypeFluid> implements LongAmountRequirement {
    @Shadow
    public final FluidStack required = null;

    @Shadow
    public float chance;

    @Unique
    private long mmceguiext$requiredAmountLong = -1L;

    private MixinRequirementFluid(RequirementTypeFluid requirementType, IOType actionType) {
        super(requirementType, actionType);
    }

    @Override
    public long mmceguiext$getRequiredAmountLong() {
        return this.mmceguiext$requiredAmountLong >= 0L ? this.mmceguiext$requiredAmountLong : (long) this.required.amount;
    }

    @Override
    public void mmceguiext$setRequiredAmountLong(long amount) {
        this.mmceguiext$requiredAmountLong = Math.max(0L, amount);
    }

    @Inject(method = "deepCopyModified", at = @At("RETURN"), cancellable = true)
    private void mmceguiext$copyLongAmount(List<RecipeModifier> modifiers, CallbackInfoReturnable<RequirementFluid> cir) {
        RequirementFluid copy = cir.getReturnValue();
        if (copy instanceof LongAmountRequirement) {
            long modified = Math.round(RecipeModifier.applyModifiers(modifiers, (RequirementFluid) (Object) this, (double) mmceguiext$getRequiredAmountLong(), false));
            ((LongAmountRequirement) copy).mmceguiext$setRequiredAmountLong(modified);
        }
    }

    @Inject(method = "copyComponents", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$copyLongHandlers(List<ProcessingComponent<?>> components, CallbackInfoReturnable<List<ProcessingComponent<?>>> cir) {
        if (LongRequirementIO.hasLongFluidHandler(components)) {
            cir.setReturnValue(LongRequirementIO.copyFluidComponents(components));
        }
    }

    @Inject(method = "canStartCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;)Lhellfirepvp/modularmachinery/common/crafting/helper/CraftCheck;",
        at = @At("HEAD"), cancellable = true)
    private void mmceguiext$canStartLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, CallbackInfoReturnable<CraftCheck> cir) {
        if (LongRequirementIO.hasLongFluidHandler(components)) {
            cir.setReturnValue(mmceguiext$doFluidIO(components, context));
        }
    }

    @Inject(method = "startCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;Lhellfirepvp/modularmachinery/common/util/ResultChance;)V",
        at = @At("HEAD"), cancellable = true)
    private void mmceguiext$startLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance, CallbackInfo ci) {
        if (this.actionType == IOType.INPUT && LongRequirementIO.hasLongFluidHandler(components)) {
            if (chance.canWork(RecipeModifier.applyModifiers(context, (RequirementFluid) (Object) this, this.chance, true))) {
                mmceguiext$doFluidIO(components, context);
            }
            ci.cancel();
        }
    }

    @Inject(method = "finishCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;Lhellfirepvp/modularmachinery/common/util/ResultChance;)V",
        at = @At("HEAD"), cancellable = true)
    private void mmceguiext$finishLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance, CallbackInfo ci) {
        if (this.actionType == IOType.OUTPUT && LongRequirementIO.hasLongFluidHandler(components)) {
            if (chance.canWork(RecipeModifier.applyModifiers(context, (RequirementFluid) (Object) this, this.chance, true))) {
                mmceguiext$doFluidIO(components, context);
            }
            ci.cancel();
        }
    }

    @Inject(method = "getMaxParallelism", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$getMaxParallelismLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxParallelism, CallbackInfoReturnable<Integer> cir) {
        if (!LongRequirementIO.hasLongFluidHandler(components)) {
            return;
        }
        if (this.ignoreOutputCheck && this.actionType == IOType.OUTPUT) {
            cir.setReturnValue(maxParallelism);
            return;
        }
        if (this.parallelizeUnaffected) {
            cir.setReturnValue(mmceguiext$doFluidIOInternal(components, context, 1) >= 1 ? maxParallelism : 0);
            return;
        }
        cir.setReturnValue(mmceguiext$doFluidIOInternal(components, context, maxParallelism));
    }

    @Nonnull
    @Unique
    private CraftCheck mmceguiext$doFluidIO(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        int mul = mmceguiext$doFluidIOInternal(components, context, this.parallelism);
        if (mul < this.parallelism) {
            if (this.actionType == IOType.INPUT) {
                return CraftCheck.failure("craftcheck.failure.fluid.input");
            }
            return this.ignoreOutputCheck ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.output.space");
        }
        return CraftCheck.success();
    }

    @Unique
    private int mmceguiext$doFluidIOInternal(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxMultiplier) {
        List<IFluidHandler> fluidHandlers = HybridFluidUtils.castFluidHandlerComponents(components);
        long required = Math.round(RecipeModifier.applyModifiers(context, (RequirementFluid) (Object) this, (double) mmceguiext$getRequiredAmountLong(), false));
        if (required <= 0L) {
            return maxMultiplier;
        }
        long maxRequired = saturatedMultiply(required, maxMultiplier);
        FluidStack stack = this.required.copy();
        long totalIO = LongRequirementIO.simulateFluid(stack, fluidHandlers, maxRequired, this.actionType);
        if (totalIO < required) {
            return 0;
        }
        LongRequirementIO.doFluid(stack, fluidHandlers, totalIO, this.actionType);
        return (int) Math.min((long) Integer.MAX_VALUE, totalIO / required);
    }

    @Unique
    private static long saturatedMultiply(long left, int right) {
        if (left <= 0L || right <= 0) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * (long) right;
    }
}
