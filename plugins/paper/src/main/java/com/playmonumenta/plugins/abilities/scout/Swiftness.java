package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.SwiftnessCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Swiftness extends Ability {
	private static final String SPEED_SRC = "SwiftnessSpeedModifier";
	private static final double SPEED_POTENCY = 0.2;
	private static final int JUMP_BOOST_POTENCY = 2; // Jump Boost 3, effect potency is 0 indexed
	private static final String ATTACK_SPEED_SRC = "SwiftnessAttackSpeedModifier";
	private static final double ATTACK_SPEED_POTENCY_1 = 0.1;
	private static final double ATTACK_SPEED_POTENCY_2 = 0.15;
	private static final double ENHANCEMENT_CDR = 0.05;
	private static final String NO_JUMP_BOOST_TAG = "SwiftnessJumpBoostDisable";

	public static final String CHARM_SPEED = "Swiftness Speed Amplifier";
	public static final String CHARM_JUMP_BOOST = "Swiftness Jump Boost Amplifier";
	public static final String CHARM_ATTACK_SPEED = "Swiftness Attack Speed Amplifier";
	public static final String CHARM_ENHANCE_CDR = "Swiftness Enhancement Cooldown Reduction";

	public static final AbilityInfo<Swiftness> INFO =
		new AbilityInfo<>(Swiftness.class, "Swiftness", Swiftness::new)
			.linkedSpell(ClassAbility.SWIFTNESS)
			.scoreboardId("Swiftness")
			.shorthandName("Swf")
			.descriptions(
				String.format("Gain %s movement speed when you are not inside a town and a passive %s attack speed.",
					StringUtils.multiplierToPercentageWithSign(SPEED_POTENCY),
					StringUtils.multiplierToPercentageWithSign(ATTACK_SPEED_POTENCY_1)),
				String.format("The attack speed is increased to %s and gain Jump Boost %s when you are not inside a town.",
					StringUtils.multiplierToPercentageWithSign(ATTACK_SPEED_POTENCY_2),
					StringUtils.toRoman(JUMP_BOOST_POTENCY + 1)),
				String.format("Breaking a spawner reduces the cooldown of all your skills by %s.",
					StringUtils.multiplierToPercentageWithSign(ENHANCEMENT_CDR)))
			.simpleDescription("Gain movement speed, attack speed, and increased jump height.")
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle jump boost", null,
				Swiftness::toggleJumpBoost, new AbilityTrigger(AbilityTrigger.Key.SWAP).enabled(false).sneaking(false)
				.lookDirections(AbilityTrigger.LookDirection.UP)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON), null,
				player -> {
					Swiftness swiftness = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(player, Swiftness.class);
					return swiftness != null && swiftness.isLevelTwo();
				}))
			.remove(player -> {
				removeMovementSpeed(player);
				removeAttackSpeed(player);
			})
			.displayItem(Material.RABBIT_FOOT);

	private final double mEnhancementCDR;
	private boolean mWasInNoMobilityZone = false;
	private boolean mJumpBoost;
	private final SwiftnessCS mCosmetic;

	public Swiftness(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mJumpBoost = !mPlayer.getScoreboardTags().contains(NO_JUMP_BOOST_TAG);
		/* This looks goofy but it makes CDR stacking with the charm effect multiplicative similar to other CDR sources */
		mEnhancementCDR = 1 - (1 - ENHANCEMENT_CDR) * (1 - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_CDR));
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SwiftnessCS());

		addMovementSpeed(mPlayer);
		EntityUtils.addAttribute(mPlayer, Attribute.GENERIC_ATTACK_SPEED,
			new AttributeModifier(ATTACK_SPEED_SRC, (isLevelTwo() ? ATTACK_SPEED_POTENCY_2 : ATTACK_SPEED_POTENCY_1) +
				CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK_SPEED), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	@Override
	public boolean blockBreakEvent(final BlockBreakEvent event) {
		if (isEnhanced() && event.getBlock().getType() == Material.SPAWNER) {
			/* Attempt to apply cooldown reduction. If at least one cooldown changed, do cosmetic */
			if (mPlugin.mTimers.updateCooldownsPercent(mPlayer, mEnhancementCDR) > 0) {
				mCosmetic.swiftnessEnhancement(mPlayer, event.getBlock().getLocation());
			}
		}
		return true;
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		final boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES);

		if (mWasInNoMobilityZone && !isInNoMobilityZone) {
			addMovementSpeed(mPlayer);
		} else if (!mWasInNoMobilityZone && isInNoMobilityZone) {
			removeMovementSpeed(mPlayer);
		}

		mWasInNoMobilityZone = isInNoMobilityZone;

		if (oneSecond && isLevelTwo() && !mWasInNoMobilityZone && mJumpBoost) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 21,
				JUMP_BOOST_POTENCY + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST), true, false));
		}
	}

	public boolean toggleJumpBoost() {
		if (mJumpBoost) {
			mJumpBoost = false;
			mPlayer.addScoreboardTag(NO_JUMP_BOOST_TAG);
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP);
			mCosmetic.toggleJumpBoostOff(mPlayer);
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned off");
		} else {
			mJumpBoost = true;
			mPlayer.removeScoreboardTag(NO_JUMP_BOOST_TAG);
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 21,
				JUMP_BOOST_POTENCY + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST), true, false));
			mCosmetic.toggleJumpBoostOn(mPlayer);
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned on");
		}
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	private static void addMovementSpeed(final Player player) {
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
			new AttributeModifier(SPEED_SRC, SPEED_POTENCY +
				CharmManager.getLevelPercentDecimal(player, CHARM_SPEED), AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static void removeMovementSpeed(final Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, SPEED_SRC);
	}

	private static void removeAttackSpeed(final Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_ATTACK_SPEED, ATTACK_SPEED_SRC);
	}

	@Override
	public @Nullable String getMode() {
		return mJumpBoost ? null : "disabled";
	}
}
