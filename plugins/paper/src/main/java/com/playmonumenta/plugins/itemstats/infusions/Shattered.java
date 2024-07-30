package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Shattered implements Infusion {

	public static final int MAX_LEVEL = 3;
	public static final int DEATH_SHATTER = 1;
	public static final int DEATH_LAVA_SHATTER = 2;
	public static final int DEATH_VOID_SHATTER = 3;
	public static final int DURABILITY_SHATTER = MAX_LEVEL; // only max level makes sense as armor stays equipped and would just shatter again almost immediately
	public static final int CURSE_OF_VANISHING_SHATTER = 1;
	public static final int DROPPED_ITEM_DESTROYED = 2;

	public static final int MINING_FATIGUE_AMPLIFIER = 1;

	private static final double MULTIPLIER = 0.04;

	private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public static final String DAMAGE_DEALT_EFFECT = "Shatter-DD";
	public static final String DAMAGE_TAKEN_EFFECT = "Shatter-DT";

	public static final String MESSAGE_DISABLE_TAG = "DisableShatteredAndScalingMessage";

	private static final HashSet<UUID> mShatteredDebuff = new HashSet<>();

	@Override
	public String getName() {
		return "Shattered";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.SHATTERED;
	}

	@Override
	public double getPriorityAmount() {
		return 4998; // just before region scaling
	}

	public static double getMultiplier(int level) {
		return (MULTIPLIER * level);
	}

	public static void updateEffects(Plugin plugin, Player player, int level) {
		if (level == 0) {
			plugin.mEffectManager.clearEffects(player, DAMAGE_DEALT_EFFECT);
			plugin.mEffectManager.clearEffects(player, DAMAGE_TAKEN_EFFECT);
		} else {
			plugin.mEffectManager.addEffect(player, DAMAGE_DEALT_EFFECT, new PercentDamageDealt(40, -getMultiplier(level)).displaysTime(false));
			plugin.mEffectManager.addEffect(player, DAMAGE_TAKEN_EFFECT, new PercentDamageReceived(40, getMultiplier(level)).displaysTime(false));
		}
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		int shatterLevel = (int) plugin.mItemStatManager.getInfusionLevel(player, InfusionType.SHATTERED);
		if (shatterLevel == 0) {
			updateEffects(plugin, player, 0);
		}
		if (shatterLevel >= MAX_LEVEL) {
			// Need to delay this code as it checks equipped items which may not yet have changed, as this method is called from within events
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (hasMaxShatteredItemEquipped(player)) {
					mShatteredDebuff.add(player.getUniqueId());
					plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW_DIGGING, PotionEffect.INFINITE_DURATION, MINING_FATIGUE_AMPLIFIER, false, false));
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
			int shatterLevel = getShatteredLevelsEquipped(player);
			if (shatterLevel > 0 && !plugin.mEffectManager.hasEffect(player, RespawnStasis.class) && !ScoreboardUtils.checkTag(player, MESSAGE_DISABLE_TAG)) {
				if (DateUtils.getSecond() % 12 < 6) {
					MessagingUtils.sendActionBarMessage(player, "Some of your gear is Shattered, giving you " + (hasMaxShatteredItemEquipped(player) ? "Mining Fatigue and " : "") + getMultiplier(shatterLevel) * 100 + "% Weakness and Vulnerability!", NamedTextColor.RED);
				} else {
					MessagingUtils.sendActionBarMessage(player, "Retrieve a Grave, or use Repair Anvils to remove Shattered.", NamedTextColor.RED);
				}
			}
			updateEffects(plugin, player, shatterLevel);
		}
	}

	public static int getShatteredLevelsEquipped(Player player) {
		return (int) Plugin.getInstance().mItemStatManager.getInfusionLevel(player, InfusionType.SHATTERED);
	}

	public static boolean hasMaxShatteredItemEquipped(Player player) {
		if (Plugin.getInstance().mItemStatManager.getInfusionLevel(player, InfusionType.SHATTERED) < MAX_LEVEL) {
			return false;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (ItemStatUtils.getInfusionLevel(player.getEquipment().getItem(slot), InfusionType.SHATTERED) >= MAX_LEVEL) {
				return true;
			}
		}
		return false;
	}

	public static int getHighestShatterLevelEquipped(Player player) {
		return Arrays.stream(EquipmentSlot.values())
				.mapToInt(slot -> ItemStatUtils.getInfusionLevel(player.getEquipment().getItem(slot), InfusionType.SHATTERED))
				.max().orElse(0);
	}

	public static int getShatterLevel(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED);
	}

	public static boolean isMaxShatter(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED) >= MAX_LEVEL;
	}

	public static boolean isShattered(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED) > 0;
	}

	/**
	 * Shatters an item by a given amount of levels.
	 *
	 * @return The new level of shatter of the item. Returns MAX_LEVEL + 1 if it attempts to shatter an item with MAX_LEVEL shatter.
	 */
	public static int shatter(ItemStack item, int numLevels) {
		if (ItemStatUtils.hasEnchantment(item, EnchantmentType.DELETE_ON_SHATTER)) {
			item.setAmount(0);
			return 0;
		}
		Tier tier = ItemStatUtils.getTier(item);
		if (Tier.CURRENCY.equals(tier) || Tier.EVENT_CURRENCY.equals(tier)) {
			return 0;
		}
		int oldLevel = ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED);
		if (oldLevel >= MAX_LEVEL) {
			return MAX_LEVEL + 1;
		}
		int newLevel = Math.min(oldLevel + numLevels, MAX_LEVEL);
		ItemStatUtils.addInfusion(item, InfusionType.SHATTERED, newLevel, NULL_UUID);
		ItemUpdateHelper.generateItemStats(item);
		return newLevel;
	}

	/**
	 * Repairs one level of Shattered on the given item.
	 *
	 * @return Whether the item now has less Shattered levels than before
	 */
	public static boolean unshatterOneLevel(ItemStack item) {
		int oldLevel = ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED);
		ItemMeta meta = item.getItemMeta();
		if (oldLevel <= 0 || (meta instanceof Damageable damageableMeta && damageableMeta.getDamage() == item.getType().getMaxDurability())) {
			return false;
		}
		if (oldLevel == 1) {
			ItemStatUtils.removeInfusion(item, InfusionType.SHATTERED);
		} else {
			ItemStatUtils.addInfusion(item, InfusionType.SHATTERED, oldLevel - 1, NULL_UUID);
		}
		ItemUpdateHelper.generateItemStats(item);
		return true;
	}
}
