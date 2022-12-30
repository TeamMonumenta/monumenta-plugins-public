package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class WorldlyProtection implements Enchantment {

	private static final double DAMAGE_MULTIPLIER_R1 = 0.05; // 5% reduction for region 1
	private static final double DAMAGE_MULTIPLIER_R2 = 0.0725; // 7.25% reduction for region 2
	private static final double DAMAGE_MULTIPLIER_R3 = 0.1; // 10% reduction for region 3

	@Override
	public String getName() {
		return "Worldly Protection";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.WORLDLY_PROTECTION;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		event.setDamage(event.getDamage() * getDamageMultiplier(value, ServerProperties.getRegion()));
	}

	public static double getDamageMultiplier(double level, ItemStatUtils.Region region) {
		return switch (region) {
			case RING -> 1 - level * DAMAGE_MULTIPLIER_R3;
			case ISLES -> 1 - level * DAMAGE_MULTIPLIER_R2;
			default -> 1 - level * DAMAGE_MULTIPLIER_R1;
		};
	}

}
