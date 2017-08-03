package net.qbar.client.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.qbar.QBar;
import net.qbar.client.gui.util.GuiMachineBase;
import net.qbar.client.gui.util.GuiProgress;
import net.qbar.client.gui.util.GuiTexturedSpace;
import net.qbar.common.tile.machine.TileSolidBoiler;

import java.util.Arrays;

public class GuiBoiler extends GuiMachineBase<TileSolidBoiler>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(QBar.MODID, "textures/gui/boiler.png");

    public GuiBoiler(final EntityPlayer player, final TileSolidBoiler boiler)
    {
        super(player, boiler, BACKGROUND);

        this.addFluidTank(boiler.getWaterTank(), 128, 7, 18, 73);
        this.addSteamTank(boiler.getSteamTank(), 151, 7, 18, 73);

        this.addAnimatedSprite(this::getBurnLeftScaled,
                GuiProgress.builder().space(GuiTexturedSpace.builder().x(81).y(38).width(14).height(13).u(176).v(12)
                        .s(190).t(25).build()).direction(GuiProgress.StartDirection.TOP).revert(false).build());

        this.addAnimatedSprite(this::getHeatScaled,
                GuiProgress.builder().space(GuiTexturedSpace.builder().x(10).y(79).width(12).height(78).u(176).v(84)
                        .s(176 + 12).t(85 + 79).build()).direction(GuiProgress.StartDirection.TOP).revert(false)
                        .build());
    }

    @Override
    public void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY)
    {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        GlStateManager.translate(-this.guiLeft, -this.guiTop, 0.0F);
        final int x = (this.width - this.xSize) / 2;
        final int y = (this.height - this.ySize) / 2;

        if (mouseX > x + 10 && mouseX < x + 22 && mouseY > y + 8 && mouseY < y + 79)
        {
            GuiUtils.drawHoveringText(
                    Arrays.asList(TextFormatting.GOLD + "" + this.getMachine().getHeat() / 10 + " / "
                            + this.getMachine().getMaxHeat() / 10 + " °C"),
                    mouseX, mouseY, this.width, this.height, -1, this.mc.fontRenderer);
        }
        GlStateManager.translate(this.guiLeft, this.guiTop, 0.0F);
    }

    private int getHeatScaled(final int pixels)
    {
        final int i = this.getMachine().getMaxHeat();

        if (i == 0)
            return 0;

        return (int) Math.min(this.getMachine().getHeat() * pixels / i, pixels);
    }

    private int getBurnLeftScaled(final int pixels)
    {
        final int i = this.getMachine().getMaxBurnTime();

        if (i == 0)
            return 0;

        return this.getMachine().getCurrentBurnTime() * pixels / i;
    }
}
