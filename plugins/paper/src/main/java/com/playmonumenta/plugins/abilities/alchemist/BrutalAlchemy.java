package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class BrutalAlchemy extends Ability implements PotionAbility {
	private static final int BRUTAL_ALCHEMY_DURATION = 8 * 20;
	private static final int BRUTAL_ALCHEMY_PERIOD = 1 * 20;
	private static final double BRUTAL_ALCHEMY_1_DOT_MULTIPLIER = 0.20;
	private static final double BRUTAL_ALCHEMY_2_DOT_MULTIPLIER = 0.35;
	private static final double BRUTAL_ALCHEMY_ENHANCED_DOT_MULTIPLIER = 0.20;
	private static final String BRUTAL_ALCHEMY_DOT_EFFECT_NAME = "BrutalAlchemyDamageOverTimeEffect";
	private static final String BRUTAL_ALCHEMY_DOT_ENHANCED_EFFECT_NAME = "BrutalAlchemyDamageOverTimeEnhancedEffect";

	public static final String CHARM_POTION_DAMAGE = "Brutal Alchemy Potion Damage";
	public static final String CHARM_DOT_DAMAGE = "Brutal Alchemy DoT Damage";
	public static final String CHARM_DURATION = "Brutal Alchemy Duration";
	public static final String CHARM_RADIUS = "Brutal Alchemy Spread Radius";
	public static final String CHARM_MULTIPLIER = "Brutal Alchemy Potion Damage Multiplier";

	public static final AbilityInfo<BrutalAlchemy> INFO =
		new AbilityInfo<>(BrutalAlchemy.class, "Brutal Alchemy", BrutalAlchemy::new)
			.linkedSpell(ClassAbility.BRUTAL_ALCHEMY)
			.scoreboardId("BrutalAlchemy")
			.shorthandName("BA")
			.descriptions(
				("Your Brutal Alchemist's Potions now apply a Damage Over Time effect which does %s%% base damage " +
				"as magic damage every second, over %ss.")
					.formatted(
							StringUtils.multiplierToPercentage(BRUTAL_ALCHEMY_1_DOT_MULTIPLIER),
							StringUtils.ticksToSeconds(BRUTAL_ALCHEMY_DURATION)
					),
				"The Damage Over Time effect now does %s%% base damage as magic damage."
					.formatted(StringUtils.multiplierToPercentage(BRUTAL_ALCHEMY_2_DOT_MULTIPLIER)),
				("Your Brutal Alchemist's Potions now apply a second Damage Over Time effect which does %s%% base damage " +
				"as magic damage every %s seconds, over %s seconds.")
					.formatted(
							StringUtils.multiplierToPercentage(BRUTAL_ALCHEMY_ENHANCED_DOT_MULTIPLIER),
							StringUtils.ticksToSeconds(BRUTAL_ALCHEMY_PERIOD * 2),
							StringUtils.ticksToSeconds(BRUTAL_ALCHEMY_DURATION)
					)
			)
			.simpleDescription("Apply a magic damage over time effect to mobs hit by your brutal potions.")
			.displayItem(Material.REDSTONE);

	private final int mPeriod;
	private @MonotonicNonNull AlchemistPotions mAlchemistPotions;

	public BrutalAlchemy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPeriod = BRUTAL_ALCHEMY_PERIOD;

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!isGruesome && mAlchemistPotions != null) {
			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, BRUTAL_ALCHEMY_DURATION);

			double baseDamage = mAlchemistPotions.getDamage(playerItemStats);
			double finalDamage = baseDamage * (isLevelOne() ? BRUTAL_ALCHEMY_1_DOT_MULTIPLIER : BRUTAL_ALCHEMY_2_DOT_MULTIPLIER);
			double extraDamage = CharmManager.getLevel(mPlayer, CHARM_DOT_DAMAGE);

			CustomDamageOverTime dot = new CustomDamageOverTime(duration, finalDamage + extraDamage, mPeriod, mPlayer, mInfo.getLinkedSpell(), DamageEvent.DamageType.MAGIC);
			dot.setVisuals(mAlchemistPotions.mCosmetic::damageOverTimeEffects);
			mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, dot);

			if (isEnhanced()) {
				double finalEnhancedDamage = baseDamage * BRUTAL_ALCHEMY_ENHANCED_DOT_MULTIPLIER;
				// Apply the enhanced dot a little later for a cool effect
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					CustomDamageOverTime enhancementDot = new CustomDamageOverTime(duration, finalEnhancedDamage + extraDamage, mPeriod * 2, mPlayer, mInfo.getLinkedSpell(), DamageEvent.DamageType.MAGIC);
					enhancementDot.setVisuals(mAlchemistPotions.mCosmetic::damageOverTimeEffects);
					mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_ENHANCED_EFFECT_NAME, enhancementDot);
				}, 10);
			}
		}
	}

}
