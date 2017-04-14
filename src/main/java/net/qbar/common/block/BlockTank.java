package net.qbar.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.Properties;
import net.qbar.common.multiblock.BlockMultiblockBase;
import net.qbar.common.multiblock.IMultiblockDescriptor;
import net.qbar.common.tile.machine.TileTank;

public class BlockTank extends BlockMultiblockBase
{
    private final int capacity;
    private final int tier;

    public BlockTank(final String name, final IMultiblockDescriptor descriptor, final int capacity, final int tier)
    {
        super(name, Material.IRON, descriptor);

        this.capacity = capacity;
        this.tier = tier;
    }

    @Override
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getExtendedState(final IBlockState state, final IBlockAccess world, final BlockPos pos)
    {
        if (world.getTileEntity(pos) != null && world.getTileEntity(pos) instanceof TileTank)
        {
            final TileTank tile = (TileTank) world.getTileEntity(pos);
            if (tile.getTier() != 0)
                return ((IExtendedBlockState) state).withProperty(Properties.AnimationProperty, tile.state);
        }
        return state;
    }

    @Override
    public TileEntity getTile(final World w, final IBlockState state)
    {
        return new TileTank(this.capacity, this.tier);
    }
}
