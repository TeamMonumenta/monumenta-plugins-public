package com.playmonumenta.plugins.overrides;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;

public class TridentOverride extends BaseOverride {
	@Override
	public boolean playerRiptide(Plugin plugin, Player player, PlayerRiptideEvent event) {
		if (player == null) {
			return true;
		}

		if (event.getItem().getEnchantmentLevel(Enchantment.RIPTIDE) > 0 && !LocationUtils.isLocationInWater(player.getEyeLocation())) {
			return false;
		}

		return true;
	}
}
