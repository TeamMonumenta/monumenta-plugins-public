package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.EscapeDeathCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.EscapeDeathEliteHunt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class EscapeDeath extends Ability {

	private static final double TRIGGER_THRESHOLD_HEALTH = 10;
	private static final int RANGE = 5;
	private static final int ELITE_HUNT_RANGE = 8;
	private static final int STUN_DURATION = 20 * 3;
	private static final int BUFF_DURATION = 20 * 8;
	private static final int ABSORPTION_HEALTH = 4;
	private static final double SPEED_PERCENT = 0.3;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EscapeDeathPercentSpeedEffect";
	private static final int JUMP_BOOST_AMPLIFIER = 2;
	private static final int COOLDOWN = 60 * 20;
	private static final int ENHANCEMENT_HEAL_DURATION = 5 * 20;
	private static final int ENHANCEMENT_HUNT_DURATION = 6 * 20;
	private static final double ENHANCEMENT_REGEN_HEAL_PERCENT = 0.05;
	private static final double ENHANCEMENT_HUNT_HEAL_PERCENT = 1.00;
	private static final String ESCAPE_DEATH_ENHANCEMENT_REGEN = "EscapeDeathEnhancementRegenEffect";
	private static final String ESCAPE_DEATH_ENHANCEMENT_ELITE_DEBUFF = "EscapeDeathEliteHuntEffect";

	private static final String DISABLE_JUMP_BOOST_TAG = "EscapeDeathNoJumpBoost";

	public static final String CHARM_ABSORPTION = "Escape Death Absorption Health";
	public static final String CHARM_JUMP = "Escape Death Jump Boost Amplifier";
	public static final String CHARM_SPEED = "Escape Death Speed Amplifier";
	public static final String CHARM_DURATION = "Escape Death Buff Duration";
	public static final String CHARM_COOLDOWN = "Escape Death Cooldown";
	public static final String CHARM_RADIUS = "Escape Death Radius";
	public static final String CHARM_HUNT_RADIUS = "Escape Death Elite Hunt Radius";
	public static final String CHARM_STUN_DURATION = "Escape Death Stun Duration";
	public static final String CHARM_HEALING = "Escape Death Healing";
	public static final String CHARM_ELITE_HEALING = "Escape Death Elite Hunt Healing";
	public static final String CHARM_ELITE_HUNT_DURATION = "Escape Death Elite Hunt Duration";
	public static final String CHARM_REGENERATION_DURATION = "Escape Death Regeneration Duration";

	public static final AbilityInfo<EscapeDeath> INFO =
		new AbilityInfo<>(EscapeDeath.class, "Escape Death", EscapeDeath::new)
			.linkedSpell(ClassAbility.ESCAPE_DEATH)
			.scoreboardId("EscapeDeath")
			.shorthandName("ED")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("When health drops below a threshold, stun nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("toggleJumpBoost", "toggle jump boost", EscapeDeath::toggleJumpBoost,
				new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP).enabled(false), AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.DRAGON_BREATH)
			.priorityAmount(10000);

	private final double mAbsorptionHealth;
	private final double mSpeed;
	private final int mJumpBoost;
	private final int mDuration;
	private final double mRadius;
	private final double mHuntRadius;
	private final int mStunDuration;
	private final double mHealing;
	private final double mHuntHealing;
	private final int mRegenDuration;
	private final int mHuntDuration;
	private final EscapeDeathCS mCosmetic;

	public EscapeDeath(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, ABSORPTION_HEALTH);
		mSpeed = SPEED_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mJumpBoost = JUMP_BOOST_AMPLIFIER + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, BUFF_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
		mHuntRadius = CharmManager.getRadius(mPlayer, CHARM_HUNT_RADIUS, ELITE_HUNT_RANGE);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, STUN_DURATION);
		mHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, ENHANCEMENT_REGEN_HEAL_PERCENT);
		mHuntHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ELITE_HEALING, ENHANCEMENT_HUNT_HEAL_PERCENT);
		mHuntDuration = CharmManager.getDuration(mPlayer, CHARM_ELITE_HUNT_DURATION, ENHANCEMENT_HUNT_DURATION);
		mRegenDuration = CharmManager.getDuration(mPlayer, CHARM_REGENERATION_DURATION, ENHANCEMENT_HEAL_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EscapeDeathCS());
	}

	private boolean toggleJumpBoost() {
		if (ScoreboardUtils.toggleTag(mPlayer, DISABLE_JUMP_BOOST_TAG)) {
			mPlayer.sendActionBar(Component.text("Escape Death's Jump Boost has been disabled"));
		} else {
			mPlayer.sendActionBar(Component.text("Escape Death's Jump Boost has been enabled"));
		}
		return true;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlocked() && !isOnCooldown()) {
			double newHealth = mPlayer.getHealth() - event.getFinalDamage(true);
			boolean dealDamageLater = newHealth < 0 && newHealth > -mAbsorptionHealth && isLevelTwo();
			if (newHealth <= TRIGGER_THRESHOLD_HEALTH && (newHealth > 0 || dealDamageLater)) {
				mPlugin.mEffectManager.damageEvent(event);
				event.setLifelineCancel(true);
				if (event.isCancelled() || event.isBlocked()) {
					return;
				}

				if (dealDamageLater) {
					event.setCancelled(true);
				}

				putOnCooldown();

				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius, mPlayer)) {
					EntityUtils.applyStun(mPlugin, mStunDuration, mob);
				}

				if (isLevelTwo()) {
					AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionHealth, mAbsorptionHealth, mDuration);
					mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
						new PercentSpeed(mDuration, mSpeed, PERCENT_SPEED_EFFECT_NAME)
							.deleteOnAbilityUpdate(true));
					if (!mPlayer.getScoreboardTags().contains(DISABLE_JUMP_BOOST_TAG)) {
						mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, mDuration, mJumpBoost, true, true));
					}
				}

				if (isEnhanced()) {
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), mHuntRadius);
					mobs.removeIf(mob -> !(EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)));

					if (mobs.isEmpty()) { // If no elite mobs in range
						mPlugin.mEffectManager.addEffect(mPlayer, ESCAPE_DEATH_ENHANCEMENT_REGEN,
							new CustomRegeneration(mRegenDuration, mHealing *
								EntityUtils.getMaxHealth(mPlayer), mPlugin).deleteOnAbilityUpdate(true));
					} else { // There is an elite or multiple, afflict random one with the hunt effect
						Entity randomElite = mobs.get(FastUtils.randomIntInRange(0, mobs.size() - 1));
						mPlugin.mEffectManager.addEffect(randomElite, ESCAPE_DEATH_ENHANCEMENT_ELITE_DEBUFF,
							new EscapeDeathEliteHunt(mHuntDuration, mPlayer, mHuntHealing));
					}
				}

				Location loc = mPlayer.getLocation();
				loc.add(0, 1, 0);

				World world = mPlayer.getWorld();

				mCosmetic.activate(mPlayer, world, loc);

				MessagingUtils.sendActionBarMessage(mPlayer, "Escape Death has been activated");

				if (dealDamageLater) {
					mPlayer.setHealth(1);
					AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) newHealth);
				}
			}
		}
	}

	// this should not happen, but better play it safe
	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private static Description<EscapeDeath> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When taking damage leaves you below ")
			.add(a -> TRIGGER_THRESHOLD_HEALTH, TRIGGER_THRESHOLD_HEALTH)
			.add(" health, throw a paralyzing grenade that stuns all mobs within ")
			.add(a -> a.mRadius, RANGE)
			.add(" blocks for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<EscapeDeath> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When this ability is triggered, you gain ")
			.add(a -> a.mAbsorptionHealth, ABSORPTION_HEALTH)
			.add(" absorption health, ")
			.addPercent(a -> a.mSpeed, SPEED_PERCENT)
			.add(" speed, and Jump Boost ")
			.addPotionAmplifier(a -> a.mJumpBoost, JUMP_BOOST_AMPLIFIER)
			.add(" for ")
			.addDuration(a -> a.mDuration, BUFF_DURATION)
			.add(" seconds. If damage taken would kill you but could have been prevented by this skill it will instead do so.");
	}

	private static Description<EscapeDeath> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When this ability is triggered, an Elite mob within ")
			.add(a -> a.mHuntRadius, ELITE_HUNT_RANGE)
			.add(" blocks will glow for ")
			.addDuration(a -> a.mHuntDuration, ENHANCEMENT_HUNT_DURATION)
			.add(" seconds. Killing this glowing Elite will heal for ")
			.addPercent(a -> a.mHuntHealing, ENHANCEMENT_HUNT_HEAL_PERCENT)
			.add(" of your max health and cleanse any debuffs. If there are no Elite mobs then instead gain a regenerating effect that heals for ")
			.addPercent(a -> a.mHealing, ENHANCEMENT_REGEN_HEAL_PERCENT)
			.add(" max health every second for ")
			.addDuration(a -> a.mRegenDuration, ENHANCEMENT_HEAL_DURATION)
			.add(" seconds.");
	}
}
