package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Radiant implements Enchantment {
	private static final String NIGHTVISION_DISABLED_TAG = "RadiantDarksightDisabled";

	@Override
	public String getName() {
		return "Radiant";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RADIANT;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (InventoryUtils.isSoulboundToPlayer(player.getInventory().getItemInMainHand(), player)) {
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.GLOWING, 600, 0, true, false));
			if (!player.getScoreboardTags().contains(NIGHTVISION_DISABLED_TAG)) {
				plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0, true, false));
			}
		}
	}
}
