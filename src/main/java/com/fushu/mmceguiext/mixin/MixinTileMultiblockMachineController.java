package com.fushu.mmceguiext.mixin;

import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = TileMultiblockMachineController.class)
public abstract class MixinTileMultiblockMachineController {

    @Shadow(remap = false)
    protected abstract void checkAndAddSmartInterface(MachineComponent<?> component, BlockPos realPos);

    @Shadow(remap = false)
    public abstract void checkAndAddUpgradeBus(MachineComponent<?> component);

    @Shadow(remap = false)
    protected TaggedPositionBlockArray foundPattern;

    @Inject(method = "checkAndAddComponents", at = @At("TAIL"), remap = false)
    private void mmceguiext$mergeCustomMixedInputComponents(final BlockPos pos,
                                                            final BlockPos ctrlPos,
                                                            final Map<Long, Map<TileEntity, ProcessingComponent<?>>> found,
                                                            final CallbackInfo ci) {
        TileMultiblockMachineController self = (TileMultiblockMachineController) (Object) this;
        BlockPos realPos = ctrlPos.add(pos);
        TileEntity te = self.getWorld().getTileEntity(realPos);
        if (!(te instanceof TileCustomAEMixedInputBus)) {
            return;
        }

        TileCustomAEMixedInputBus inputBus = (TileCustomAEMixedInputBus) te;
        Collection<MachineComponent<?>> rawComponents = inputBus.provideComponents();
        if (rawComponents.isEmpty()) {
            return;
        }

        List<MachineComponent<?>> components = new ObjectArrayList<MachineComponent<?>>(rawComponents);
        if (components.isEmpty()) {
            return;
        }

        ComponentSelectorTag tag = mmceguiext$readTag(pos);
        long mergedGroupId = components.iterator().next().getGroupID();
        if (mergedGroupId < 0L) {
            return;
        }

        Map<TileEntity, ProcessingComponent<?>> merged = found.get(mergedGroupId);
        if (merged == null) {
            merged = new LinkedHashMap<TileEntity, ProcessingComponent<?>>();
        }

        mmceguiext$removeOriginalEntries(found, te, components);
        found.put(mergedGroupId, merged);

        int index = 0;
        for (MachineComponent<?> component : components) {
            TileEntity key = new VirtualComponentTile(te, index++);
            merged.put(key, new ProcessingComponent(component, component.getContainerProvider(), tag));
            checkAndAddUpgradeBus(component);
            checkAndAddSmartInterface(component, realPos);
        }
    }

    @Unique
    private void mmceguiext$removeOriginalEntries(final Map<Long, Map<TileEntity, ProcessingComponent<?>>> found,
                                                  final TileEntity originalTile,
                                                  final Collection<MachineComponent<?>> components) {
        for (MachineComponent<?> component : components) {
            long groupId = component.getGroupID();
            Map<TileEntity, ProcessingComponent<?>> grouped = found.get(groupId);
            if (grouped == null) {
                continue;
            }
            grouped.remove(originalTile);
            if (grouped.isEmpty()) {
                found.remove(groupId);
            }
        }
    }

    @Unique
    private ComponentSelectorTag mmceguiext$readTag(final BlockPos pos) {
        return foundPattern == null ? null : foundPattern.getTag(pos);
    }

    @Unique
    private static final class VirtualComponentTile extends TileEntity {
        private final TileEntity delegate;
        private final int index;

        private VirtualComponentTile(final TileEntity delegate, final int index) {
            this.delegate = delegate;
            this.index = index;
            this.setWorld(delegate.getWorld());
            this.setPos(delegate.getPos());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof VirtualComponentTile)) {
                return false;
            }
            VirtualComponentTile other = (VirtualComponentTile) obj;
            return this.index == other.index && this.delegate == other.delegate;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.delegate) * 31 + this.index;
        }
    }
}
