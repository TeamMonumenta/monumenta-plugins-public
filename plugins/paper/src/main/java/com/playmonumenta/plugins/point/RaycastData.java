package com.playmonumenta.plugins.point;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

public class RaycastData {

	private final List<LivingEntity> mEntities = new ArrayList<>();
	private final List<Block> mBlocks = new ArrayList<>();

	public List<LivingEntity> getEntities() {
		return mEntities;
	}

	public List<Block> getBlocks() {
		return mBlocks;
	}


}
