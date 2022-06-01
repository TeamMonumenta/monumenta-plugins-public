package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Swiftness extends Ability {

	private static final String SWIFTNESS_SPEED_MODIFIER = "SwiftnessSpeedModifier";
	private static final double SWIFTNESS_SPEED_BONUS = 0.2;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;
	private static final double DODGE_CHANCE = 0.1;

	private boolean mWasInNoMobilityZone = false;
	private boolean mJumpBoost = true;

	public Swiftness(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Swiftness");
		mInfo.mScoreboardId = "Swiftness";
		mInfo.mShorthandName = "Swf";
		mInfo.mDescriptions.add("Gain +20% Speed when you are not inside a town.");
		mInfo.mDescriptions.add("In addition, gain Jump Boost III when you are not inside a town. Swap hands looking up, not sneaking, and not holding a bow, crossbow, or trident to toggle the Jump Boost.");
		mInfo.mDescriptions.add("You now have a 10% chance to dodge any projectile or melee attack.");
		mDisplayItem = new ItemStack(Material.RABBIT_FOOT, 1);
		if (player != null) {
			addModifier(player);
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		DamageEvent.DamageType type = event.getType();
		if ((type == DamageEvent.DamageType.MELEE || type == DamageEvent.DamageType.PROJECTILE) && isEnhanced() && FastUtils.RANDOM.nextDouble() < DODGE_CHANCE) {
			event.setCancelled(true);
			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.CLOUD, loc, 40, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_WITCH_THROW, 1, 2f);
		}
	}

	@Override
	public void setupClassPotionEffects() {
		if (isLevelTwo()) {
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
		if (isLevelOne()) {
			return;
		}

		event.setCancelled(true);

		if (mPlayer.isSneaking() || mPlayer.getLocation().getPitch() >= -45 || ItemUtils.isBowOrTrident(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}

		if (mJumpBoost) {
			mJumpBoost = false;
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP);
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned off");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.6f);
		} else {
			mJumpBoost = true;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned on");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
		}
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private static void addModifier(Player player) {
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
			new AttributeModifier(SWIFTNESS_SPEED_MODIFIER, SWIFTNESS_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static void removeModifier(Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, SWIFTNESS_SPEED_MODIFIER);
	}

	@Override
	public @Nullable String getMode() {
		return mJumpBoost ? null : "disabled";
	}
}
