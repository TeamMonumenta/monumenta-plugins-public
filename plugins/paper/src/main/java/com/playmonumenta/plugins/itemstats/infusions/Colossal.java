package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;

public class Colossal implements Infusion {

	@Override
	public String getName() {
		return "Colossal";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.COLOSSAL;
	}

	@Override
	public void onItemDamage(Plugin plugin, Player player, double value, PlayerItemDamageEvent event) {
		if (event.getDamage() > 0) {
			//Check if the item taking damage is the one with the enchantment
			if (ItemStatUtils.getInfusionLevel(event.getItem(), InfusionType.COLOSSAL) > 0) {
				//With enchant, 50% chance to take durability damage on this event, stacks with unbreaking
				if (FastUtils.RANDOM.nextInt(2) == 0) {
					event.setDamage(0);
				}
			}
		}
	}
}
