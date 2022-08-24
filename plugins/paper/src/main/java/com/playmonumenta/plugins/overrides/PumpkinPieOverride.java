package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PumpkinPieOverride extends BaseOverride {
	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity, ItemStack item) {
		if (player == null
				|| !(clickedEntity instanceof Creeper)
				|| !InventoryUtils.testForItemWithName(item, "Creeper's Delight")
				|| "plots".equals(ServerProperties.getShardName())
				|| "playerplots".equals(ServerProperties.getShardName())
				|| clickedEntity.getScoreboardTags().contains("boss_halloween_creeper")
				|| clickedEntity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return true;
		}

		// TODO: Clean this up once the boss plugin and main plugin are merged
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bossfight " + clickedEntity.getUniqueId().toString() + " boss_halloween_creeper");

		// Consume the item
		item.subtract(1);

		Location loc = clickedEntity.getLocation();
		MMLog.info(player.getName() + " summoned Tricky Creeper at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

		return true;
	}

	@Override
	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		if (player == null || !InventoryUtils.testForItemWithName(event.getItem(), "Creeper's Delight")) {
			return true;
		}

		Location loc = player.getLocation();
		loc.getWorld().playSound(loc, Sound.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0f, 1.0f);
		PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.GLOWING, 4 * 60 * 20, 0));

		return true;
	}
}
