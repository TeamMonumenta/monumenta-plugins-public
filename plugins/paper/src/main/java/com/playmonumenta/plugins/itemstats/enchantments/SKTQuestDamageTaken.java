package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SKTQuestDamageTaken implements Enchantment {

	public static final double DAMAGE_TAKEN_MULTIPLIER = 0.5;

	@Override
	public String getName() {
		return "SKTQuestDamageTaken";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SKT_DAMAGE_TAKEN;
	}

	@Override
	public double getPriorityAmount() {
		return 0;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (player.getScoreboardTags().contains("SKTQuest")) {
			event.setDamage(event.getDamage() * DAMAGE_TAKEN_MULTIPLIER);
		}
	}
}
