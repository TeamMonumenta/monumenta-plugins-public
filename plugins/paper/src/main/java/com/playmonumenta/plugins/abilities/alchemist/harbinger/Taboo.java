package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.TabooCS;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentPotionRecharge;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


public class Taboo extends Ability implements AbilityWithDuration, AbilityWithChargesOrStacks {
	private static final double PERCENT_HEALTH_DAMAGE = 0.05;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.5;
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "TabooKnockbackResistanceEffect";
	private static final double MAGIC_DAMAGE_INCREASE_1 = 0.25;
	private static final double MAGIC_DAMAGE_INCREASE_2 = 0.35;
	private static final int EFFECT_DURATION = 20 * 5;
	private static final int MAX_RECASTS_1 = 2;
	private static final int MAX_RECASTS_2 = 3;
	private static final int BASE_COOLDOWN = 20 * 5;
	private static final int EXTRA_COOLDOWN_PER_RECAST = 20 * 2;
	private static final double RECAST_COST = 0.5;
	private static final double BASE_RECHARGE_RATE_BONUS = 0.5;
	public static final String RECHARGE_RATE_MULTIPLIER_NAME = "TabooPotionRechargeRateMultiplier";

	public static final String CHARM_RECHARGE_RATE = "Taboo Recharge Rate";
	public static final String CHARM_COOLDOWN = "Taboo Cooldown";
	public static final String CHARM_SELF_DAMAGE = "Taboo Self Damage";
	public static final String CHARM_KNOCKBACK_RESISTANCE = "Taboo Knockback Resistance";
	public static final String CHARM_DAMAGE = "Taboo Damage Modifier";
	public static final String CHARM_POTION_REFUND_PER_KILL = "Taboo Potion Refund Per Kill";
	public static final String CHARM_POTION_REFUND_MAX = "Taboo Max Potion Refund";
	public static final String CHARM_EFFECT_DURATION = "Taboo Effect Duration";
	public static final String CHARM_EXTRA_COOLDOWN_PER_RECAST = "Taboo Extra Cooldown Per Recast";
	public static final String CHARM_MAX_RECASTS = "Taboo Max Recasts";
	public static final String CHARM_RECAST_COST = "Taboo Recast Cost";

