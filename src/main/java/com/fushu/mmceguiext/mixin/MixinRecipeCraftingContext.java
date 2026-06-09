package com.fushu.mmceguiext.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluidPerTick;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = RecipeCraftingContext.class)
public abstract class MixinRecipeCraftingContext {

    @Shadow(remap = false)
    private Map<Long, Collection<ProcessingComponent<?>>> typeComponents;

    @Inject(method = "getComponentsFor", at = @At("RETURN"), cancellable = true, remap = false)
    private void mmceguiext$restoreCustomMixedInputItemCandidates(final ComponentRequirement<?, ?> requirement,
                                                                  @Nullable final ComponentSelectorTag tag,
                                                                  final CallbackInfoReturnable<Map<Long, List<ProcessingComponent<?>>>> cir) {
        if (!(requirement instanceof RequirementItem) || requirement.getActionType() != IOType.INPUT) {
            return;
        }

        Map<Long, List<ProcessingComponent<?>>> current = cir.getReturnValue();
        if (current != null && !mmceguiext$isEmptyResult(current)) {
            return;
        }

        Map<Long, List<ProcessingComponent<?>>> restored = new LinkedHashMap<>();
        for (Map.Entry<Long, Collection<ProcessingComponent<?>>> entry : this.typeComponents.entrySet()) {
            for (ProcessingComponent<?> component : entry.getValue()) {
                if (component == null || component.component() == null) {
                    continue;
                }
                if (component.component().getIOType() != IOType.INPUT) {
                    continue;
                }
                if (!component.component().getComponentType().equals(ComponentTypesMM.COMPONENT_ITEM)) {
                    continue;
                }
                Object provided = component.getProvidedComponent();
                if (provided == null || !provided.getClass().getName().contains("TileCustomAEMixedInputBus$LoggingItemHandler")) {
                    continue;
                }
                if (tag != null && !tag.equals(component.getTag())) {
                    continue;
                }
                restored.computeIfAbsent(entry.getKey(), ignored -> new ObjectArrayList<>()).add(component);
            }
        }

        if (!restored.isEmpty()) {
            cir.setReturnValue(restored);
        }
    }

    @Inject(method = "getComponentsFor", at = @At("RETURN"), cancellable = true, remap = false)
    private void mmceguiext$restoreCustomMixedInputGasCandidates(final ComponentRequirement<?, ?> requirement,
                                                                 @Nullable final ComponentSelectorTag tag,
                                                                 final CallbackInfoReturnable<Map<Long, List<ProcessingComponent<?>>>> cir) {
        boolean gasRequirement = requirement instanceof RequirementGas || requirement instanceof RequirementGasPerTick;
        if (!gasRequirement || requirement.getActionType() != IOType.INPUT) {
            return;
        }

        Map<Long, List<ProcessingComponent<?>>> current = cir.getReturnValue();
        if (current != null && !mmceguiext$isEmptyResult(current)) {
            return;
        }

        Map<Long, List<ProcessingComponent<?>>> restored = new LinkedHashMap<>();
        for (Map.Entry<Long, Collection<ProcessingComponent<?>>> entry : this.typeComponents.entrySet()) {
            for (ProcessingComponent<?> component : entry.getValue()) {
                if (component == null || component.component() == null) {
                    continue;
                }
                if (component.component().getIOType() != IOType.INPUT) {
                    continue;
                }
                Object provider = component.getProvidedComponent();
                if (provider == null || !provider.getClass().getName().contains("TileCustomAEMixedInputBus$LoggingGasHandler")) {
                    continue;
                }
                if (tag != null && !tag.equals(component.getTag())) {
                    continue;
                }
                restored.computeIfAbsent(entry.getKey(), ignored -> new ObjectArrayList<>()).add(component);
            }
        }

        if (!restored.isEmpty()) {
            cir.setReturnValue(restored);
        }
    }

    @Inject(method = "getComponentsFor", at = @At("RETURN"), cancellable = true, remap = false)
    private void mmceguiext$restoreCustomMixedInputFluidCandidates(final ComponentRequirement<?, ?> requirement,
                                                                   @Nullable final ComponentSelectorTag tag,
                                                                   final CallbackInfoReturnable<Map<Long, List<ProcessingComponent<?>>>> cir) {
        boolean fluidRequirement = requirement instanceof RequirementFluid || requirement instanceof RequirementFluidPerTick;
        if (!fluidRequirement || requirement.getActionType() != IOType.INPUT) {
            return;
        }

        Map<Long, List<ProcessingComponent<?>>> current = cir.getReturnValue();
        if (current != null && !mmceguiext$isEmptyResult(current)) {
            return;
        }

        Map<Long, List<ProcessingComponent<?>>> restored = new LinkedHashMap<>();
        for (Map.Entry<Long, Collection<ProcessingComponent<?>>> entry : this.typeComponents.entrySet()) {
            for (ProcessingComponent<?> component : entry.getValue()) {
                if (component == null || component.component() == null) {
                    continue;
                }
                if (component.component().getIOType() != IOType.INPUT) {
                    continue;
                }
                Object provider = component.getProvidedComponent();
                if (provider == null || !provider.getClass().getName().contains("TileCustomAEMixedInputBus$LoggingFluidHandler")) {
                    continue;
                }
                if (tag != null && !tag.equals(component.getTag())) {
                    continue;
                }
                restored.computeIfAbsent(entry.getKey(), ignored -> new ObjectArrayList<>()).add(component);
            }
        }

        if (!restored.isEmpty()) {
            cir.setReturnValue(restored);
        }
    }

    private boolean mmceguiext$isEmptyResult(final Map<Long, List<ProcessingComponent<?>>> map) {
        if (map.isEmpty()) {
            return true;
        }
        for (List<ProcessingComponent<?>> components : map.values()) {
            if (components != null && !components.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
