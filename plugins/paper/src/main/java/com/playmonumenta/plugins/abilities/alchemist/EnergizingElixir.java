package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EnergizingElixirStacks;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class EnergizingElixir extends Ability implements AbilityWithChargesOrStacks {

	private static final int COOLDOWN = 2 * 20;
	private static final int DURATION = 6 * 20;
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
			.descriptions(
				("Left click while holding an Alchemist's Bag to consume a potion to apply %s%% Speed and Jump Boost %s " +
				"to yourself for %ss. Cooldown: %ss.")
					.formatted(
						StringUtils.multiplierToPercentage(SPEED_AMPLIFIER_1),
						JUMP_LEVEL + 1,
						StringUtils.ticksToSeconds(DURATION),
						StringUtils.ticksToSeconds(COOLDOWN)
					),
				"Speed is increased to %s%%; additionally, gain a %s%% damage buff from all sources for the same duration."
					.formatted(
						StringUtils.multiplierToPercentage(SPEED_AMPLIFIER_2),
						StringUtils.multiplierToPercentage(DAMAGE_AMPLIFIER_2)
					),
				("Recasting this ability while the buff is still active refreshes the duration and increases " +
				"the damage bonus and speed by %s%%, up to %s stacks. Stacks decay every %ss.")
					.formatted(
						StringUtils.multiplierToPercentage(ENHANCED_BONUS),
						ENHANCED_MAX_STACK,
						StringUtils.ticksToSeconds(DURATION)
					)
			)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast",
				EnergizingElixir::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggleRecast", "toggle automatic recast",
				"Automatically keeps the buff up at a cost to potion recharge rate " + StringUtils.multiplierToPercentage(AUTO_RECAST_COST_INCREASE) + "% higher than what optimal manual casts would use.",
				EnergizingElixir::toggleRecast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.DOWN).enabled(false), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggleJumpBoost", "toggle jump boost",
				EnergizingElixir::toggleJumpBoost, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP).enabled(false), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(new ItemStack(Material.RABBIT_FOOT, 1));

	private final double mSpeedAmp;
	private final int mDuration;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mStacks;
	private final int mMaxStacks;
	private final int mPrice;
	private final int mJumpBoostAmplifier;

	public EnergizingElixir(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSpeedAmp = (isLevelOne() ? SPEED_AMPLIFIER_1 : SPEED_AMPLIFIER_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mStacks = 0;
		mMaxStacks = isEnhanced() ? ENHANCED_MAX_STACK + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS) : 0;
		mPrice = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_PRICE);
		mJumpBoostAmplifier = JUMP_LEVEL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		if (MetadataUtils.happenedThisTick(mPlayer, AlchemistPotions.METADATA_KEY, -1)) {
			//this may be strange but sometime for some player, when they throw an Alch pot, elixir is randomly cast
			//Stopping elixir since was caused by potion
			return;
		}
		if (mAlchemistPotions == null || !mAlchemistPotions.decrementCharges(mPrice)) {
			// If no charges, do not activate ability
			return;
		}

		activate(true);

		putOnCooldown();
	}

	private void activate(boolean soundAndParticles) {

		if (isEnhanced()) {
			if (mPlugin.mEffectManager.hasEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME)) {
				mStacks = Math.min(mMaxStacks, mStacks + 1);
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCED_STACKS_NAME, new EnergizingElixirStacks(mDuration, mStacks));
			}
		}

		applyEffects();

		if (soundAndParticles) {
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			new PartialParticle(Particle.TOTEM, loc, 50, 1.5, 1, 1.5, 0).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1, 0);
		}
	}

	private void toggleRecast() {
		// Toggling off is always possible. Toggling on casts the ability once, so requires being off cooldown and having enough potions to use it.
		if (mPlayer.getScoreboardTags().remove(TOGGLE_TAG)) {
			mPlayer.sendActionBar(Component.text("Energizing Elixir automatic recast has been disabled"));
			ClientModHandler.updateAbility(mPlayer, this);
		} else if (!isOnCooldown() && mAlchemistPotions != null && mAlchemistPotions.decrementCharges(mPrice)) {
			mPlayer.sendActionBar(Component.text("Energizing Elixir automatic recast has been enabled"));
			mPlayer.getScoreboardTags().add(TOGGLE_TAG);
			activate(true);
			putOnCooldown();
		}
	}

	private void applyEffects() {
		int duration = mDuration;
		if (mStacks > 1) {
			duration += 10; // to prevent gaps
		}
		double bonus = mStacks * (ENHANCED_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS));
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(duration, mSpeedAmp + bonus, PERCENT_SPEED_EFFECT_NAME));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, duration, mJumpBoostAmplifier,
			true, false, !mPlayer.getScoreboardTags().contains(TOGGLE_TAG)));
		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(duration, DAMAGE_AMPLIFIER_2 + bonus + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE)));
		}
	}

	private void toggleJumpBoost() {
		if (ScoreboardUtils.toggleTag(mPlayer, DISABLE_JUMP_BOOST_TAG)) {
			// jump boost disabled: remove jump boost if active
			mPlayer.sendActionBar(Component.text("Energizing Elixir's Jump Boost has been disabled"));
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP, mJumpBoostAmplifier);
		} else {
			// jump boost enabled: apply jump boost if elixir is currently active
			mPlayer.sendActionBar(Component.text("Energizing Elixir's Jump Boost has been enabled"));
			Effect activeEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME);
			if (activeEffect != null) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, activeEffect.getDuration(), mJumpBoostAmplifier,
					true, false, !mPlayer.getScoreboardTags().contains(TOGGLE_TAG)));
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		boolean toggled = mPlayer.getScoreboardTags().contains(TOGGLE_TAG);
		if (toggled && mAlchemistPotions != null) {
			// Toggled potion cost is implemented by periodically increasing potion recharge time,
			// as a constant modifier of the recharge rate results in different costs depending on the current value of that rate.
			double potionTimer = (1 + AUTO_RECAST_COST_INCREASE) * 5 * 20 * mPrice / mDuration;
			mAlchemistPotions.modifyCurrentPotionTimer(-potionTimer);
		}
		if (toggled && Arrays.stream(mPlayer.getInventory().getContents()).limit(9).noneMatch(ItemUtils::isAlchemistItem)) {
			toggled = false;
		}
		if (mStacks > 0 && !mPlugin.mEffectManager.hasEffect(mPlayer, EnergizingElixirStacks.class)) {
			if (toggled) {
				mStacks = Math.min(mMaxStacks, mStacks + 1);
			} else {
				mStacks--;
			}
			if (mStacks > 0) {
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCED_STACKS_NAME, new EnergizingElixirStacks(mDuration, mStacks));
			}
			applyEffects();
			ClientModHandler.updateAbility(mPlayer, this);
		} else if (toggled) {
			Effect activeEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME);
			if (activeEffect == null || activeEffect.getDuration() <= 5) {
				activate(false);
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

}
