package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.requirement.LongAmountRequirement;
import com.fushu.mmceguiext.common.requirement.LongRequirementAmounts;
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
import java.util.Collections;
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
        if (this.mmceguiext$requiredAmountLong >= 0L) {
            return this.mmceguiext$requiredAmountLong;
        }
        return this.required == null ? 0L : (long) Math.max(0, this.required.amount);
    }

    @Override
    public void mmceguiext$setRequiredAmountLong(long amount) {
        this.mmceguiext$requiredAmountLong = Math.max(0L, amount);
    }

    @Inject(method = "deepCopyModified", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$copyLongAmount(List<RecipeModifier> modifiers, CallbackInfoReturnable<RequirementFluid> cir) {
        if (this.required == null) {
            cir.setReturnValue((RequirementFluid) (Object) this);
            return;
        }
        long modified = LongRequirementAmounts.applyModifiers(modifiers, (RequirementFluid) (Object) this, mmceguiext$getRequiredAmountLong());
        FluidStack copied = this.required.copy();
        copied.amount = LongRequirementAmounts.downcastAmount(modified);
        RequirementFluid copy = new RequirementFluid(this.actionType, copied);
        copy.chance = RecipeModifier.applyModifiers(modifiers, (RequirementFluid) (Object) this, this.chance, true);
        copy.setMatchNBTTag(((RequirementFluid) (Object) this).getTagMatch());
        copy.setDisplayNBTTag(((RequirementFluid) (Object) this).getTagDisplay());
        ((LongAmountRequirement) copy).mmceguiext$setRequiredAmountLong(modified);
        cir.setReturnValue(copy);
    }

    @Inject(method = "copyComponents", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$copyLongHandlers(List<ProcessingComponent<?>> components, CallbackInfoReturnable<List<ProcessingComponent<?>>> cir) {
        if (this.required == null) {
            cir.setReturnValue(Collections.<ProcessingComponent<?>>emptyList());
            return;
        }
        if (mmceguiext$shouldUseLongFluidPath(components)) {
            cir.setReturnValue(LongRequirementIO.copyFluidComponents(components));
        }
    }

    @Inject(method = "canStartCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;)Lhellfirepvp/modularmachinery/common/crafting/helper/CraftCheck;",
        at = @At("HEAD"), cancellable = true)
    private void mmceguiext$canStartLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, CallbackInfoReturnable<CraftCheck> cir) {
        if (this.required == null) {
            cir.setReturnValue(mmceguiext$missingFluidRequirement());
            return;
        }
        if (mmceguiext$shouldUseLongFluidPath(components)) {
            cir.setReturnValue(mmceguiext$doFluidIO(components, context));
        }
    }

    @Inject(method = "startCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;Lhellfirepvp/modularmachinery/common/util/ResultChance;)V",
        at = @At("HEAD"), cancellable = true)
    private void mmceguiext$startLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance, CallbackInfo ci) {
        if (this.required == null) {
            ci.cancel();
            return;
        }
        if (this.actionType == IOType.INPUT && mmceguiext$shouldUseLongFluidPath(components)) {
            if (chance.canWork(RecipeModifier.applyModifiers(context, (RequirementFluid) (Object) this, this.chance, true))) {
                mmceguiext$doFluidIO(components, context);
            }
            ci.cancel();
        }
    }

    @Inject(method = "finishCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;Lhellfirepvp/modularmachinery/common/util/ResultChance;)V",
        at = @At("HEAD"), cancellable = true)
    private void mmceguiext$finishLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance, CallbackInfo ci) {
        if (this.required == null) {
            ci.cancel();
            return;
        }
        if (this.actionType == IOType.OUTPUT && mmceguiext$shouldUseLongFluidPath(components)) {
            if (chance.canWork(RecipeModifier.applyModifiers(context, (RequirementFluid) (Object) this, this.chance, true))) {
                mmceguiext$doFluidIO(components, context);
            }
            ci.cancel();
        }
    }

    @Inject(method = "getMaxParallelism", at = @At("HEAD"), cancellable = true)
    private void mmceguiext$getMaxParallelismLong(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxParallelism, CallbackInfoReturnable<Integer> cir) {
        if (this.required == null) {
            cir.setReturnValue(0);
            return;
        }
        if (!mmceguiext$shouldUseLongFluidPath(components)) {
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
        if (this.required == null) {
            return 0;
        }
        List<IFluidHandler> fluidHandlers = HybridFluidUtils.castFluidHandlerComponents(components);
        long required = LongRequirementAmounts.applyModifiers(context, (RequirementFluid) (Object) this, mmceguiext$getRequiredAmountLong());
        if (required <= 0L) {
            return maxMultiplier;
        }
        long maxRequired = LongRequirementAmounts.saturatedMultiply(required, maxMultiplier);
        FluidStack stack = this.required.copy();
        long totalIO = LongRequirementIO.simulateFluid(stack, fluidHandlers, maxRequired, this.actionType);
        if (totalIO < required) {
            return 0;
        }
        LongRequirementIO.doFluid(stack, fluidHandlers, totalIO, this.actionType);
        return (int) Math.min((long) Integer.MAX_VALUE, totalIO / required);
    }

    @Unique
    private boolean mmceguiext$shouldUseLongFluidPath(List<ProcessingComponent<?>> components) {
        return this.required != null
            && mmceguiext$getRequiredAmountLong() > Integer.MAX_VALUE
            && LongRequirementIO.hasLongFluidHandler(components);
    }

    @Unique
    private CraftCheck mmceguiext$missingFluidRequirement() {
        if (this.actionType == IOType.INPUT) {
            return CraftCheck.failure("craftcheck.failure.fluid.input");
        }
        return this.ignoreOutputCheck ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.output.space");
    }
}
