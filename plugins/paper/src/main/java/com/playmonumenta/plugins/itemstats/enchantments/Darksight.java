package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Darksight implements Enchantment {

	private static HashSet<UUID> mDarksightPlayers = new HashSet<>();
	private static final String DARKSIGHT_DISABLED_TAG = "DarksightDisabled";

	@Override
	public String getName() {
		return "Darksight";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DARKSIGHT;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.DARKSIGHT) > 0 && !player.getScoreboardTags().contains(DARKSIGHT_DISABLED_TAG)) {
			mDarksightPlayers.add(player.getUniqueId());
			plugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.NIGHT_VISION, 10000000, 0, true, false));
		} else if (mDarksightPlayers.remove(player.getUniqueId())) {
			plugin.mPotionManager.removePotion(player, PotionID.ITEM, PotionEffectType.NIGHT_VISION);
		}
	}
}
