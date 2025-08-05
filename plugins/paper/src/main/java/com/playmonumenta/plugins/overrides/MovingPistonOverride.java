package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.block.Block;

public class MovingPistonOverride extends BaseOverride {
	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return false;
	}
}
