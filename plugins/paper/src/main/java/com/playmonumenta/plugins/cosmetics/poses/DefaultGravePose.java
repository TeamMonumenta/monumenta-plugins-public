package com.playmonumenta.plugins.cosmetics.poses;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;

public class DefaultGravePose implements GravePose {

	public static final String NAME = "Default";

	@Override
	public Material getDisplayItem() {
		return Material.ARMOR_STAND;
	}

	@Override
	public void playAnimation(ArmorStand grave) {
	}
}
