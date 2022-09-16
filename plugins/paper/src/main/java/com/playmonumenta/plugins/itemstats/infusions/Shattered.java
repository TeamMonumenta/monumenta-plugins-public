package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Shattered implements Infusion {

	public static final int MAX_LEVEL = 3;
	public static final int DEATH_SHATTER = 1;
	public static final int DEATH_LAVA_SHATTER = 2;
	public static final int DEATH_VOID_SHATTER = 3;
	public static final int DURABILITY_SHATTER = MAX_LEVEL; // only max level makes sense as armor stays equipped and would just shatter again almost immediately
	public static final int CURSE_OF_VANISHING_SHATTER = 1;
	public static final int DROPPED_ITEM_DESTROYED = 2;

	public static final int MINING_FATIGUE_AMPLIFIER = 1;

	private static final double LIGHT_DAMAGE_DEALT_MULTIPLIER = -0.3;
	private static final double HEAVY_DAMAGE_DEALT_MULTIPLIER = -0.6;

	private static final double LIGHT_DAMAGE_TAKEN_MULTIPLIER = 0.3;
	private static final double HEAVY_DAMAGE_TAKEN_MULTIPLIER = 0.6;

	private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private static final HashSet<UUID> mShatteredDebuff = new HashSet<>();

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

	public static double getDamageDealtMultiplier(boolean maxShatter) {
		if (maxShatter) {
			return 1 + HEAVY_DAMAGE_DEALT_MULTIPLIER;
		} else {
			return 1 + LIGHT_DAMAGE_DEALT_MULTIPLIER;
		}
	}

	public static double getDamageTakenMultiplier(boolean maxShatter) {
		if (maxShatter) {
			return 1 + HEAVY_DAMAGE_TAKEN_MULTIPLIER;
		} else {
			return 1 + LIGHT_DAMAGE_TAKEN_MULTIPLIER;
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		event.setDamage(event.getDamage() * getDamageDealtMultiplier(hasMaxShatteredItemEquipped(player)));
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		event.setDamage(event.getDamage() * getDamageTakenMultiplier(hasMaxShatteredItemEquipped(player)));
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (plugin.mItemStatManager.getInfusionLevel(player, ItemStatUtils.InfusionType.SHATTERED) >= MAX_LEVEL) {
			// Need to delay this code as it checks equipped items which may not yet have changed, as this method is called from within events
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (hasMaxShatteredItemEquipped(player)) {
					mShatteredDebuff.add(player.getUniqueId());
					plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW_DIGGING, 10000000, MINING_FATIGUE_AMPLIFIER, false, false));
				} else if (mShatteredDebuff.remove(player.getUniqueId())) {
					plugin.mPotionManager.removePotion(player, PotionManager.PotionID.ITEM, PotionEffectType.SLOW_DIGGING, MINING_FATIGUE_AMPLIFIER);
				}
			});
		} else if (mShatteredDebuff.remove(player.getUniqueId())) {
			plugin.mPotionManager.removePotion(player, PotionManager.PotionID.ITEM, PotionEffectType.SLOW_DIGGING, MINING_FATIGUE_AMPLIFIER);
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (oneHz) {
			plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM,
				new PotionEffect(PotionEffectType.BAD_OMEN, 40, 0, false, false, true));
		}
	}

	public static boolean hasMaxShatteredItemEquipped(Player player) {
		if (Plugin.getInstance().mItemStatManager.getInfusionLevel(player, ItemStatUtils.InfusionType.SHATTERED) < MAX_LEVEL) {
			return false;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (ItemStatUtils.getInfusionLevel(player.getEquipment().getItem(slot), ItemStatUtils.InfusionType.SHATTERED) >= MAX_LEVEL) {
				return true;
			}
		}
		return false;
	}

	public static boolean isMaxShatter(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SHATTERED) >= MAX_LEVEL;
	}

	public static boolean isShattered(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SHATTERED) > 0;
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
