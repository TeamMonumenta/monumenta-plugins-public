package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.EnergizingElixirCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EnergizingElixirStacks;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class EnergizingElixir extends Ability implements AbilityWithChargesOrStacks {
	private static final int COOLDOWN = TICKS_PER_SECOND * 2;
	private static final int DURATION = TICKS_PER_SECOND * 6;
	private static final double SPEED_AMPLIFIER_1 = 0.1;
	private static final double SPEED_AMPLIFIER_2 = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EnergizingElixirPercentSpeedEffect";
	private static final int JUMP_LEVEL = 1;
	private static final double DAMAGE_AMPLIFIER_2 = 0.1;
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "EnergizingElixirPercentDamageEffect";
	private static final double AUTO_RECAST_COST_INCREASE = 0.2;

	private static final String ENHANCED_STACKS_NAME = EnergizingElixirStacks.GENERIC_NAME;
	private static final double ENHANCED_BONUS = 0.03;
	private static final int ENHANCED_MAX_STACK = 4;

	private static final String TOGGLE_TAG = "EnergizingElixirRecast";
	private static final String DISABLE_JUMP_BOOST_TAG = "EnergizingElixirNoJumpBoost";

	public static final String CHARM_DURATION = "Energizing Elixir Effect Duration";
	public static final String CHARM_SPEED = "Energizing Elixir Speed Modifier";
	public static final String CHARM_JUMP_BOOST = "Energizing Elixir Jump Boost Modifier";
	public static final String CHARM_DAMAGE = "Energizing Elixir Damage Modifier";
	public static final String CHARM_BONUS = "Energizing Elixir Bonus Per Stack";
	public static final String CHARM_STACKS = "Energizing Elixir Max Stacks";
	public static final String CHARM_PRICE = "Energizing Elixir Potion Price";

	public static final AbilityInfo<EnergizingElixir> INFO =
		new AbilityInfo<>(EnergizingElixir.class, "Energizing Elixir", EnergizingElixir::new)
			.linkedSpell(ClassAbility.ENERGIZING_ELIXIR)
			.scoreboardId("EnergizingElixir")
			.shorthandName("EE")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Consume potions to give yourself mobility and damage buffs.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast",
				EnergizingElixir::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggleRecast", "toggle automatic recast",
				"Automatically keeps the buffs active at a " + StringUtils.multiplierToPercentageWithSign(AUTO_RECAST_COST_INCREASE) + " higher cost to potion recharge rate compared to optimal manual casting.",
				EnergizingElixir::toggleRecast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN).enabled(false), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggleJumpBoost", "toggle jump boost",
				EnergizingElixir::toggleJumpBoost, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP).enabled(false), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.RABBIT_FOOT);

	private final double mSpeedAmp;
	private final double mDamageAmp;
	private final int mDuration;
	private final double mEnhanceEffectBonus;
	private final int mMaxStacks;
	private final int mPrice;
	private final int mJumpBoostAmplifier;
	private final EnergizingElixirCS mCosmetic;

	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mStacks;

	public EnergizingElixir(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSpeedAmp = (isLevelOne() ? SPEED_AMPLIFIER_1 : SPEED_AMPLIFIER_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDamageAmp = DAMAGE_AMPLIFIER_2 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mEnhanceEffectBonus = ENHANCED_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS);
		mStacks = 0;
		mMaxStacks = isEnhanced() ? ENHANCED_MAX_STACK + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS) : 0;
		mPrice = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_PRICE);
		mJumpBoostAmplifier = JUMP_LEVEL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new EnergizingElixirCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mAlchemistPotions = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		if (MetadataUtils.happenedThisTick(mPlayer, AlchemistPotions.METADATA_KEY, -1)) {
			//this may be strange but sometime for some player, when they throw an Alch pot, elixir is randomly cast
			//Stopping elixir since was caused by potion
			return false;
		}
		if (mAlchemistPotions == null || !mAlchemistPotions.decrementCharges(mPrice)) {
			// If no charges, do not activate ability
			return false;
		}

		activate(true, true);

		putOnCooldown();
		return true;
	}

	private void activate(final boolean manualCast, final boolean showParticles) {
		if (isEnhanced()) {
			if (mPlugin.mEffectManager.hasEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME)) {
				mStacks = Math.min(mMaxStacks, mStacks + 1);
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCED_STACKS_NAME,
					new EnergizingElixirStacks(mDuration, mStacks).deleteOnAbilityUpdate(true));
			}
		}

		applyEffects();

		if (showParticles) {
			mCosmetic.activate(mPlayer, mStacks, manualCast);
		}
	}

	private boolean toggleRecast() {
		// Toggling off is always possible. Toggling on casts the ability once, so requires being off cooldown and having enough potions to use it.
		if (mPlayer.getScoreboardTags().remove(TOGGLE_TAG)) {
			mPlayer.sendActionBar(Component.text("Energizing Elixir automatic recast has been disabled"));
			ClientModHandler.updateAbility(mPlayer, this);
			mCosmetic.toggleRecastOff(mPlayer);
		} else if (!isOnCooldown() && mAlchemistPotions != null && mAlchemistPotions.decrementCharges(mPrice)) {
			mPlayer.sendActionBar(Component.text("Energizing Elixir automatic recast has been enabled"));
			mPlayer.getScoreboardTags().add(TOGGLE_TAG);
			mCosmetic.toggleRecastOn(mPlayer);
			activate(true, true);
			putOnCooldown();
		}
		return true;
	}

	private void applyEffects() {
		int duration = mDuration;
		if (mStacks > 1) {
			duration += 10; // to prevent gaps
		}

		final double effectAmpBonus = mStacks * mEnhanceEffectBonus;
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
			new PercentSpeed(duration, mSpeedAmp + effectAmpBonus, PERCENT_SPEED_EFFECT_NAME)
				.deleteOnAbilityUpdate(true));

		if (!mPlayer.getScoreboardTags().contains(DISABLE_JUMP_BOOST_TAG)) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.JUMP, duration, mJumpBoostAmplifier, true, false,
					!mPlayer.getScoreboardTags().contains(TOGGLE_TAG)));
		}

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_EFFECT_NAME,
				new PercentDamageDealt(duration, mDamageAmp + effectAmpBonus).deleteOnAbilityUpdate(true));
		}
	}

	private boolean toggleJumpBoost() {
		if (ScoreboardUtils.toggleTag(mPlayer, DISABLE_JUMP_BOOST_TAG)) {
			// jump boost disabled: remove jump boost if active
			mPlayer.sendActionBar(Component.text("Energizing Elixir's Jump Boost has been disabled"));
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP, mJumpBoostAmplifier);
		} else {
			// jump boost enabled: apply jump boost if elixir is currently active
			mPlayer.sendActionBar(Component.text("Energizing Elixir's Jump Boost has been enabled"));
			final Effect activeEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME);
			if (activeEffect != null) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, activeEffect.getDuration(), mJumpBoostAmplifier,
					true, false, !mPlayer.getScoreboardTags().contains(TOGGLE_TAG)));
			}
		}
		return true;
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		boolean toggled = mPlayer.getScoreboardTags().contains(TOGGLE_TAG);
		if (toggled && mAlchemistPotions != null) {
			// Toggled potion cost is implemented by periodically increasing potion recharge time,
			// as a constant modifier of the recharge rate results in different costs depending on the current value of that rate.
			final double potionTimer = (1 + AUTO_RECAST_COST_INCREASE) * 5 * TICKS_PER_SECOND * mPrice / mDuration;
			mAlchemistPotions.modifyCurrentPotionTimer(-potionTimer);
		}
		if (toggled && Arrays.stream(mPlayer.getInventory().getContents()).limit(9).noneMatch(ItemUtils::isAlchemistItem)) {
			toggled = false;
		}
		if (mStacks > 0 && !mPlugin.mEffectManager.hasEffect(mPlayer, EnergizingElixirStacks.class)) {
			if (toggled) {
				if (mStacks < mMaxStacks) {
					mStacks = Math.min(mMaxStacks, mStacks + 1);
					mCosmetic.activate(mPlayer, mStacks, false);
				}
			} else {
				mStacks--;
				mCosmetic.stackDecayEffect(mPlayer, mStacks);
			}
			if (mStacks > 0) {
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCED_STACKS_NAME,
					new EnergizingElixirStacks(mDuration, mStacks).deleteOnAbilityUpdate(true));
			}
			applyEffects();
			ClientModHandler.updateAbility(mPlayer, this);
		} else if (toggled) {
			final Effect activeEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME);
			if (activeEffect == null || activeEffect.getDuration() <= 5) {
				activate(false, activeEffect == null);
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

	@Override
	public @Nullable String getMode() {
		return mPlayer.getScoreboardTags().contains(TOGGLE_TAG) ? "active" : null;
	}

	private static Description<EnergizingElixir> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger(0)
			.add(" to consume a potion and gain ")
			.addPercent(a -> a.mSpeedAmp, SPEED_AMPLIFIER_1, false, Ability::isLevelOne)
			.add(" speed and Jump Boost ")
			.addPotionAmplifier(a -> a.mJumpBoostAmplifier, JUMP_LEVEL)
			.add(" for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<EnergizingElixir> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The speed is increased to ")
			.addPercent(a -> a.mSpeedAmp, SPEED_AMPLIFIER_2, false, Ability::isLevelTwo)
			.add(" and gain ")
			.addPercent(a -> a.mDamageAmp, DAMAGE_AMPLIFIER_2, false, Ability::isLevelTwo)
			.add(" damage for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.");
	}

	private static Description<EnergizingElixir> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Recasting this ability while the buff is active refreshes the duration and increases the damage and speed by ")
			.addPercent(a -> a.mEnhanceEffectBonus, ENHANCED_BONUS)
			.add(", up to ")
			.add(a -> a.mMaxStacks, ENHANCED_MAX_STACK, false, Ability::isEnhanced)
			.add(" stacks. Stacks decay every ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.");
	}
}
