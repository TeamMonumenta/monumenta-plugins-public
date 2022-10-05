package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SKTQuestDamageDealt implements Enchantment {

	public static final double DAMAGE_DEALT_MULTIPLIER = 1.6;

	@Override
	public String getName() {
		return "SKTQuestDamageDealt";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SKT_DAMAGE_DEALT;
	}

	@Override
	public double getPriorityAmount() {
		return 5000; // should be the final damage dealt modifier
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (player.getScoreboardTags().contains("SKTQuest") && ServerProperties.getShardName().startsWith("skt")) {
			event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER);
		}
	}
}
