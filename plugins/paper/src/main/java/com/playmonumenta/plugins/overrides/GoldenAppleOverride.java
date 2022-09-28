package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GoldenAppleOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack itemInHand) {
		// Need an extra check for golden apples, and can't put them into two overrides
		HorseFoodOverride horseFoodOverride = new HorseFoodOverride();
		if (!horseFoodOverride.rightClickEntityInteraction(plugin, player, clickedEntity, itemInHand)) {
			return false;
		}

		return (clickedEntity == null || !(clickedEntity instanceof ZombieVillager));
	}

	@Override
	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		ItemStack mainhand = player.getItemInHand();

		if (player == null || mainhand.getType() != event.getItem().getType()) {
			return true;
		}

		event.setCancelled(true);

		player.setItemInHand(mainhand.subtract(1));
		player.setFoodLevel(Math.min(player.getFoodLevel() + 4, 20));
		player.setSaturation(Math.min(player.getSaturation() + 9.6f, player.getFoodLevel()));

		return true;
	}
}
