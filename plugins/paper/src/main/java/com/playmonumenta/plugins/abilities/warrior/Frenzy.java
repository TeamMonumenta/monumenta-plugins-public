package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Frenzy extends Ability {

	private static final int FRENZY_DURATION = 5 * 20;

	public Frenzy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Frenzy";
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		int frenzy = getAbilityScore();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (!InventoryUtils.isPickaxeItem(mainHand)) {
			int hasteAmp = frenzy == 1 ? 2 : 3;

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, FRENZY_DURATION, hasteAmp, true, true));

			Location loc = mPlayer.getLocation();
			mWorld.playSound(loc, Sound.ENTITY_POLAR_BEAR_HURT, 0.1f, 1.0f);

			if (frenzy > 1) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, FRENZY_DURATION, 0, true, true));
			}
		}
	}

	@Override
	public void setupClassPotionEffects() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isPickaxeItem(mainHand)) {
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
		}
	}

	@Override
	public void playerItemHeldEvent(ItemStack mainHand, ItemStack offHand) {
		if (InventoryUtils.isPickaxeItem(mainHand)) {
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
		}
	}
}
