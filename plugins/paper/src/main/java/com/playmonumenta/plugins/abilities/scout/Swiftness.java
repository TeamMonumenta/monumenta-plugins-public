package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Swiftness extends Ability {

	private static final String SWIFTNESS_SPEED_MODIFIER = "SwiftnessSpeedModifier";
	private static final double SWIFTNESS_SPEED_BONUS = 0.2;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;
	private static final double DODGE_CHANCE = 0.15;

	public static final String CHARM_SPEED = "Swiftness Speed Amplifier";
	public static final String CHARM_JUMP_BOOST = "Swiftness Jump Boost Amplifier";
	public static final String CHARM_DODGE = "Swiftness Dodge Chance";

	public static final AbilityInfo<Swiftness> INFO =
		new AbilityInfo<>(Swiftness.class, "Swiftness", Swiftness::new)
			.linkedSpell(ClassAbility.SWIFTNESS)
			.scoreboardId("Swiftness")
			.shorthandName("Swf")
			.descriptions(
				String.format("Gain +%d%% Speed when you are not inside a town.", (int) (SWIFTNESS_SPEED_BONUS * 100)),
				String.format("In addition, gain Jump Boost %s when you are not inside a town. " +
					              "Swap hands looking up, not sneaking, and not holding a projectile weapon to toggle the Jump Boost.", StringUtils.toRoman(SWIFTNESS_EFFECT_JUMP_LVL + 1)),
				String.format("You now have a %d%% chance to dodge any projectile or melee attack.", (int) (DODGE_CHANCE * 100)))
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle jump boost", Swiftness::toggleJumpBoost, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).lookDirections(AbilityTrigger.LookDirection.UP),
				AbilityTriggerInfo.NOT_HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.RABBIT_FOOT, 1));

	private boolean mWasInNoMobilityZone = false;
	private boolean mJumpBoost = true;

	public Swiftness(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (player != null) {
			addModifier(player);
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		DamageEvent.DamageType type = event.getType();
		if ((type == DamageEvent.DamageType.MELEE || type == DamageEvent.DamageType.PROJECTILE) && isEnhanced() && FastUtils.RANDOM.nextDouble() < DODGE_CHANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DODGE)) {
			event.setCancelled(true);
			Location loc = mPlayer.getLocation();
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.CLOUD, loc, 40, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_WITCH_THROW, 1, 2f);
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

		if (oneSecond && isLevelTwo() && !mWasInNoMobilityZone && mJumpBoost) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 21, SWIFTNESS_EFFECT_JUMP_LVL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST), true, false));
		}
	}

	public void toggleJumpBoost() {
		if (isLevelOne()) {
			return;
		}

		if (mJumpBoost) {
			mJumpBoost = false;
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP);
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned off");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.6f);
		} else {
			mJumpBoost = true;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 21, SWIFTNESS_EFFECT_JUMP_LVL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST), true, false));
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned on");
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.6f);
		}
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private static void addModifier(Player player) {
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
			new AttributeModifier(SWIFTNESS_SPEED_MODIFIER, SWIFTNESS_SPEED_BONUS + CharmManager.getLevelPercentDecimal(player, CHARM_SPEED), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static void removeModifier(Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, SWIFTNESS_SPEED_MODIFIER);
	}

	@Override
	public @Nullable String getMode() {
		return mJumpBoost ? null : "disabled";
	}
}
