package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BrutalAlchemy extends Ability implements PotionAbility {
	private static final int BRUTAL_ALCHEMY_DURATION = 8 * 20;
	private static final int BRUTAL_ALCHEMY_PERIOD = 20;
	private static final double BRUTAL_ALCHEMY_1_DOT_MULTIPLIER = 0.20;
	private static final double BRUTAL_ALCHEMY_2_DOT_MULTIPLIER = 0.35;
	private static final double BRUTAL_ALCHEMY_ENHANCED_DOT_MULTIPLIER = 0.20;
	private static final String BRUTAL_ALCHEMY_DOT_EFFECT_NAME = "BrutalAlchemyDamageOverTimeEffect";
	private static final String BRUTAL_ALCHEMY_DOT_ENHANCED_EFFECT_NAME = "BrutalAlchemyDamageOverTimeEnhancedEffect";

	public static final String CHARM_DAMAGE_MULTIPLIER = "Brutal Alchemy Damage Multiplier";
	public static final String CHARM_DOT_DAMAGE = "Brutal Alchemy DoT Damage";
	public static final String CHARM_DURATION = "Brutal Alchemy Duration";

	public static final AbilityInfo<BrutalAlchemy> INFO =
		new AbilityInfo<>(BrutalAlchemy.class, "Brutal Alchemy", BrutalAlchemy::new)
			.linkedSpell(ClassAbility.BRUTAL_ALCHEMY)
			.scoreboardId("BrutalAlchemy")
			.shorthandName("BA")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Apply a magic damage over time effect to mobs hit by your brutal potions.")
			.displayItem(Material.REDSTONE);

	private final int mDuration;
	private final double mDoTMultiplier;
	private final double mEnhancementMultiplier;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public BrutalAlchemy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, BRUTAL_ALCHEMY_DURATION);
		mDoTMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_DAMAGE, isLevelOne() ? BRUTAL_ALCHEMY_1_DOT_MULTIPLIER : BRUTAL_ALCHEMY_2_DOT_MULTIPLIER);
		mEnhancementMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_DAMAGE, BRUTAL_ALCHEMY_ENHANCED_DOT_MULTIPLIER);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!isGruesome && mAlchemistPotions != null) {

			double baseDamage = mAlchemistPotions.getDamage(playerItemStats);
			double damage = baseDamage * mDoTMultiplier;
			CustomDamageOverTime dot = new CustomDamageOverTime(mDuration, Math.max(1, damage), BRUTAL_ALCHEMY_PERIOD, mPlayer, playerItemStats, mInfo.getLinkedSpell(), DamageEvent.DamageType.MAGIC);
			dot.setVisuals(mAlchemistPotions.mCosmetic::damageOverTimeEffects);
			mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, dot);

			if (isEnhanced()) {
				double finalEnhancedDamage = baseDamage * mEnhancementMultiplier;
				// Apply the enhanced dot a little later for a cool effect
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					CustomDamageOverTime enhancementDot = new CustomDamageOverTime(mDuration, Math.max(1, finalEnhancedDamage), BRUTAL_ALCHEMY_PERIOD * 2, mPlayer, playerItemStats, mInfo.getLinkedSpell(), DamageEvent.DamageType.MAGIC);
					if (mAlchemistPotions != null) {
						enhancementDot.setVisuals(mAlchemistPotions.mCosmetic::damageOverTimeEffects);
					}
					mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_ENHANCED_EFFECT_NAME, enhancementDot);
				}, 10);
			}
		}
	}

	private static Description<BrutalAlchemy> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your Brutal Alchemist's Potions now apply a Damage Over Time effect which does ")
			.addPercent(a -> a.mDoTMultiplier, BRUTAL_ALCHEMY_1_DOT_MULTIPLIER, false, Ability::isLevelOne)
			.add(" base damage as magic damage every second, over ")
			.addDuration(a -> a.mDuration, BRUTAL_ALCHEMY_DURATION)
			.add(" seconds. The damage dealt by this effect cannot be less than 1.");
	}

	private static Description<BrutalAlchemy> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The Damage Over Time effect now does ")
			.addPercent(a -> a.mDoTMultiplier, BRUTAL_ALCHEMY_2_DOT_MULTIPLIER, false, Ability::isLevelTwo)
			.add(" base damage as magic damage.");
	}

	private static Description<BrutalAlchemy> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your Brutal Alchemist's Potions now apply a second Damage Over Time effect which does ")
			.addPercent(a -> a.mEnhancementMultiplier, BRUTAL_ALCHEMY_ENHANCED_DOT_MULTIPLIER)
			.add(" base damage as magic damage every ")
			.addDuration(BRUTAL_ALCHEMY_PERIOD * 2)
			.add(" seconds, over ")
			.addDuration(a -> a.mDuration, BRUTAL_ALCHEMY_DURATION)
			.add(" seconds.");
	}
}