	public static final AbilityInfo<Taboo> INFO =
		new AbilityInfo<>(Taboo.class, "Taboo", Taboo::new)
			.linkedSpell(ClassAbility.TABOO)
			.scoreboardId("Taboo")
			.shorthandName("Tb")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Sacrifice health in exchange for increased magic damage.")
			.cooldown(BASE_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("activate", "activate", Taboo::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
					PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.HONEY_BOTTLE);

	private final double mMagicDamageIncrease;
	private final double mHealthDamagePercent;
	private final double mKBR;
	private final int mMaxRecasts;
	private final double mRecastCost;
	private final int mCastDuration;
	private final int mExtraCooldownPerRecast;
	private final double mBaseRechargeRateBonus;

	private @Nullable AlchemistPotions mAlchemistPotions;

	private boolean mIsActive;
	private int mActiveTimer;
	private int mCurrentRecasts;
	private int mCurrentExtraCooldown;
	private double mCurrentRechargeRateBonus;
	private int mSingleCastTimer;
	private final TabooCS mCosmetic;

	public Taboo(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		resetVariables();
		mMagicDamageIncrease = (isLevelOne() ? MAGIC_DAMAGE_INCREASE_1 : MAGIC_DAMAGE_INCREASE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mHealthDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SELF_DAMAGE, PERCENT_HEALTH_DAMAGE);
		mKBR = PERCENT_KNOCKBACK_RESIST + CharmManager.getLevel(mPlayer, CHARM_KNOCKBACK_RESISTANCE) / 10;
		mCastDuration = CharmManager.getDuration(mPlayer, CHARM_EFFECT_DURATION, EFFECT_DURATION);
		mMaxRecasts = (isLevelOne() ? MAX_RECASTS_1 : MAX_RECASTS_2) + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_RECASTS);
		mRecastCost = RECAST_COST + CharmManager.getLevel(mPlayer, CHARM_RECAST_COST);
		mExtraCooldownPerRecast = CharmManager.getDuration(mPlayer, CHARM_EXTRA_COOLDOWN_PER_RECAST, EXTRA_COOLDOWN_PER_RECAST);
		mBaseRechargeRateBonus = BASE_RECHARGE_RATE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RECHARGE_RATE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TabooCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	private void resetVariables() {
		mIsActive = false;
		mActiveTimer = 0;
		mCurrentRecasts = 0;
		mCurrentExtraCooldown = 0;
		mCurrentRechargeRateBonus = 0;
		mSingleCastTimer = 0;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		return activate();
	}

	public boolean activate() {
		if (mAlchemistPotions == null) {
			return false;
		}

		if (mIsActive) {
			// Re-cast
			if (mCurrentRecasts < mMaxRecasts && mAlchemistPotions.decrementCharges(mRecastCost)) {
				// Recast valid
				mCurrentRecasts++;
				mActiveTimer += mCastDuration;
				mCurrentExtraCooldown += mExtraCooldownPerRecast;
				mCosmetic.toggle(mPlayer, true);
				ClientModHandler.updateAbility(mPlayer, this);
				return true;
			}
		} else {
			// Initial Cast
			if (mAlchemistPotions.decrementCharge()) {
				mIsActive = true;
				mActiveTimer += mCastDuration;
				mCurrentRechargeRateBonus = mBaseRechargeRateBonus;
				updatePotionRechargeEffect();
				mCosmetic.toggle(mPlayer, true);
				ClientModHandler.updateAbility(mPlayer, this);
				return true;
			}
		}
		return false;
	}

	private void updatePotionRechargeEffect() {
		mPlugin.mEffectManager.clearEffects(mPlayer, RECHARGE_RATE_MULTIPLIER_NAME);
		if (mAlchemistPotions != null) {
			mPlugin.mEffectManager.addEffect(
				mPlayer,
				RECHARGE_RATE_MULTIPLIER_NAME,
				new PercentPotionRecharge(
					mCastDuration,
					mCurrentRechargeRateBonus,
					RECHARGE_RATE_MULTIPLIER_NAME,
					mAlchemistPotions
				).deleteOnAbilityUpdate(true).displaysTime(false)
			);
		}
	}

	public void deactivate() {
		if (mAlchemistPotions != null) {
			mAlchemistPotions.removeRechargeRateMultiplier(RECHARGE_RATE_MULTIPLIER_NAME);
		}

		putOnCooldown(getModifiedCooldown() + mCurrentExtraCooldown);
		resetVariables();
		mCosmetic.toggle(mPlayer, false);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (!mIsActive) {
			return;
		}

		mActiveTimer -= 5;
		mSingleCastTimer += 5;
		mCosmetic.periodicEffects(mPlayer, twoHertz, oneSecond, ticks, false);

		if (oneSecond) {
			double maxHealth = EntityUtils.getMaxHealth(mPlayer);
			double selfDamage = mHealthDamagePercent * maxHealth;
			if (mPlayer.getHealth() > selfDamage) {
				mPlayer.setHealth(Math.min(mPlayer.getHealth(), maxHealth) - selfDamage); // Health is sometimes higher than max for whatever reason, raising an exception
				mPlayer.damage(0);
			}
			mPlugin.mEffectManager.addEffect(
				mPlayer,
				KNOCKBACK_RESIST_EFFECT_NAME,
				new PercentKnockbackResist(
					20,
					mKBR,
					KNOCKBACK_RESIST_EFFECT_NAME
				)
					.displaysTime(false)
					.deleteOnAbilityUpdate(true)
			);
		}

		if (mActiveTimer <= 0) {
			deactivate();
			return;
		}

		if (mSingleCastTimer >= mCastDuration) {
			if (mAlchemistPotions != null) {
				updatePotionRechargeEffect();
			}
			mSingleCastTimer = 0;
		}
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
		return mIsActive ? "active" : null;
	}

	@Override
	public int getInitialAbilityDuration() {
		if (!mIsActive) {
			return 0;
		}
		return mCastDuration * (mCurrentRecasts + 1);
	}

	@Override
	public int getRemainingAbilityDuration() {
		if (!mIsActive) {
			return 0;
		}
		return mActiveTimer;
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

		if (mIsActive) {
			// Apparently getRemainingAbilityDuration() doesn't actually work
			output = output.append(Component.text((mActiveTimer / Constants.TICKS_PER_SECOND) + "s", NamedTextColor.GOLD));
		} else if (remainingCooldown > 0) {
			output = output.append(Component.text(((int) Math.ceil(remainingCooldown / 20.0)) + "s", NamedTextColor.GRAY));
		} else {
			output = output.append(Component.text("%s/%s".formatted(getCharges(), getMaxCharges()), NamedTextColor.GREEN));
		}

		return output;
	}

	private static Description<Taboo> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to consume a potion and undergo a Taboo transformation, which lasts for ")
			.addDuration(a -> a.mCastDuration, EFFECT_DURATION)
			.add("s. While transformed, you deal ")
			.addPercent(a -> a.mMagicDamageIncrease, MAGIC_DAMAGE_INCREASE_1, false, Ability::isLevelOne)
			.add(" more magic damage, gain ")
			.addPercent(a -> a.mKBR, PERCENT_KNOCKBACK_RESIST)
			.add(" knockback resistance, and gain +")
			.addPercent(a -> a.mBaseRechargeRateBonus, BASE_RECHARGE_RATE_BONUS)
			.add(" potion recharge rate. However, you lose ")
			.addPercent(a -> a.mHealthDamagePercent, PERCENT_HEALTH_DAMAGE, true)
			.add(" of your health per second, which bypasses resistances and absorption, but cannot kill you.")
			.add(" While Taboo is active, you can recast it at the cost of ")
			.add(a -> a.mRecastCost, RECAST_COST, true)
			.add(" potions to extend the duration by another ")
			.addDuration(a -> a.mCastDuration, EFFECT_DURATION)
			.add("s, up to a maximum of ")
			.add(a -> a.mMaxRecasts, MAX_RECASTS_1, false, Ability::isLevelOne)
			.add(" times. Each time you do this, the cooldown is increased by ")
			.addDuration(a -> a.mExtraCooldownPerRecast, EXTRA_COOLDOWN_PER_RECAST, true)
			.add("s.")
			.addCooldown(BASE_COOLDOWN);
	}

	private static Description<Taboo> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The magic damage bonus is increased to ")
			.addPercent(a -> a.mMagicDamageIncrease, MAGIC_DAMAGE_INCREASE_2, false, Ability::isLevelTwo)
			.add(", and you can now extend the timer up to a maximum of ")
			.add(a -> a.mMaxRecasts, MAX_RECASTS_2, false, Ability::isLevelTwo)
			.add(" times.");
	}

	@Override
	public int getCharges() {
		return mMaxRecasts + 1 - mCurrentRecasts - (mIsActive ? 1 : 0);
	}

	@Override
	public int getMaxCharges() {
		return mMaxRecasts + 1;
	}
}
