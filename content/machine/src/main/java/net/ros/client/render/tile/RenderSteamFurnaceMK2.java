package net.ros.client.render.tile;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.ros.common.inventory.InventoryHandler;
import net.ros.common.machine.module.InventoryModule;
import net.ros.common.machine.module.impl.CraftingModule;
import net.ros.common.tile.machine.TileSteamFurnaceMK2;
import net.ros.client.render.RenderUtil;

public class RenderSteamFurnaceMK2 extends TileEntitySpecialRenderer<TileSteamFurnaceMK2>
{
    @Override
    public void render(TileSteamFurnaceMK2 tile, double x, double y, double z, float partialTicks, int destroyStage,
                       float alpha)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + .42, y + 1.2, z + .8);

        switch (tile.getFacing())
        {
            case NORTH:
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.translate(1.6, 0, -0.84375);
                break;
            case SOUTH:
                GlStateManager.rotate(-90, 0, 1, 0);
                GlStateManager.translate(1, 0, -1);
                break;
            case WEST:
                GlStateManager.rotate(180, 0, 1, 0);
                GlStateManager.translate(1.22, 0, -0.625);
                break;
            default:
                GlStateManager.translate(1.38, 0, -1.22);
                break;
        }

        CraftingModule crafter = tile.getModule(CraftingModule.class);
        InventoryHandler inventory = tile.getModule(InventoryModule.class).getInventory("crafting");

        if (!inventory.getStackInSlot(0).isEmpty())
            RenderUtil.handleRenderItem(inventory.getStackInSlot(0), true);
        if (!inventory.getStackInSlot(2).isEmpty())
        {
            GlStateManager.translate(-3 * (crafter.getCurrentProgress() / crafter.getMaxProgress()), 0, 0);

            if (crafter.getCurrentProgress() / crafter.getMaxProgress() > 0.5)
                RenderUtil.handleRenderItem(tile.getCachedStack(), true);
            else
                RenderUtil.handleRenderItem(inventory.getStackInSlot(2), true);
        }
        GlStateManager.popMatrix();
    }
}
