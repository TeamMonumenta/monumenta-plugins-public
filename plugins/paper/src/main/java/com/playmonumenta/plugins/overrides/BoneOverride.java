package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

public class BoneOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack item) {
		if (player == null || !(clickedEntity instanceof Wolf) || item == null) {
			return true;
		}

		GameMode gamemode = player.getGameMode();
		if (gamemode == GameMode.ADVENTURE) {
			return false;
		} else if (gamemode == GameMode.CREATIVE) {
			return true;
		}

		// Don't allow players to feed bones with lore text to wolves or any bones to hostile wolves
		return !((item.hasItemMeta() && item.getItemMeta().hasLore()) || EntityUtils.isHostileMob(clickedEntity));
	}
}
