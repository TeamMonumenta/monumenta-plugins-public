package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegionScalingDamageDealt implements Enchantment {

	public static final double DAMAGE_DEALT_MULTIPLIER = 0.5;
	private static HashSet<UUID> mFatiguePlayers = new HashSet<>();

	@Override
	public String getName() {
		return "RegionScalingDamageDealt";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REGION_SCALING_DAMAGE_DEALT;
	}

	@Override
	public double getPriorityAmount() {
		return 5000; // should be the final damage dealt modifier
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER);
		}
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.REGION_SCALING_DAMAGE_DEALT) > 0) {
				mFatiguePlayers.add(player.getUniqueId());
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW_DIGGING, 10000000, 0, false, false));
			} else if (mFatiguePlayers.remove(player.getUniqueId())) {
				plugin.mPotionManager.removePotion(player, PotionManager.PotionID.ITEM, PotionEffectType.SLOW_DIGGING);
			}
		}
	}
}
