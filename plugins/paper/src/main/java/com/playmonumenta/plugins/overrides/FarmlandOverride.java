package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;

import com.playmonumenta.plugins.Plugin;

public class FarmlandOverride extends BaseOverride {
	@Override
	public boolean blockChangeInteraction(Plugin plugin, Block block) {
		return false;
	}
}
