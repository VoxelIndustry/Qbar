package net.ros.common.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.ros.common.ROSConstants;
import net.ros.common.tile.TileBase;

public abstract class BlockMachineBase<T extends TileBase> extends BlockContainer
{
    private Class<T> tileClass;

    public BlockMachineBase(final String name, final Material material, Class<T> tileClass)
    {
        super(material);

        this.setRegistryName(ROSConstants.MODID, name);
        this.setUnlocalizedName(name);
        this.setCreativeTab(ROSConstants.TAB_ALL);
        this.tileClass = tileClass;
    }

    @Override
    public void breakBlock(final World worldIn, final BlockPos pos, final IBlockState state)
    {
        final TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof IInventory)
        {
            InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory) tileentity);
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public EnumBlockRenderType getRenderType(final IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    public T getWorldTile(IBlockAccess world, BlockPos pos)
    {
        return (T) this.getRawWorldTile(world, pos);
    }

    public TileEntity getRawWorldTile(IBlockAccess world, BlockPos pos)
    {
        if (world instanceof ChunkCache)
            return ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        else
            return world.getTileEntity(pos);
    }

    public boolean checkWorldTile(IBlockAccess world, BlockPos pos)
    {
        if (world instanceof ChunkCache)
            return tileClass.isInstance(((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK));
        else
            return tileClass.isInstance(world.getTileEntity(pos));
    }

    public Class<T> getTileClass()
    {
        return tileClass;
    }
}
