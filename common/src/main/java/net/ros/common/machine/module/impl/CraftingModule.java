package net.ros.common.machine.module.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.ros.common.fluid.LimitedTank;
import net.ros.common.inventory.InventoryHandler;
import net.ros.common.machine.component.CraftingComponent;
import net.ros.common.machine.component.FluidComponent;
import net.ros.common.machine.component.SteamComponent;
import net.ros.common.machine.event.RecipeChangeEvent;
import net.ros.common.machine.module.*;
import net.ros.common.recipe.RecipeBase;
import net.ros.common.recipe.RecipeHandler;
import net.ros.common.recipe.ingredient.RecipeIngredient;
import net.ros.common.steam.ISteamHandler;
import net.ros.common.util.ItemUtils;
import net.voxelindustry.hermod.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class CraftingModule extends MachineModule implements ITickableModule, ISerializableModule
{
    private static final Function<IModularMachine, Float> DEFAULT_EFFICIENCY = machine ->
            machine.getModule(SteamModule.class).getInternalSteamHandler().getPressure() /
                    machine.getDescriptor().get(SteamComponent.class).getWorkingPressure();

    @Getter
    private final CraftingComponent  crafter;
    @Getter
    private final SteamComponent     steamMachine;
    private       ISteamHandler      steamHandler;
    private       InventoryHandler   inventory;
    private       FluidStorageModule fluidStorage;

    @Getter
    @Setter
    private float      currentProgress;
    @Getter
    @Setter
    private float      maxProgress;
    @Getter
    private RecipeBase currentRecipe;

    @Setter
    private EventHandler<RecipeChangeEvent>  onRecipeChange;
    @Setter
    private Function<IModularMachine, Float> efficiencySupplier;

    private List<FluidTank>  inputTanks;
    private List<FluidTank>  outputTanks;
    private List<FluidStack> bufferFluidStacks;

    public CraftingModule(IModularMachine machine)
    {
        super(machine, "CraftingModule");

        this.crafter = machine.getDescriptor().get(CraftingComponent.class);
        this.steamMachine = machine.getDescriptor().get(SteamComponent.class);

        this.efficiencySupplier = DEFAULT_EFFICIENCY;

        this.inputTanks = new ArrayList<>();
        this.outputTanks = new ArrayList<>();
        this.bufferFluidStacks = new ArrayList<>();

        if (machine.hasModule(FluidStorageModule.class))
        {
            FluidStorageModule fluidStorage = machine.getModule(FluidStorageModule.class);

            for (String name: this.crafter.getInputTanks())
            {
                this.inputTanks.add((FluidTank) fluidStorage.getFluidHandler(name));
                this.bufferFluidStacks.add(null);
            }

            for (String name: this.crafter.getOutputTanks())
                this.outputTanks.add((FluidTank) fluidStorage.getFluidHandler(name));
        }

        if (machine.hasModule(InventoryModule.class))
        {
            InventoryHandler inventory = new InventoryHandler(crafter.getInventorySize());

            for (int slot = 0; slot < inventory.getSlots(); slot++)
            {
                inventory.setSlotLimit(slot, 1);

                if (slot < crafter.getInputs())
                {
                    int finalSlot = slot;
                    inventory.addSlotFilter(slot, stack -> this.isBufferEmpty() && this.isOutputEmpty() &&
                            RecipeHandler.inputMatchWithoutCount(crafter.getRecipeCategory(), finalSlot, stack));
                }
            }
            machine.getModule(InventoryModule.class).addInventory("crafting", inventory);
        }
    }

    private ISteamHandler getSteamHandler()
    {
        if (steamHandler == null)
            steamHandler = this.getMachine().getModule(SteamModule.class).getInternalSteamHandler();
        return steamHandler;
    }

    private InventoryHandler getInventory()
    {
        if (inventory == null)
            inventory = this.getMachine().getModule(InventoryModule.class).getInventory("crafting");
        return inventory;
    }

    private FluidStorageModule getFluidStorage()
    {
        if (fluidStorage == null)
            fluidStorage = this.getMachine().getModule(FluidStorageModule.class);
        return fluidStorage;
    }

    private boolean hasInventory()
    {
        return this.getMachine().hasModule(InventoryModule.class) &&
                this.getMachine().getModule(InventoryModule.class).hasInventory("crafting");
    }

    private boolean hasFluidStorage()
    {
        return this.getMachine().hasModule(FluidStorageModule.class);
    }

    @Override
    public void tick()
    {
        if (this.isClient())
            return;

        if (this.currentRecipe == null && (!this.isInputEmpty() || !this.isBufferEmpty()))
        {
            if (this.getSteamHandler().getSteam() >= this.steamMachine.getSteamConsumption())
            {
                if (this.isBufferEmpty())
                {
                    Object[] ingredients = new Object[this.crafter.getInputs()
                            + this.crafter.getInputTanks().length];

                    for (int i = 0; i < this.crafter.getInputs(); i++)
                        ingredients[i] = this.getInventory().getStackInSlot(i);
                    for (int i = 0; i < this.crafter.getInputTanks().length; i++)
                        ingredients[this.crafter.getInputs() + i] = this.getInputFluidStack(i);

                    Optional<RecipeBase> recipe = RecipeHandler.getRecipe(this.crafter.getRecipeCategory(),
                            ingredients);

                    if (recipe.isPresent() && this.setCurrentRecipe(recipe.get()))
                    {
                        this.setMaxProgress((int) (this.currentRecipe.getTime() / this.getCraftingSpeed()));
                        int i = 0;
                        for (final RecipeIngredient<ItemStack> stack: this.currentRecipe
                                .getRecipeInputs(ItemStack.class))
                        {
                            this.getInventory().extractItem(i, stack.getRaw().getCount(), false);
                            this.getInventory().setStackInSlot(crafter.getInputs() + i, stack.getRaw().copy());
                            i++;
                        }
                        i = 0;
                        for (final RecipeIngredient<FluidStack> stack: this.currentRecipe
                                .getRecipeInputs(FluidStack.class))
                        {
                            this.inputTanks.get(i).drainInternal(stack.getQuantity(), true);
                            this.bufferFluidStacks.set(i, stack.getRaw().copy());
                            i++;
                        }
                        this.sync();
                    }
                }
                else
                {
                    final Object[] ingredients = new Object[this.crafter.getInputs()
                            + this.crafter.getInputTanks().length];

                    for (int i = 0; i < this.crafter.getInputs(); i++)
                        ingredients[i] = this.getInventory().getStackInSlot(this.crafter.getInputs() + i);
                    for (int i = 0; i < this.crafter.getInputTanks().length; i++)
                        ingredients[this.crafter.getInputs() + i] = this.bufferFluidStacks.get(i);

                    final Optional<RecipeBase> recipe = RecipeHandler.getRecipe(this.crafter.getRecipeCategory(),
                            ingredients);
                    if (recipe.isPresent())
                    {
                        this.setCurrentRecipe(recipe.get());
                        this.setMaxProgress((int) (this.currentRecipe.getTime() / this.getCraftingSpeed()));

                        this.sync();
                    }
                }
            }
        }
        if (this.currentRecipe != null && !this.isBufferEmpty())
        {
            if (this.getCurrentProgress() < this.getMaxProgress())
            {
                if (this.getSteamHandler().getSteam() >= this.steamMachine.getSteamConsumption())
                {
                    this.setCurrentProgress(this.getCurrentProgress() + this.getCurrentCraftingSpeed());
                    this.getSteamHandler().drainSteam(this.steamMachine.getSteamConsumption(), true);
                    this.sync();
                }
            }
            else
            {
                int i = 0;
                for (final RecipeIngredient<ItemStack> stack: this.currentRecipe.getRecipeOutputs(ItemStack.class))
                {
                    if (!ItemUtils.canMergeStacks(stack.getRaw(),
                            this.getInventory().getStackInSlot(this.crafter.getInputs() * 2 + i)))
                        return;
                    i++;
                }
                i = 0;
                for (final RecipeIngredient<FluidStack> stack: this.currentRecipe.getRecipeOutputs(FluidStack.class))
                {
                    if (this.outputTanks.get(i).fill(stack.getRaw(), false) == 0)
                        return;
                    i++;
                }

                for (int buffer = crafter.getInputs(); buffer < crafter.getInputs() * 2; buffer++)
                    this.getInventory().setStackInSlot(buffer, ItemStack.EMPTY);
                for (int j = 0; j < this.crafter.getInputTanks().length; j++)
                    this.bufferFluidStacks.set(j, null);

                i = 0;
                for (final RecipeIngredient<ItemStack> stack: this.currentRecipe.getRecipeOutputs(ItemStack.class))
                {
                    if (!this.getInventory().getStackInSlot(this.crafter.getInputs() * 2 + i).isEmpty())
                        this.getInventory().getStackInSlot(this.crafter.getInputs() * 2 + i).grow(
                                stack.getRaw().getCount());
                    else
                        this.getInventory().setStackInSlot(this.crafter.getInputs() * 2 + i, stack.getRaw().copy());
                    i++;
                }
                i = 0;
                for (final RecipeIngredient<FluidStack> stack: this.currentRecipe.getRecipeOutputs(FluidStack.class))
                {
                    this.outputTanks.get(i).fillInternal(stack.getRaw(), true);
                    i++;
                }
                this.setCurrentRecipe(null);
                this.setCurrentProgress(0);
                this.sync();
            }
        }
    }

    @Override
    public void fromNBT(NBTTagCompound tag)
    {
        this.setCurrentProgress(tag.getFloat("currentProgress"));
        this.setMaxProgress(tag.getFloat("maxProgress"));

        if (tag.getInteger("bufferFluidStack") != 0)
        {
            for (int i = 0; i < tag.getInteger("bufferFluidStack"); i++)
            {
                if (!tag.hasKey("bufferFluidStack"))
                {
                    this.bufferFluidStacks.set(i, null);
                    continue;
                }
                this.bufferFluidStacks.set(i,
                        FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("bufferFluidStack" + i)));
            }
        }
    }

    @Override
    public NBTTagCompound toNBT(NBTTagCompound tag)
    {
        tag.setFloat("currentProgress", this.currentProgress);
        tag.setFloat("maxProgress", this.maxProgress);

        tag.setInteger("bufferFluidStack", this.bufferFluidStacks.size());

        if (!this.bufferFluidStacks.isEmpty())
        {
            int i = -1;
            for (FluidStack stack: this.bufferFluidStacks)
            {
                i++;
                if (stack == null)
                    continue;
                tag.setTag("bufferFluidStack" + i, stack.writeToNBT(new NBTTagCompound()));
            }
        }
        return tag;
    }

    private boolean setCurrentRecipe(RecipeBase recipe)
    {
        if (this.onRecipeChange != null)
        {
            RecipeChangeEvent event = new RecipeChangeEvent(this.getMachine(), this.currentRecipe, recipe);
            this.onRecipeChange.handle(event);
            if (recipe != null && event.isCancelled())
                return false;
        }
        this.currentRecipe = recipe;
        return true;
    }

    public float getCurrentCraftingSpeed()
    {
        return this.getCraftingSpeed() * this.getEfficiency();
    }

    public float getCraftingSpeed()
    {
        return this.crafter.getCraftingSpeed();
    }

    public float getEfficiency()
    {
        return this.efficiencySupplier.apply(this.getMachine());
    }

    public int getProgressScaled(int scale)
    {
        if (this.currentProgress != 0 && this.maxProgress != 0)
            return (int) (this.currentProgress * scale / this.maxProgress);
        return 0;
    }

    ////////////////////
    // FLUID HANDLING //
    ////////////////////

    public void linkInputTank(String name)
    {
        FluidTank craftTank;

        FluidComponent component = this.getMachine().getDescriptor().get(FluidComponent.class);
        if (component.getTankThrottle(name) != Integer.MAX_VALUE)
            craftTank = new LimitedTank(component.getTankCapacity(name), component.getTankThrottle(name));
        else
            craftTank = new FluidTank(component.getTankCapacity(name));

        this.getMachine().getModule(FluidStorageModule.class).setFluidHandler(name, craftTank);

        this.inputTanks.add(craftTank);
        this.bufferFluidStacks.add(null);
    }

    public void linkOutputTank(String name)
    {
        FluidComponent component = this.getMachine().getDescriptor().get(FluidComponent.class);
        FluidTank craftTank = new FluidTank(component.getTankCapacity(name));

        this.getMachine().getModule(FluidStorageModule.class).setFluidHandler(name, craftTank);

        this.outputTanks.add(craftTank);
    }

    private FluidStack getInputFluidStack(int index)
    {
        return this.inputTanks.get(index).getFluid();
    }


    ///////////////
    // INVENTORY //
    ///////////////

    public boolean isBufferEmpty()
    {
        if (this.hasInventory())
            for (int slot = 0; slot < this.crafter.getInputs(); slot++)
            {
                if (!this.getInventory().getStackInSlot(this.crafter.getInputs() + slot).isEmpty())
                    return false;
            }
        if (this.hasFluidStorage())
            return this.bufferFluidStacks.isEmpty() || this.bufferFluidStacks.stream().allMatch(Objects::isNull);
        return true;
    }

    public boolean isInputEmpty()
    {
        if (this.hasInventory())
            for (int i = 0; i < this.crafter.getInputs(); i++)
            {
                if (!this.getInventory().getStackInSlot(i).isEmpty())
                    return false;
            }
        if (this.hasFluidStorage())
            for (int i = 0; i < this.crafter.getInputTanks().length; i++)
            {
                if (((IFluidTank) this.getFluidStorage().getFluidHandler(this.crafter.getInputTanks()[i])).getFluidAmount() != 0)
                    return false;
            }
        return true;
    }

    public boolean isOutputEmpty()
    {
        if (this.hasInventory())
            for (int i = 0; i < this.crafter.getOutputs(); i++)
            {
                if (!this.getInventory().getStackInSlot(this.crafter.getInputs() * 2 + i).isEmpty())
                    return false;
            }
        if (this.hasFluidStorage())
            for (int i = 0; i < this.crafter.getOutputTanks().length; i++)
            {
                if (((IFluidTank) this.getFluidStorage().getFluidHandler(this.crafter.getInputTanks()[i])).getFluidAmount() != 0)
                    return false;
            }
        return true;
    }
}
