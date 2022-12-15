package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegionScalingDamageDealt implements Enchantment {

	public static final double[] DAMAGE_DEALT_MULTIPLIER = {1, 0.5, 0.25};

	public static final int MINING_FATIGUE_AMPLIFIER = 0;

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
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		event.setDamage(event.getDamage() * DAMAGE_DEALT_MULTIPLIER[Math.max(0, Math.min((int) value, DAMAGE_DEALT_MULTIPLIER.length - 1))]);
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		ItemStack item = player.getItemInHand();
		if (ItemStatUtils.getAttributeAmount(item, ItemStatUtils.AttributeType.ATTACK_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND) > 0 || ItemUtils.isPickaxe(item) || ItemUtils.isAxe(item) || ItemUtils.isShovel(item)) {
			plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW_DIGGING, 21, MINING_FATIGUE_AMPLIFIER, false, false));
		}
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.REGION_SCALING_DAMAGE_DEALT) <= 0) {
			List<PotionUtils.PotionInfo> potionInfos = plugin.mPotionManager.getAllPotionInfos(player).getOrDefault(PotionManager.PotionID.ITEM, Collections.emptyList());
			for (PotionUtils.PotionInfo potionInfo : potionInfos) {
				if (PotionEffectType.SLOW_DIGGING.equals(potionInfo.mType)
					    && potionInfo.mAmplifier == MINING_FATIGUE_AMPLIFIER
					    && potionInfo.mDuration <= 21) {
					potionInfo.mDuration = 0;
					plugin.mPotionManager.updatePotionStatus(player, 0);
					return;
				}
			}
		}
	}
}
