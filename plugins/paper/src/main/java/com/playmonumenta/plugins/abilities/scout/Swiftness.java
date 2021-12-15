package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class Swiftness extends Ability {

	private static final String SWIFTNESS_SPEED_MODIFIER = "SwiftnessSpeedModifier";
	private static final double SWIFTNESS_SPEED_BONUS = 0.2;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;

	private boolean mWasInNoMobilityZone = false;
	private boolean mJumpBoost = true;

	public Swiftness(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Swiftness");
		mInfo.mScoreboardId = "Swiftness";
		mInfo.mShorthandName = "Swf";
		mInfo.mDescriptions.add("Gain +20% Speed when you are not inside a town.");
		mInfo.mDescriptions.add("In addition, gain Jump Boost III when you are not inside a town. Swap hands looking up, not sneaking, and not holding a bow or crossbow to toggle the Jump Boost.");
		mDisplayItem = new ItemStack(Material.RABBIT_FOOT, 1);
		if (player != null) {
			addModifier(player);
		}
	}

	@Override
	public void setupClassPotionEffects() {
		if (getAbilityScore() > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES);

		if (mWasInNoMobilityZone && !isInNoMobilityZone) {
			addModifier(mPlayer);
		} else if (!mWasInNoMobilityZone && isInNoMobilityZone) {
			removeModifier(mPlayer);
		}

		mWasInNoMobilityZone = isInNoMobilityZone;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (getAbilityScore() < 2) {
			return;
		}

		event.setCancelled(true);

		if (mPlayer.isSneaking() || mPlayer.getLocation().getPitch() >= -45 || ItemUtils.isSomeBow(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}

		if (mJumpBoost) {
			mJumpBoost = false;
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP);
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Jump Boost has been turned off");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.6f);
		} else {
			mJumpBoost = true;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
			MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Jump Boost has been turned on");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
		}
	}

	private static void addModifier(Player player) {
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
				new AttributeModifier(SWIFTNESS_SPEED_MODIFIER, SWIFTNESS_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static void removeModifier(Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, SWIFTNESS_SPEED_MODIFIER);
	}

}
