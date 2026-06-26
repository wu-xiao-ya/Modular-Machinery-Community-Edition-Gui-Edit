package com.fushu.mmceguiext.common.requirement;

import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.junit.Test;

import java.lang.reflect.Field;

import static hellfirepvp.modularmachinery.common.machine.IOType.INPUT;
import static hellfirepvp.modularmachinery.common.machine.IOType.OUTPUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LongRequirementIOTest {
    @Test
    public void simulateFluidOutputAcceptsIntoEmptyCustomHatch() throws Exception {
        TileCustomHatch hatch = new TileCustomHatch();
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        IFluidHandler tank = internalTank(hatch);

        long simulated = LongRequirementIO.simulateFluid(water, java.util.Collections.singletonList(tank), 1000L, OUTPUT);

        assertEquals(1000L, simulated);
    }

    @Test
    public void doFluidOutputFillsCustomHatchAndTracksLongAmount() throws Exception {
        TileCustomHatch hatch = new TileCustomHatch();
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        IFluidHandler tank = internalTank(hatch);

        LongRequirementIO.doFluid(water, java.util.Collections.singletonList(tank), 1000L, OUTPUT);

        assertEquals(1000L, hatch.getFluidAmountLong());
        assertNotNull(hatch.getFluidStack());
        assertEquals("water", hatch.getFluidStack().getFluid().getName());
    }

    @Test
    public void simulateFluidInputSeesExistingCustomHatchContents() throws Exception {
        TileCustomHatch hatch = new TileCustomHatch();
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        IFluidHandler tank = internalTank(hatch);
        LongRequirementIO.doFluid(water, java.util.Collections.singletonList(tank), 1000L, OUTPUT);

        long simulated = LongRequirementIO.simulateFluid(water, java.util.Collections.singletonList(tank), 750L, INPUT);

        assertEquals(750L, simulated);
    }

    private static IFluidHandler internalTank(TileCustomHatch hatch) throws Exception {
        Field field = TileCustomHatch.class.getDeclaredField("tank");
        field.setAccessible(true);
        return (IFluidHandler) field.get(hatch);
    }
}
