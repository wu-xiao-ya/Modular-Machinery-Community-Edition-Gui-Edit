package com.fushu.mmceguiext.common.container;

import appeng.container.slot.AppEngSlot;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SlotAEMixedCapacityCardTest {
    @Test
    public void capacityCardSlotIsAeContainerSlot() {
        assertTrue(AppEngSlot.class.isAssignableFrom(SlotAEMixedCapacityCard.class));
    }
}
