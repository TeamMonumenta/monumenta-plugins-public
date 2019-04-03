package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

public class FarmlandOverride extends BaseOverride {
	@Override
	public boolean physicsInteraction(Plugin plugin, Player player, Block block) {
		return false;
	}
}
