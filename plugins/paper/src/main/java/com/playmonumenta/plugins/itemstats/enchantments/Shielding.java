package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import javax.annotation.Nullable;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Shielding implements Enchantment {

	public static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	public static final double ARMOR_BONUS_PER_LEVEL_DISABLED = 0.1;
	public static final double DISTANCE = 2.5;
	public static final int DISABLE_DURATION = 5 * 20;
	private static final String DISABLE_METAKEY = "ShieldingDisabled";

	@Override
	public String getName() {
		return "Shielding";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SHIELDING;
	}

	public static double applyShielding(DamageEvent event, Plugin plugin, Player player) {
		LivingEntity source = event.getSource();
		if (doesShieldingApply(player, source)) {
			double bonusPerLevel = MetadataUtils.happenedInRecentTicks(player, DISABLE_METAKEY, DISABLE_DURATION) ? ARMOR_BONUS_PER_LEVEL_DISABLED : ARMOR_BONUS_PER_LEVEL;
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SHIELDING) * bonusPerLevel;
		}
		return 0;
	}

	public static boolean doesShieldingApply(Player player, @Nullable LivingEntity source) {
		return source != null && player.getWorld() == source.getWorld() && player.getLocation().distance(source.getLocation()) <= DISTANCE;
	}

	public static void disable(Player player) {
		Plugin plugin = Plugin.getInstance();
		if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SHIELDING) > 0 && !MetadataUtils.happenedInRecentTicks(player, DISABLE_METAKEY, DISABLE_DURATION)) {
			player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.75f, 0.5f);
			MetadataUtils.markThisTick(plugin, player, DISABLE_METAKEY);
		}
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (doesShieldingApply(player, source)
			    && event.getType() == DamageEvent.DamageType.MELEE
			    && event.getDamage() > 0
			    && source.getEquipment() != null
			    && ItemUtils.isAxe(source.getEquipment().getItemInMainHand())
			    && !event.isBlockedByShield()) {
			disable(player);
		}
	}

}
