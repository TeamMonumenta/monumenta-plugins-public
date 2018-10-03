package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Frenzy extends Ability {
	
	private static final int FRENZY_DURATION = 5 * 20;

	
	@Override
	public boolean EntityDeathEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) { 
		int frenzy = getAbilityScore(player);
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if (!InventoryUtils.isPickaxeItem(mainHand)) {
			int hasteAmp = frenzy == 1 ? 2 : 3;

			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, FRENZY_DURATION, hasteAmp, true, true));

			Location loc = player.getLocation();
			mWorld.playSound(loc, Sound.ENTITY_POLAR_BEAR_HURT, 0.1f, 1.0f);

			if (frenzy > 1) 
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, FRENZY_DURATION, 0, true, true));
		}
		return true; 
	}
	
	@Override
	public void setupClassPotionEffects(Player player) { 
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if (InventoryUtils.isPickaxeItem(mainHand)) {
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
		}
	}
	
	@Override
	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		if (InventoryUtils.isPickaxeItem(mainHand)) {
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
		}
	}
	
	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 2;
		info.specId = -1;
		info.scoreboardId = "Frenzy";
		return info;
	}

}
