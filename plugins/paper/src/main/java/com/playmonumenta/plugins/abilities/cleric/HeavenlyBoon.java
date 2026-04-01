package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.abilities.cleric.seraph.HallowedBeam;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HeavenlyBoonCS;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.HeavenlyBoonTracker;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public final class HeavenlyBoon extends Ability implements KillTriggeredAbility {
	private static final String BOON_EFFECT_NAME = "ClericHeavenlyBoonTracker";
	private static final int MOB_EFFECT_DURATION = (int) (Constants.TICKS_PER_SECOND * 1.5);

	private static final double HEAVENLY_BOON_HEAL = 0.2;
	private static final double HEAVENLY_BOON_STRENGTH = 0.1;
	private static final int HEAVENLY_BOON_REGEN = 0; // Actually 1 because of how effects work
	private static final double HEAVENLY_BOON_RESISTANCE = 0.1;
	private static final double HEAVENLY_BOON_SPEED = 0.2;
	private static final double HEAVENLY_BOON_ABSORPTION = 0.2;
	private static final double HEAVENLY_BOON_COOLDOWN_RECHARGE_RATE = 0.1;
	private static final int HEAVENLY_BOON_HEAL_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int HEAVENLY_BOON_EFFECT_DURATION_1 = Constants.TICKS_PER_SECOND * 20;
	private static final int HEAVENLY_BOON_EFFECT_DURATION_2 = Constants.TICKS_PER_SECOND * 40;
	private static final int HEAVENLY_BOON_EFFECT_DURATION_ENHANCEMENT_BONUS = Constants.TICKS_PER_SECOND * 20;

	private static final double HEAVENLY_BOON_1_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.15;
	private static final double HEAVENLY_BOON_CHANCE_INCREASE_INCREMENT = 0.01;
	private static final double HEAVENLY_BOON_CHANCE_INCREASE_MAX = 0.2;
	private static final int HEAVENLY_BOON_CHANCE_INCREASE_INTERVAL = 2 * 20;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0;
	private static final int BOSS_DAMAGE_THRESHOLD_R1 = 100;
	private static final int BOSS_DAMAGE_THRESHOLD_R2 = 200;
	private static final int BOSS_DAMAGE_THRESHOLD_R3 = 300;

	public static final String CHARM_CHANCE = "Heavenly Boon Potion Chance";
	public static final String CHARM_CHANCE_INCREASE = "Heavenly Boon Potion Chance Increase";
	public static final String CHARM_MAX_CHANCE_INCREASE = "Heavenly Boon Max Potion Chance Increase";
	public static final String CHARM_CHANCE_INCREASE_INTERVAL = "Heavenly Boon Potion Chance Increase Interval";
	public static final String CHARM_EFFECT_DURATION = "Heavenly Boon Potion Effect Duration";
	public static final String CHARM_RADIUS = "Heavenly Boon Radius";

	public static final String CHARM_HEAL_AMPLIFIER = "Heavenly Boon Healing";
	public static final String CHARM_REGEN_AMPLIFIER = "Heavenly Boon Regeneration Amplifier";
	public static final String CHARM_SPEED_AMPLIFIER = "Heavenly Boon Speed Amplifier";
	public static final String CHARM_STRENGTH_AMPLIFIER = "Heavenly Boon Strength Amplifier";
	public static final String CHARM_RESIST_AMPLIFIER = "Heavenly Boon Resistance Amplifier";
	public static final String CHARM_ABSORPTION_AMPLIFIER = "Heavenly Boon Absorption Amplifier";
	public static final String CHARM_COOLDOWN_RECHARE_RATE = "Heavenly Boon Cooldown Recharge Rate";

	public static final AbilityInfo<HeavenlyBoon> INFO =
		new AbilityInfo<>(HeavenlyBoon.class, "Heavenly Boon", HeavenlyBoon::new)
			.linkedSpell(ClassAbility.HEAVENLY_BOON)
			.scoreboardId("HeavenlyBoon")
			.shorthandName("HB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Share all positive splash potion effects with nearby players and occasionally generate splash potions when killing Heretics.")
			.displayItem(Material.SPLASH_POTION);

	private enum BoonPotion {
		REGENERATION,
		RESISTANCE,
		ABSORPTION,
		STRENGTH,
		SPEED,
		COOLDOWN_RECHARGE_RATE
	}

	private final KillTriggeredAbilityTracker mTracker;
	private final double mChance;
	private final double mChanceIncrease;
	private final double mMaxChanceIncrease;
	private final int mChanceIncreaseInterval;
	private final double mHealth;
	private final int mRegeneration;
	private final double mResistance;
	private final double mAbsorption;
	private final double mStrength;
	private final double mSpeed;
	private final double mCooldownRechargeRate;
	private final List<BoonPotion> mBoonDropsList;
	private final int mEffectDuration;
	private final double mRadius;
	private final HeavenlyBoonCS mCosmetic;

	private final BoonPotion[] mLastBoons = {null, null};
	private int mLastBoonTick = 0;

	public HeavenlyBoon(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mTracker = new KillTriggeredAbilityTracker(mPlayer, this, BOSS_DAMAGE_THRESHOLD_R1, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new HeavenlyBoonCS());

		mChance = CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CHANCE) + (isLevelOne() ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE);
		mChanceIncrease = CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CHANCE_INCREASE) + HEAVENLY_BOON_CHANCE_INCREASE_INCREMENT;
		mMaxChanceIncrease = CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MAX_CHANCE_INCREASE) + HEAVENLY_BOON_CHANCE_INCREASE_MAX;
		mChanceIncreaseInterval = CharmManager.getDuration(mPlayer, CHARM_CHANCE_INCREASE_INTERVAL, HEAVENLY_BOON_CHANCE_INCREASE_INTERVAL);
		mEffectDuration = CharmManager.getDuration(mPlayer, CHARM_EFFECT_DURATION, (isLevelOne() ? HEAVENLY_BOON_EFFECT_DURATION_1 : HEAVENLY_BOON_EFFECT_DURATION_2) + (isEnhanced() ? HEAVENLY_BOON_EFFECT_DURATION_ENHANCEMENT_BONUS : 0));
		mHealth = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL_AMPLIFIER, HEAVENLY_BOON_HEAL);
		mRegeneration = HEAVENLY_BOON_REGEN + (int) CharmManager.getLevel(player, CHARM_REGEN_AMPLIFIER);
		mResistance = HEAVENLY_BOON_RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESIST_AMPLIFIER);
		mAbsorption = HEAVENLY_BOON_ABSORPTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ABSORPTION_AMPLIFIER);
		mStrength = HEAVENLY_BOON_STRENGTH + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STRENGTH_AMPLIFIER);
		mSpeed = HEAVENLY_BOON_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED_AMPLIFIER);
		mCooldownRechargeRate = HEAVENLY_BOON_COOLDOWN_RECHARGE_RATE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_COOLDOWN_RECHARE_RATE);
		mBoonDropsList = isEnhanced() ? Arrays.stream(BoonPotion.values()).toList() : List.of(BoonPotion.REGENERATION, BoonPotion.RESISTANCE, BoonPotion.ABSORPTION, BoonPotion.STRENGTH, BoonPotion.SPEED);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, HEAVENLY_BOON_RADIUS);
	}

	/*
	 * When a cleric is hit by a splash potion, distribute full durations of any positive effects to all
	 * nearby players. If a player gets a full-duration effect in this way, they are removed from the
	 * affectedEntities list so they don't get potion effects applied to them twice
	 */
	@Override
	public boolean playerSplashedByPotionEvent(final Collection<LivingEntity> affectedEntities, final ThrownPotion potion, final PotionSplashEvent event) {
		if (!(potion.getShooter() instanceof Player)) {
			return true;
		}

		final boolean hasPositiveEffects = PotionUtils.hasPositiveEffects(PotionUtils.getEffects(potion.getItem()));
		if ((PotionUtils.hasNegativeEffects(potion.getItem()) || ItemStatUtils.hasNegativeEffect(potion.getItem(), false)) && !hasPositiveEffects) {
			return true;
		}

		if (event.isCancelled()) {
			//Another cleric already spread this potion
			return false;
		}

		if (event.getIntensity(mPlayer) >= HEAVENLY_BOON_TRIGGER_INTENSITY) {
			/* If within range, apply full strength of all potion effects to all nearby players */

			for (final Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
				// Don't buff players that have their class disabled
				if (p.getScoreboardTags().contains("disable_class")) {
					continue;
				}

				ItemStatUtils.applyCustomEffects(mPlugin, p, potion.getItem(), false);

				/* Remove this player from the "usual" application of potion effects */
				affectedEntities.remove(p);
			}
		}

		return true;
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (event.getType().equals(DamageType.TRUE)) {
			return false; // don't count true damage
		}

		mTracker.updateDamageDealtToBosses(event);

		if (enemy.isValid() && !isOnCooldown()) {
			// Construct custom source for each player.
			final String source = BOON_EFFECT_NAME + mPlayer.getName();
			mPlugin.mEffectManager.addEffect(enemy, source, new HeavenlyBoonTracker(MOB_EFFECT_DURATION, mPlayer.getUniqueId()));
		}
		return false;
	}

	@Override
	public void triggerOnKill(final LivingEntity mob) {
		if (Crusade.enemyTriggersAbilities(mob) && (FastUtils.RANDOM.nextDouble() < (mChance + Math.min(mMaxChanceIncrease, Math.floor((double) (Bukkit.getCurrentTick() - mLastBoonTick) / mChanceIncreaseInterval) * mChanceIncrease)) || MetadataUtils.happenedThisTick(mob, HallowedBeam.BEAM_2_BOON_MARK + mPlayer.getName()))) {
			// Select random boon effect, but cannot be the same as the last 2
			List<BoonPotion> drops = new ArrayList<>(mBoonDropsList);
			drops.remove(mLastBoons[0]);
			drops.remove(mLastBoons[1]);
			BoonPotion drop = drops.get(FastUtils.randomIntInRange(0, drops.size() - 1));

			mLastBoons[1] = mLastBoons[0];
			mLastBoons[0] = drop;
			mLastBoonTick = Bukkit.getCurrentTick();

			List<Player> players = EntityUtils.getNearestPlayers(mPlayer.getLocation(), mRadius);
			mPlugin.mEffectManager.addEffect(mPlayer, "HeavenlyBoonHealing", new CustomRegeneration(HEAVENLY_BOON_HEAL_DURATION, EntityUtils.getMaxHealth(mPlayer) * mHealth * 5 / HEAVENLY_BOON_HEAL_DURATION, 5, mPlayer, true, mPlugin));
			switch (drop) {
				case REGENERATION -> {
					mCosmetic.splashEffectRegeneration(mPlayer, mob);
					players.forEach(p -> PotionUtils.applyPotion(mPlugin, p, new PotionEffect(PotionEffectType.REGENERATION, mEffectDuration, mRegeneration)));
				}
				case RESISTANCE -> {
					mCosmetic.splashEffectResistance(mPlayer, mob);
					players.forEach(p -> mPlugin.mEffectManager.addEffect(p, "HeavenlyBoonResistance", new PercentDamageReceived(mEffectDuration, -mResistance)));
				}
				case ABSORPTION -> {
					mCosmetic.splashEffectAbsorption(mPlayer, mob);
					players.forEach(p -> AbsorptionUtils.addAbsorption(p, EntityUtils.getMaxHealth(p) * mAbsorption, EntityUtils.getMaxHealth(p) * mAbsorption, mEffectDuration));
				}
				case STRENGTH -> {
					mCosmetic.splashEffectStrength(mPlayer, mob);
					players.forEach(p -> mPlugin.mEffectManager.addEffect(p, "HeavenlyBoonStrength", new PercentDamageDealt(mEffectDuration, mStrength)));
				}
				case SPEED -> {
					mCosmetic.splashEffectSpeed(mPlayer, mob);
					players.forEach(p -> mPlugin.mEffectManager.addEffect(p, "HeavenlyBoonSpeed", new PercentSpeed(mEffectDuration, mSpeed, "HeavenlyBoonSpeed")));
				}
				case COOLDOWN_RECHARGE_RATE -> {
					mCosmetic.splashEffectCooldownRechargeRate(mPlayer, mob);
					players.forEach(p -> mPlugin.mEffectManager.addEffect(p, "HeavenlyBoonCooldownRechargeRate", new AbilityCooldownRechargeRate(mEffectDuration, mCooldownRechargeRate)));
				}
				default -> { }
			}
		}
	}

	private static Description<HeavenlyBoon> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("When you are splashed with a beneficial potion,")
			.addLine("all nearby players also gain its effects.")
			.addLine()
			.addStat("Potion Sharing Radius: %r")
				.statValues(stat(a -> a.mRadius, HEAVENLY_BOON_RADIUS))
			.addLine()
			.addLine("When a *Heretic* you've damaged in the last %t dies,").styles(Cleric.HERETIC_COLOR)
				.statValues(stat(MOB_EFFECT_DURATION))
			.addLine("or when you deal %d0R damage to Bosses, you")
				.statValues(perRegion(BOSS_DAMAGE_THRESHOLD_R1, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3))
			.addLine("have a chance to be splashed by a healing")
			.addLine("potion with a random bonus effect. The chance")
			.addLine("increases over time and resets on splash.")
			.addLine()
			.addStat("Potion Chance: %p1 + %p every %t (max %p1)")
				.statValues(stat(a -> a.mChance, HEAVENLY_BOON_1_CHANCE),
					stat(a -> a.mChanceIncrease, HEAVENLY_BOON_CHANCE_INCREASE_INCREMENT),
					stat(a -> a.mChanceIncreaseInterval, HEAVENLY_BOON_CHANCE_INCREASE_INTERVAL),
					stat(a -> a.mChance + a.mMaxChanceIncrease, HEAVENLY_BOON_1_CHANCE + HEAVENLY_BOON_CHANCE_INCREASE_MAX))
			.addStat("Healing: %p HP over %t")
				.statValues(stat(a -> a.mHealth, HEAVENLY_BOON_HEAL), stat(HEAVENLY_BOON_HEAL_DURATION))
			.addStat("Bonus Effect: (can't be the same twice in 3 boons)")
				.addListItem("Regeneration %d").statValues(stat(a -> a.mRegeneration + 1, HEAVENLY_BOON_REGEN + 1))
				.addListItem("+%p Damage").statValues(stat(a -> a.mStrength, HEAVENLY_BOON_STRENGTH))
				.addListItem("+%p Resistance").statValues(stat(a -> a.mResistance, HEAVENLY_BOON_RESISTANCE))
				.addListItem("+%p Speed").statValues(stat(a -> a.mSpeed, HEAVENLY_BOON_SPEED))
				.addListItem("+%p Absorption").statValues(stat(a -> a.mAbsorption, HEAVENLY_BOON_ABSORPTION))
			.addStat("Bonus Effect Duration: %t1")
				.statValues(stat(a -> a.mEffectDuration, HEAVENLY_BOON_EFFECT_DURATION_1))
			.addDashedLine();
	}

	private static Description<HeavenlyBoon> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Heavenly Boon*'s potion chance and").styles(UNDERLINED)
			.addLine("effect duration.")
			.addLine()
			.addStatComparison("Chance: %p1 -> %p2 + %p every %t (max %p2)")
			.statValues(stat(HEAVENLY_BOON_1_CHANCE),
				stat(a -> a.mChance, HEAVENLY_BOON_2_CHANCE),
				stat(a -> a.mChanceIncrease, HEAVENLY_BOON_CHANCE_INCREASE_INCREMENT),
				stat(a -> a.mChanceIncreaseInterval, HEAVENLY_BOON_CHANCE_INCREASE_INTERVAL),
				stat(a -> a.mChance + a.mMaxChanceIncrease, HEAVENLY_BOON_2_CHANCE + HEAVENLY_BOON_CHANCE_INCREASE_MAX))
			.addStatComparison("Bonus Effect Duration: %t1 -> %t2")
			.statValues(
				stat(HEAVENLY_BOON_EFFECT_DURATION_1),
				stat(a -> a.mEffectDuration, HEAVENLY_BOON_EFFECT_DURATION_2))
			.addDashedLine();
	}

	private static Description<HeavenlyBoon> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Expand *Heavenly Boon*'s list of bonus potion").styles(UNDERLINED)
			.addLine("effects and increase the effect duration")
			.addLine("by +%t.")
				.statValues(stat(HEAVENLY_BOON_EFFECT_DURATION_ENHANCEMENT_BONUS))
			.addLine()
			.addStat("New Bonus Effect:")
				.addListItem("+%p Cooldown Recharge Rate")
					.statValues(stat(a -> a.mCooldownRechargeRate, HEAVENLY_BOON_COOLDOWN_RECHARGE_RATE))
			.addDashedLine();
	}
}
