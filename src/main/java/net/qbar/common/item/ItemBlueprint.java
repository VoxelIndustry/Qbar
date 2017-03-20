package net.qbar.common.item;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.qbar.common.init.QBarBlocks;
import net.qbar.common.multiblock.BlockMultiblockBase;
import net.qbar.common.multiblock.BlockStructure;
import net.qbar.common.multiblock.IMultiblockDescriptor;
import net.qbar.common.multiblock.TileMultiblockGag;
import net.qbar.common.multiblock.blueprint.Blueprint;
import net.qbar.common.multiblock.blueprint.Blueprints;
import net.qbar.common.tile.TileStructure;
import net.qbar.common.util.ItemUtils;

public class ItemBlueprint extends ItemBase
{
    public ItemBlueprint()
    {
        super("blueprint");
        this.setHasSubtypes(true);
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World world, BlockPos pos, final EnumHand hand,
            final EnumFacing facing, final float hitX, final float hitY, final float hitZ)
    {
        final IBlockState iblockstate = world.getBlockState(pos);
        final Block block = iblockstate.getBlock();

        if (!block.isReplaceable(world, pos))
            pos = pos.offset(facing);

        final ItemStack stack = player.getHeldItem(hand);

        if (!stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("blueprint"))
        {
            final String name = stack.getTagCompound().getString("blueprint");
            final Blueprint blueprint = Blueprints.getInstance().getBlueprints()
                    .get(stack.getTagCompound().getString("blueprint"));
            final BlockMultiblockBase base = (BlockMultiblockBase) Block.getBlockFromName("qbar:" + name);

            if ((player.capabilities.isCreativeMode
                    || ItemUtils.hasPlayerEnough(player.inventory, blueprint.getRodStack(), false))
                    && player.canPlayerEdit(pos, facing, stack)
                    && world.mayPlace(base, pos, false, facing, (Entity) null))
            {
                final int i = this.getMetadata(stack.getMetadata());
                final IBlockState state = base.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, i, player,
                        hand);

                if (this.placeBlockAt(stack, player, world, pos, state, base.getDescriptor()))
                {
                    final SoundType soundtype = world.getBlockState(pos).getBlock()
                            .getSoundType(world.getBlockState(pos), world, pos, player);
                    world.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
                            (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                    stack.shrink(1);
                    if (!player.capabilities.isCreativeMode)
                        ItemUtils.drainPlayer(player.inventory, blueprint.getRodStack());
                }
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.FAIL;
    }

    public boolean placeBlockAt(final ItemStack stack, final EntityPlayer player, final World world, final BlockPos pos,
            final IBlockState newState, final IMultiblockDescriptor descriptor)
    {
        BlockPos corePos = pos;
        if (descriptor.getOffsetX() != 0 || descriptor.getOffsetY() != 0 || descriptor.getOffsetZ() != 0)
        {
            if (BlockMultiblockBase.getFacing(newState).getAxis().equals(Axis.Z))
                corePos = pos.add(descriptor.getOffsetX(), descriptor.getOffsetY(), descriptor.getOffsetZ());
            else
                corePos = pos.add(descriptor.getOffsetZ(), descriptor.getOffsetY(), descriptor.getOffsetX());
        }
        if (!world.setBlockState(corePos, QBarBlocks.STRUCTURE.getDefaultState(), 11))
            return false;

        final IBlockState state = world.getBlockState(corePos);
        ItemBlock.setTileEntityNBT(world, player, corePos, stack);
        state.getBlock().onBlockPlacedBy(world, corePos, state, player, stack);
        final TileStructure structure = (TileStructure) world.getTileEntity(corePos);
        if (structure != null)
        {
            structure
                    .setBlueprint(Blueprints.getInstance().getBlueprint(stack.getTagCompound().getString("blueprint")));
            structure.setMeta(newState.getBlock().getMetaFromState(newState));
        }

        Iterable<BlockPos> searchables = null;
        if (BlockMultiblockBase.getFacing(newState).getAxis().equals(Axis.Z))
        {
            searchables = BlockPos.getAllInBox(
                    corePos.subtract(
                            new Vec3i(descriptor.getOffsetX(), descriptor.getOffsetY(), descriptor.getOffsetZ())),
                    corePos.add(descriptor.getWidth() - 1 - descriptor.getOffsetX(),
                            descriptor.getHeight() - 1 - descriptor.getOffsetY(),
                            descriptor.getLength() - 1 - descriptor.getOffsetZ()));
        }
        else
        {
            searchables = BlockPos.getAllInBox(
                    corePos.subtract(
                            new Vec3i(descriptor.getOffsetZ(), descriptor.getOffsetY(), descriptor.getOffsetX())),
                    corePos.add(descriptor.getLength() - 1 - descriptor.getOffsetZ(),
                            descriptor.getHeight() - 1 - descriptor.getOffsetY(),
                            descriptor.getWidth() - 1 - descriptor.getOffsetX()));
        }
        for (final BlockPos current : searchables)
        {
            if (!current.equals(corePos))
            {
                if (!world.setBlockState(current,
                        QBarBlocks.STRUCTURE.getDefaultState().withProperty(BlockStructure.MULTIBLOCK_GAG, true)))
                    return false;
                final TileMultiblockGag gag = (TileMultiblockGag) world.getTileEntity(current);
                if (gag != null)
                    gag.setCorePos(corePos);
            }
        }
        return true;
    }

    @Override
    public void getSubItems(final Item item, final CreativeTabs tab, final NonNullList<ItemStack> list)
    {
        Blueprints.getInstance().getBlueprints().forEach((name, blueprint) ->
        {
            final ItemStack stack = new ItemStack(this);
            final NBTTagCompound tag = new NBTTagCompound();
            stack.setTagCompound(tag);

            tag.setString("blueprint", name);
            list.add(stack);
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getItemStackDisplayName(final ItemStack stack)
    {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("blueprint"))
            return I18n
                    .translateToLocalFormatted("item.blueprint.name", new Object[] {
                            I18n.translateToLocal("tile." + stack.getTagCompound().getString("blueprint") + ".name") })
                    .trim();
        return this.name;
    }
}
