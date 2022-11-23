package net.frozenblock.wilderwild.block;

import net.frozenblock.wilderwild.tag.WilderBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class UnknownBushBlock extends DoublePlantBlock {
	public UnknownBushBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
		return state.is(WilderBlockTags.BUSH_MAY_PLACE_ON);
	}
}
