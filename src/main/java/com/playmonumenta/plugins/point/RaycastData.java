package com.playmonumenta.plugins.point;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

public class RaycastData {

	private List<LivingEntity> entities = new ArrayList<LivingEntity>();
	private List<Block> blocks = new ArrayList<Block>();

	public List<LivingEntity> getEntities() {
		return entities;
	}

	public List<Block> getBlocks() {
		return blocks;
	}


}
