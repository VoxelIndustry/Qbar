package net.ros.common.machine.module.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.ros.common.grid.node.IBelt;
import net.ros.common.inventory.InventoryHandler;
import net.ros.common.machine.OutputPoint;
import net.ros.common.machine.component.AutomationComponent;
import net.ros.common.machine.module.IModularMachine;
import net.ros.common.machine.module.ITickableModule;
import net.ros.common.machine.module.InventoryModule;
import net.ros.common.machine.module.MachineModule;
import net.ros.common.multiblock.MultiblockComponent;
import net.ros.common.multiblock.MultiblockSide;

import java.util.HashMap;
import java.util.Map;

public class AutomationModule extends MachineModule implements ITickableModule
{
    private final HashMap<OutputPoint, Integer> lastOutput;

    private final Map<OutputPoint, InventoryHandler> outputWrappers;

    public AutomationModule(IModularMachine machine)
    {
        super(machine, "AutomationModule");

        this.lastOutput = new HashMap<>();
        this.outputWrappers = new HashMap<>();

        for (OutputPoint point : getMachine().getDescriptor().get(AutomationComponent.class).getOutputs())
        {
            String inventoryName = "undefined".equals(point.getInventory()) ?
                    (machine.hasModule(CraftingModule.class) ? "crafting" : "basic") : point.getInventory();

            this.outputWrappers.put(point, machine.getModule(InventoryModule.class).getInventory(inventoryName));
        }
    }

    @Override
    public void tick()
    {
        for (OutputPoint point : this.getMachine().getDescriptor().get(AutomationComponent.class).getOutputs())
        {
            if (!anySlotFull(point))
                continue;

            MultiblockSide side = this.getMachine().getDescriptor().get(MultiblockComponent.class)
                    .multiblockSideToWorldSide(point.getSide(), this.getMachine().getFacing());

            BlockPos computedPos = this.getMachineTile().getPos().add(side.getPos());
            if (this.hasBelt(this.getMachineTile().getWorld(), computedPos))
            {
                InventoryHandler inventory = this.outputWrappers.get(point);
                int slot;

                if (point.getSlots().length > 1)
                {
                    if (point.isRoundRobin())
                        slot = this.getNextSlotSeq(point);
                    else
                        slot = this.getFirstFullSlot(point);
                }
                else
                    slot = point.getSlots()[0];

                if (slot == -1)
                    continue;

                ItemStack toTransfer = inventory.getStackInSlot(slot);
                IBelt belt = (IBelt) this.getMachineTile().getWorld().getTileEntity(computedPos);

                if (this.canInsert(belt, toTransfer, side.getFacing()))
                    this.insert(belt, inventory.extractItem(slot, 1, false), side.getFacing());
            }
        }
        this.getMachineTile().sync();
    }

    private void insert(IBelt belt, ItemStack stack, EnumFacing facing)
    {
        if (belt.getFacing() == facing.rotateY())
            belt.insert(stack, 0, 10 / 32F, true);
        if (belt.getFacing() == facing.rotateYCCW())
            belt.insert(stack, 10 / 16F, 10 / 32F, true);
        else
            belt.insert(stack, true);
    }

    private boolean canInsert(IBelt belt, ItemStack stack, EnumFacing facing)
    {
        if (belt.getFacing() == facing)
            return false;
        if (belt.getFacing() == facing.rotateY())
            return belt.insert(stack, 0, 10 / 32F, false);
        if (belt.getFacing() == facing.rotateYCCW())
            return belt.insert(stack, 10 / 16F, 10 / 32F, false);
        else
            return belt.insert(stack, false);
    }

    private boolean hasBelt(World w, BlockPos pos)
    {
        return w.getTileEntity(pos) instanceof IBelt;
    }

    private boolean anySlotFull(OutputPoint point)
    {
        for (int slot : point.getSlots())
        {
            if (!outputWrappers.get(point).getStackInSlot(slot).isEmpty())
                return true;
        }
        return false;
    }

    private int getNextSlotSeq(OutputPoint point)
    {
        if (!this.lastOutput.containsKey(point))
            this.lastOutput.put(point, point.getSlots().length);

        int start = this.lastOutput.get(point);
        if (start == point.getSlots().length)
            start = -1;
        start++;

        while (start < point.getSlots().length)
        {
            InventoryHandler inventory = outputWrappers.get(point);
            if (!inventory.getStackInSlot(point.getSlots()[start]).isEmpty())
            {
                this.lastOutput.put(point, start);
                return point.getSlots()[start];
            }
            start++;

            if (start == point.getSlots().length)
                start = 0;
        }
        return 0;
    }

    private int getFirstFullSlot(OutputPoint point)
    {
        for (int slot : point.getSlots())
        {
            if (!outputWrappers.get(point).getStackInSlot(slot).isEmpty())
                return slot;
        }
        return -1;
    }
}
