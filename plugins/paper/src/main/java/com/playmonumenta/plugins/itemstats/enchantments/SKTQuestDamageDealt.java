package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SKTQuestDamageDealt implements Enchantment {

	public static final double DAMAGE_DEALT_MULTIPLIER = 2.7;

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
		return 5000; // same priority as RegionScalingDamageDealt; both are multiplicative modifiers so the order between them doesn't matter
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (player.getScoreboardTags().contains("SKTQuest")) {
			event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER);
		}
	}
}
