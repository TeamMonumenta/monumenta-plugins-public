package com.playmonumenta.plugins.point;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class RaycastData {

	private List<LivingEntity> mEntities = new ArrayList<LivingEntity>();
	private List<Block> mBlocks = new ArrayList<Block>();

	public List<LivingEntity> getEntities() {
		return mEntities;
	}

	public List<Block> getBlocks() {
		return mBlocks;
	}


}
