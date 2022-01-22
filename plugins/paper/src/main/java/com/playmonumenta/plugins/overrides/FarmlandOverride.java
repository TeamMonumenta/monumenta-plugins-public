package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.block.Block;

public class FarmlandOverride extends BaseOverride {
	@Override
	public boolean blockChangeInteraction(Plugin plugin, Block block) {
		return false;
	}
}
