package net.ros.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.ros.RiseOfSteam;
import net.ros.client.render.BlueprintRender;
import net.ros.client.render.model.obj.ROSOBJLoader;
import net.ros.client.render.tile.*;
import net.ros.common.CommonProxy;
import net.ros.common.ROSConstants;
import net.ros.common.block.IModelProvider;
import net.ros.common.init.ROSBlocks;
import net.ros.common.init.ROSFluids;
import net.ros.common.init.ROSItems;
import net.ros.common.item.IItemModelProvider;
import net.ros.common.network.MultiblockBoxPacket;
import net.ros.common.recipe.Materials;
import net.ros.common.tile.TileStructure;
import net.ros.common.tile.machine.TileBelt;
import net.ros.common.tile.machine.TileRollingMill;
import net.ros.common.tile.machine.TileSawMill;
import net.ros.common.tile.machine.TileSteamFurnaceMK2;
import org.lwjgl.input.Mouse;
import org.yggard.brokkgui.style.StylesheetManager;

import java.util.function.BiConsumer;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    private PipeModelRegistry pipeModelRegistry;

    @Override
    public void preInit(final FMLPreInitializationEvent e)
    {
        OBJLoader.INSTANCE.addDomain(ROSConstants.MODID);
        ModelLoaderRegistry.registerLoader(ROSOBJLoader.INSTANCE);
        ROSOBJLoader.INSTANCE.addDomain(ROSConstants.MODID);

        MinecraftForge.EVENT_BUS.register(this);
        super.preInit(e);

        ROSOBJLoader.INSTANCE.addRetexturedModel("_belt_animated.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/belt.mwm"), new String[]{"Top"},
                new String[]{"ros:blocks/belt_top_anim"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_belt_slope_down.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/belt_slope_up.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/belt_slope_down"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_belt_slope_up_animated.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/belt_slope_up.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/belt_slope_up_anim"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_belt_slope_down_animated.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/belt_slope_up.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/belt_slope_down_anim"});

        ROSOBJLoader.INSTANCE.addRetexturedModel("_fluidvalve_small.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/steamvalve_small.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/fluidvalve_small"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_fluidvalve_medium.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/steamvalve_medium.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/fluidvalve_medium"});

        ROSOBJLoader.INSTANCE.addRetexturedModel("_fluidpipe_cast_iron_small.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/fluidpipe_iron_small.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/fluidpipe_cast_iron_small"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_fluidpipe_cast_iron_medium.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/fluidpipe_iron_medium.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/fluidpipe_cast_iron_medium"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_fluidpipe_cast_iron_large.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/fluidpipe_iron_large.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/fluidpipe_cast_iron_large"});

        ROSOBJLoader.INSTANCE.addRetexturedModel("_steampipe_steel_small.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/steampipe_brass_small.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/steampipe_steel_small"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_steampipe_steel_medium.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/steampipe_brass_medium.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/steampipe_steel_medium"});
        ROSOBJLoader.INSTANCE.addRetexturedModel("_steampipe_steel_large.mwm",
                new ResourceLocation(ROSConstants.MODID + ":block/steampipe_brass_large.mwm"), new String[]{"None"},
                new String[]{"ros:blocks/steampipe_steel_large"});

        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());

        ROSItems.ITEMS.stream().filter(item -> item instanceof IItemModelProvider)
                .forEach(item -> ((IItemModelProvider) item).registerVariants());
    }

    @Override
    public void init(final FMLInitializationEvent e)
    {
        super.init(e);

        ClientRegistry.bindTileEntitySpecialRenderer(TileBelt.class, new RenderBelt());
        ClientRegistry.bindTileEntitySpecialRenderer(TileStructure.class, new RenderStructure());
        ClientRegistry.bindTileEntitySpecialRenderer(TileRollingMill.class, new RenderRollingMill());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSteamFurnaceMK2.class, new RenderSteamFurnaceMK2());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSawMill.class, new RenderSawMill());
        MinecraftForge.EVENT_BUS.register(new MachineClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new LogisticClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent e)
    {
        super.postInit(e);

        StylesheetManager.getInstance().addUserAgent(ROSConstants.MODID, "/assets/ros/css/theme.css");
    }

    @Override
    public void registerItemRenderer(final Item item, final int meta)
    {
        ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    private static void registerFluidsClient()
    {
        ROSFluids.FLUIDS.forEach((fluid, blockFluid) ->
        {
            ModelResourceLocation fluidLocation = new ModelResourceLocation(blockFluid.getRegistryName(),
                    fluid.getName());
            ModelLoader.setCustomStateMapper(blockFluid, new StateMapperBase()
            {
                @Override
                protected ModelResourceLocation getModelResourceLocation(final IBlockState state)
                {
                    return fluidLocation;
                }
            });
        });
    }

    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent e)
    {
        ClientProxy.registerFluidsClient();

        for (Item item: ROSItems.ITEMS)
        {
            if (item instanceof IItemModelProvider && ((IItemModelProvider) item).hasSpecialModel())
                ((IItemModelProvider) item).registerModels();
            else
                RiseOfSteam.proxy.registerItemRenderer(item, 0);
        }

        pipeModelRegistry = new PipeModelRegistry();
        pipeModelRegistry.onModelRegistry();
    }

    @SubscribeEvent
    public void onModelBake(final ModelBakeEvent e)
    {
        ModelResourceLocation key = new ModelResourceLocation(ROSItems.BLUEPRINT.getRegistryName(),
                "inventory");
        IBakedModel originalModel = e.getModelRegistry().getObject(key);
        e.getModelRegistry().putObject(key, new BlueprintRender(originalModel));

        pipeModelRegistry.onModelBake(e.getModelRegistry());

        pipeModelRegistry.replacePipeInventoryModel(e.getModelRegistry(),
                Materials.IRON, Materials.CAST_IRON, Materials.BRASS, Materials.STEEL);

        ModelLoader.setCustomModelResourceLocation(Item.getByNameOrId("ros:itemextractor"), 1,
                new ModelResourceLocation(ROSConstants.MODID + ":itemextractor", "facing=down,filter=true"));
        ModelLoader.setCustomModelResourceLocation(Item.getByNameOrId("ros:itemsplitter"), 1,
                new ModelResourceLocation(ROSConstants.MODID + ":itemsplitter", "facing=up,filter=true"));

        ModelLoader.setCustomModelResourceLocation(ROSItems.VALVE, 0, new ModelResourceLocation(
                ROSConstants.MODID + ":itemvalve", "inventory"));
        ModelLoader.setCustomModelResourceLocation(ROSItems.GAUGE, 0, new ModelResourceLocation(
                ROSConstants.MODID + ":itemgauge", "inventory"));

        ROSBlocks.BLOCKS.keySet().stream().filter(IModelProvider.class::isInstance).forEach(block ->
        {
            IModelProvider modelProvider = (IModelProvider) block;

            BiConsumer<Integer, Block> modelRegister = modelProvider.registerItemModels();
            for (int i = 0; i < modelProvider.getItemModelCount(); i++)
                modelRegister.accept(i, block);
        });
    }

    @SubscribeEvent
    public void onRightClick(GuiScreenEvent.MouseInputEvent event)
    {
        if (event.getGui() instanceof GuiContainer && Mouse.isButtonDown(1))
        {
            GuiContainer container = (GuiContainer) event.getGui();
            if (container.getSlotUnderMouse() != null
                    && Minecraft.getMinecraft().player.inventory.getItemStack().isEmpty()
                    && container.getSlotUnderMouse().getStack().getItem() == ROSItems.MULTIBLOCK_BOX)
            {
                new MultiblockBoxPacket(container.getSlotUnderMouse().slotNumber).sendToServer();
                event.setCanceled(true);
            }
        }
    }
}
