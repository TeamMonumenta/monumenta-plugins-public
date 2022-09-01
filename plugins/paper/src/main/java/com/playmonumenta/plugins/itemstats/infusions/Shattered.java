package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Shattered implements Infusion {

	public static final int MAX_LEVEL = 3;
	public static final int DEATH_SHATTER = 1;
	public static final int DEATH_LAVA_SHATTER = 2;
	public static final int DEATH_VOID_SHATTER = 3;
	public static final int DURABILITY_SHATTER = MAX_LEVEL; // only max level makes sense as armor stays equipped and would just shatter again almost immediately
	public static final int CURSE_OF_VANISHING_SHATTER = 1;
	public static final int DROPPED_ITEM_DESTROYED = 2;

	public static final double DAMAGE_DEALT_MULTIPLIER = -0.025;
	public static final double DAMAGE_TAKEN_MULTIPLIER = 0.025;

	private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	@Override
	public String getName() {
		return "Shattered";
	}

	@Override
	public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.SHATTERED;
	}

	@Override
	public double getPriorityAmount() {
		return 4998; // just before region scaling
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		event.setDamage(event.getDamage() * (1 + value * DAMAGE_DEALT_MULTIPLIER));
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		event.setDamage(event.getDamage() * (1 + value * DAMAGE_TAKEN_MULTIPLIER));
	}

	/**
	 * Shatters an item by a given amount of levels.
	 *
	 * @return Whether the item now has more Shattered levels than before
	 */
	public static boolean shatter(ItemStack item, int numLevels) {
		int oldLevel = ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SHATTERED);
		if (oldLevel >= MAX_LEVEL) {
			return false;
		}
		int newLevel = Math.min(oldLevel + numLevels, MAX_LEVEL);
		ItemStatUtils.addInfusion(item, ItemStatUtils.InfusionType.SHATTERED, newLevel, NULL_UUID);
		ItemStatUtils.generateItemStats(item);
		return true;
	}

	/**
	 * Repairs one level of Shattered on the given item.
	 *
	 * @return Whether the item now has less Shattered levels than before
	 */
	public static boolean unshatterOneLevel(ItemStack item) {
		int oldLevel = ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SHATTERED);
		if (oldLevel <= 0) {
			return false;
		}
		if (oldLevel == 1) {
			ItemStatUtils.removeInfusion(item, ItemStatUtils.InfusionType.SHATTERED);
		} else {
			ItemStatUtils.addInfusion(item, ItemStatUtils.InfusionType.SHATTERED, oldLevel - 1, NULL_UUID);
		}
		ItemStatUtils.generateItemStats(item);
		return true;
	}

}
