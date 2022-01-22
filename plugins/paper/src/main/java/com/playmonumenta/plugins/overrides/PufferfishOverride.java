package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class PufferfishOverride extends BaseOverride {
	@Override
	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		if (player == null || InventoryUtils.testForItemWithName(event.getItem(), "Magic Fish")) {
			return false;
		}

		return true;
	}
}
