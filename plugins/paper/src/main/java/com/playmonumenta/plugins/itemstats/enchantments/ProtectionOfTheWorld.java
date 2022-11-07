package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ProtectionOfTheWorld implements Enchantment {

	private static final double DAMAGE_MULTIPLIER_R1 = 0.05; // 5% reduction for region 1
	private static final double DAMAGE_MULTIPLIER_R2 = 0.0725; // 7.25% reduction for region 2
	private static final double DAMAGE_MULTIPLIER_R3 = 0.1; // 10% reduction for region 3

	@Override
	public String getName() {
		return "Worldly Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PROTECTION_OF_THE_WORLD;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {

		double mDamageMultiplier = 1;
		double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.PROTECTION_OF_THE_WORLD);

		if (ServerProperties.getAbilityEnhancementsEnabled()) {
			mDamageMultiplier -= DAMAGE_MULTIPLIER_R3 * level;
		} else if (ServerProperties.getClassSpecializationsEnabled()) {
			mDamageMultiplier -= DAMAGE_MULTIPLIER_R2 * level;
		} else {
			mDamageMultiplier -= DAMAGE_MULTIPLIER_R1 * level;
		}
		event.setDamage(event.getDamage() * mDamageMultiplier);
	}
}
