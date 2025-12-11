package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.TabooCS;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentPotionRecharge;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class Taboo extends Ability {
	private static final double PERCENT_HEALTH_DAMAGE_PER_SECOND = 0.025;
	private static final double PERCENT_HEALTH_DAMAGE_RAMPING_PER_SECOND = 0.006;
	private static final int SELF_DAMAGE_INTERVAL = 5;
	private static final int SELF_DAMAGE_TICKS_PER_SECOND = 20 / SELF_DAMAGE_INTERVAL;
	// Correction for the hp loss so that when applying it in faster intervals it doesn't end up being more than just applying it once every sec
	// 1.5 / 4 when 4 self damage ticks per second
	private static final double SELF_DAMAGE_LOSS_CORRECTION = (double) (SELF_DAMAGE_TICKS_PER_SECOND - 1) / 2 / SELF_DAMAGE_TICKS_PER_SECOND;
	private static final double ABSORPTION_LOSS_THRESHOLD = 0.1;
	private static final double PERCENT_ABSORPTION_LOSS_PER_SECOND = 0.1;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.5;
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "TabooKnockbackResistanceEffect";
	private static final double MAGIC_DAMAGE_INCREASE_1 = 0.3;
	private static final double MAGIC_DAMAGE_INCREASE_2 = 0.4;
	private static final double MISSING_HEALTH_FRACTION_PER_ABSORPTION = 0.1;
	private static final double ABSORPTION_ON_DEACTIVATION_AMOUNT = 1;
	private static final double ABSORPTION_ON_DEACTIVATION_MAX = 10;
	private static final int ABSORPTION_ON_DEACTIVATION_DURATION = 20 * 8;
	private static final int COOLDOWN = 20 * 10;
	private static final double BASE_RECHARGE_RATE_BONUS = 0.5;
	public static final String RECHARGE_RATE_MULTIPLIER_NAME = "TabooPotionRechargeRateMultiplier";

	public static final String CHARM_RECHARGE_RATE = "Taboo Recharge Rate";
	public static final String CHARM_COOLDOWN = "Taboo Cooldown";
	public static final String CHARM_SELF_DAMAGE = "Taboo Base Self Damage";
	public static final String CHARM_SELF_DAMAGE_RAMPING = "Taboo Self Damage Increase";
	public static final String CHARM_KNOCKBACK_RESISTANCE = "Taboo Knockback Resistance";
	public static final String CHARM_DAMAGE = "Taboo Damage Modifier";
	public static final String CHARM_MISSING_HEALTH_FRACTION_PER_ABSORPTION = "Taboo Missing Health Fraction Per Absorption";
	public static final String CHARM_ABSORPTION_ON_DEACTIVATION_AMOUNT = "Taboo Absorption On Deactivation Amount";
	public static final String CHARM_ABSORPTION_ON_DEACTIVATION_MAX = "Taboo Absorption On Deactivation Max";
	public static final String CHARM_ABSORPTION_ON_DEACTIVATION_DURATION = "Taboo Absorption On Deactivation Duration";

	public static final AbilityInfo<Taboo> INFO =
		new AbilityInfo<>(Taboo.class, "Taboo", Taboo::new)
			.linkedSpell(ClassAbility.TABOO)
			.scoreboardId("Taboo")
			.shorthandName("Tb")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Sacrifice health in exchange for increased magic damage.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("activate", "activate", Taboo::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.HONEY_BOTTLE);

	private final double mMagicDamageIncrease;
	private final double mSelfDamagePercentPerSecond;
	private final double mPercentDamagePercentRampingPerSecond;
	private final double mMissingHealthFractionPerAbsorption;
	private final double mAbsorptionOnDeactivationAmount;
	private final double mAbsorptionOnDeactivationMax;
	private final int mAbsorptionOnDeactivationDuration;
	private final double mKBR;
	private final double mRechargeRateBonus;
	private final double mSelfDamageRampingPerTabooTick;
	private final double mTickRateAdjustedAbsorptionLossThreshold;
	private final double mPercentAbsorptionLossPerTabooTick;

	private @Nullable AlchemistPotions mAlchemistPotions;

	private boolean mIsActive = false;
	private int mActiveTimer = 0;
	private double mCurrentSelfDamagePercentPerTabooTick;
	private boolean mAbsorptionLossStarted = false;
	private int mLastCastTicks = 0;
	private final TabooCS mCosmetic;

	public Taboo(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mMagicDamageIncrease = (isLevelOne() ? MAGIC_DAMAGE_INCREASE_1 : MAGIC_DAMAGE_INCREASE_2)
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mSelfDamagePercentPerSecond =
			CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF_DAMAGE, PERCENT_HEALTH_DAMAGE_PER_SECOND);
		mPercentDamagePercentRampingPerSecond =
			CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF_DAMAGE_RAMPING, PERCENT_HEALTH_DAMAGE_RAMPING_PER_SECOND);
		mMissingHealthFractionPerAbsorption = MISSING_HEALTH_FRACTION_PER_ABSORPTION
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MISSING_HEALTH_FRACTION_PER_ABSORPTION);
		mAbsorptionOnDeactivationAmount =
			CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_ON_DEACTIVATION_AMOUNT, ABSORPTION_ON_DEACTIVATION_AMOUNT);
		mAbsorptionOnDeactivationMax =
			CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_ON_DEACTIVATION_MAX, ABSORPTION_ON_DEACTIVATION_MAX);
		mAbsorptionOnDeactivationDuration =
			CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_ON_DEACTIVATION_DURATION, ABSORPTION_ON_DEACTIVATION_DURATION);
		mKBR = PERCENT_KNOCKBACK_RESIST
			+ CharmManager.getLevel(mPlayer, CHARM_KNOCKBACK_RESISTANCE) / 10;
		mRechargeRateBonus = BASE_RECHARGE_RATE_BONUS
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RECHARGE_RATE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TabooCS());
		mCurrentSelfDamagePercentPerTabooTick = (mSelfDamagePercentPerSecond - (mPercentDamagePercentRampingPerSecond * SELF_DAMAGE_LOSS_CORRECTION))
			/ SELF_DAMAGE_TICKS_PER_SECOND;
		mSelfDamageRampingPerTabooTick = mPercentDamagePercentRampingPerSecond / (SELF_DAMAGE_TICKS_PER_SECOND * SELF_DAMAGE_TICKS_PER_SECOND);
		mTickRateAdjustedAbsorptionLossThreshold = ABSORPTION_LOSS_THRESHOLD / SELF_DAMAGE_TICKS_PER_SECOND;
		mPercentAbsorptionLossPerTabooTick = PERCENT_ABSORPTION_LOSS_PER_SECOND / SELF_DAMAGE_TICKS_PER_SECOND;

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	private void resetVariables() {
		mActiveTimer = 0;
		mAbsorptionLossStarted = false;
		mCurrentSelfDamagePercentPerTabooTick = (mSelfDamagePercentPerSecond - (mPercentDamagePercentRampingPerSecond * SELF_DAMAGE_LOSS_CORRECTION))
			/ SELF_DAMAGE_TICKS_PER_SECOND;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		// Prevent accidentally double-casting
		int currentTick = Bukkit.getCurrentTick();
		if (currentTick - mLastCastTicks < 10) {
			return false;
		}
		mLastCastTicks = currentTick;

		if (mIsActive) {
			deactivate();
			return true;
		}

		return activate();
	}

	public boolean activate() {
		if (mAlchemistPotions == null) {
			return false;
		}

		resetVariables();
		mIsActive = true;
		mCosmetic.toggle(mPlayer, true);
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	public void deactivate() {
		putOnCooldown();
		if (isLevelTwo()) {
			double maxHealth = EntityUtils.getMaxHealth(mPlayer);
			double missingHealthFraction = Math.max(0, maxHealth - mPlayer.getHealth()) / maxHealth;
			int absorptionTicks = (int) (missingHealthFraction / mMissingHealthFractionPerAbsorption);
			double absorptionAmount = Math.min(mAbsorptionOnDeactivationMax, mAbsorptionOnDeactivationAmount * absorptionTicks);
			// Max amount set to 20 so it is compatible with other sources of absorption.
			AbsorptionUtils.addAbsorption(mPlayer, absorptionAmount, mAbsorptionOnDeactivationMax, mAbsorptionOnDeactivationDuration);
		}
		resetVariables();
		mIsActive = false;
		mCosmetic.toggle(mPlayer, false);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (!mIsActive || mAlchemistPotions == null) {
			return;
		}

		mActiveTimer += 5;
		mCosmetic.periodicEffects(mPlayer, twoHertz, oneSecond, ticks, mCurrentSelfDamagePercentPerTabooTick, mTickRateAdjustedAbsorptionLossThreshold);
		if (mActiveTimer >= SELF_DAMAGE_INTERVAL) {
			mActiveTimer -= SELF_DAMAGE_INTERVAL;
			// Health is sometimes higher than max for whatever reason, raising an exception
			double maxHealth = EntityUtils.getMaxHealth(mPlayer);
			double selfDamage = mCurrentSelfDamagePercentPerTabooTick * maxHealth;
			if (!mPlayer.isDead()) {
				mPlayer.setHealth(Math.max(0.1, Math.min(mPlayer.getHealth(), maxHealth) - selfDamage));
			}
			if (mCurrentSelfDamagePercentPerTabooTick >= mTickRateAdjustedAbsorptionLossThreshold) {
				ClientModHandler.updateAbility(mPlayer, this);
				AbsorptionUtils.subtractAbsorption(mPlayer, mPercentAbsorptionLossPerTabooTick * maxHealth);
			}
			mCurrentSelfDamagePercentPerTabooTick = Math.min(1, mCurrentSelfDamagePercentPerTabooTick + mSelfDamageRampingPerTabooTick);
			if (!mAbsorptionLossStarted && mCurrentSelfDamagePercentPerTabooTick >= mTickRateAdjustedAbsorptionLossThreshold) {
				mAbsorptionLossStarted = true;
				mCosmetic.notifyAbsorptionLossStart(mPlayer, mPlugin);
			}
		}

		mPlugin.mEffectManager.addEffect(
			mPlayer,
			KNOCKBACK_RESIST_EFFECT_NAME,
			new PercentKnockbackResist(
				6,
				mKBR,
				KNOCKBACK_RESIST_EFFECT_NAME
			).deleteOnAbilityUpdate(true).displaysTime(false)
		);
		mPlugin.mEffectManager.addEffect(
			mPlayer,
			RECHARGE_RATE_MULTIPLIER_NAME,
			new PercentPotionRecharge(
				6,
				mRechargeRateBonus,
				RECHARGE_RATE_MULTIPLIER_NAME,
				mAlchemistPotions
			).deleteOnAbilityUpdate(true).displaysTime(false)
		);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity damagee) {
		if (mIsActive && event.getType() == DamageType.MAGIC) {
			event.updateDamageWithMultiplier(1 + mMagicDamageIncrease);
		}
		return false;
	}

	@Override
	public @Nullable String getMode() {
		if (mIsActive) {
			return mAbsorptionLossStarted ? "burst" : "active";
		}
		return null;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		ClassAbility classAbility = INFO.getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), classAbility);
		TextColor color = INFO.getActionBarColor();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text("Tb", mIsActive ? color : NamedTextColor.GRAY))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}

	private static Description<Taboo> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to undergo a Taboo transformation, which lasts until deactivated. ")
			.add("While transformed, you deal ")
			.addPercent(a -> a.mMagicDamageIncrease, MAGIC_DAMAGE_INCREASE_1, false, Ability::isLevelOne)
			.add(" more magic damage, gain ")
			.addPercent(a -> a.mKBR, PERCENT_KNOCKBACK_RESIST)
			.add(" knockback resistance, and gain +")
			.addPercent(a -> a.mRechargeRateBonus, BASE_RECHARGE_RATE_BONUS)
			.add(" potion recharge rate. However, you lose ")
			.addPercent(a -> a.mSelfDamagePercentPerSecond, PERCENT_HEALTH_DAMAGE_PER_SECOND, true)
			.add(" of your health every second, which bypasses resistances and absorption, but cannot kill you. When this happens, the health loss increases by ")
			.addPercent(a -> a.mPercentDamagePercentRampingPerSecond, PERCENT_HEALTH_DAMAGE_RAMPING_PER_SECOND, true)
			.add(". If the health loss reaches ")
			.addPercent(ABSORPTION_LOSS_THRESHOLD)
			.add(", you also start losing ")
			.addPercent(PERCENT_ABSORPTION_LOSS_PER_SECOND)
			.add(" of your max health in absorption.")
			.addCooldown(COOLDOWN);
	}

	private static Description<Taboo> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The magic damage bonus is increased to ")
			.addPercent(a -> a.mMagicDamageIncrease, MAGIC_DAMAGE_INCREASE_2, false, Ability::isLevelTwo)
			.add(". Deactivating Taboo now gives you ")
			.add(a -> a.mAbsorptionOnDeactivationAmount, ABSORPTION_ON_DEACTIVATION_AMOUNT)
			.add(" absorption health (max of ")
			.add(a -> a.mAbsorptionOnDeactivationMax, ABSORPTION_ON_DEACTIVATION_MAX, false)
			.add(") for every ")
			.addPercent(a -> a.mMissingHealthFractionPerAbsorption, MISSING_HEALTH_FRACTION_PER_ABSORPTION)
			.add(" of health missing, lasting for ")
			.addDuration(a -> a.mAbsorptionOnDeactivationDuration, ABSORPTION_ON_DEACTIVATION_DURATION)
			.add("s.");
	}
}
