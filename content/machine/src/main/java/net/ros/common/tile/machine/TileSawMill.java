package net.ros.common.tile.machine;

import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.ros.common.ROSConstants;
import net.ros.common.init.ROSItems;
import net.ros.common.inventory.InventoryHandler;
import net.ros.common.machine.Machines;
import net.ros.common.machine.module.InventoryModule;
import net.ros.common.machine.module.impl.IOModule;
import net.ros.common.machine.module.impl.SteamModule;
import net.ros.common.recipe.RecipeHandler;
import net.ros.common.steam.SteamUtil;
import net.ros.common.container.BuiltContainer;
import net.ros.common.container.ContainerBuilder;
import net.ros.common.container.IContainerProvider;
import net.ros.common.gui.MachineGui;
import net.ros.common.machine.event.RecipeChangeEvent;
import net.ros.common.machine.module.impl.AutomationModule;
import net.ros.common.machine.module.impl.CraftingModule;

public class TileSawMill extends TileTickingModularMachine implements IContainerProvider
{
    @Getter
    private ItemStack cachedStack;

    public TileSawMill()
    {
        super(Machines.SAW_MILL);

        this.cachedStack = ItemStack.EMPTY;
    }

    @Override
    protected void reloadModules()
    {
        super.reloadModules();

        this.addModule(new InventoryModule(this));
        this.addModule(new SteamModule(this, SteamUtil::createTank));

        CraftingModule crafter = new CraftingModule(this);
        crafter.setOnRecipeChange(this::onRecipeChange);

        this.addModule(crafter);
        this.addModule(new AutomationModule(this));
        this.addModule(new IOModule(this));
    }

    private void onRecipeChange(RecipeChangeEvent e)
    {
        if (this.getModule(CraftingModule.class).getCurrentRecipe() != null)
            this.cachedStack = this.getModule(CraftingModule.class)
                    .getCurrentRecipe().getRecipeOutputs(ItemStack.class).get(0).getRaw();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        super.writeToNBT(tag);

        tag.setTag("cachedStack", this.cachedStack.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.cachedStack = new ItemStack(tag.getCompoundTag("cachedStack"));
    }

    @Override
    public BuiltContainer createContainer(final EntityPlayer player)
    {
        InventoryHandler inventory = this.getModule(InventoryModule.class).getInventory("crafting");
        CraftingModule crafter = this.getModule(CraftingModule.class);
        SteamModule steamEngine = this.getModule(SteamModule.class);

        return new ContainerBuilder("sawmill", player).player(player).inventory(8, 84).hotbar(8, 142)
                .addInventory().tile(inventory)
                .recipeSlot(0, RecipeHandler.SAW_MILL_UID, 0, 47, 36,
                        slot -> crafter.isBufferEmpty() && crafter.isOutputEmpty())
                .outputSlot(2, 116, 35).displaySlot(1, -1000, 0)
                .syncFloatValue(crafter::getCurrentProgress, crafter::setCurrentProgress)
                .syncFloatValue(crafter::getMaxProgress, crafter::setMaxProgress)
                .syncIntegerValue(steamEngine.getInternalSteamHandler()::getSteam,
                        steamEngine.getInternalSteamHandler()::setSteam).addInventory().create();
    }

    @Override
    public boolean onRightClick(final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY,
                                final float hitZ, BlockPos from)
    {
        if (player.isSneaking())
            return false;
        if (player.getHeldItemMainhand().getItem() == ROSItems.WRENCH)
            return false;

        player.openGui(ROSConstants.MODINSTANCE, MachineGui.SAWMILL.getUniqueID(), this.world, this.pos.getX(), this
                        .pos.getY(),
                this.pos.getZ());
        return true;
    }

    @Override
    public boolean hasFastRenderer()
    {
        return true;
    }
}
