package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FireChargeOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		return !(clickedEntity instanceof Creeper);
	}
}

