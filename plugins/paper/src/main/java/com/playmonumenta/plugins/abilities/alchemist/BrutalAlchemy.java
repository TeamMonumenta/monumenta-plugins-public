package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.SpreadEffectOnDeath;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BrutalAlchemy extends PotionAbility {
	private static final int BRUTAL_ALCHEMY_1_DAMAGE = 1;
	private static final int BRUTAL_ALCHEMY_2_DAMAGE = 2;
	private static final int BRUTAL_ALCHEMY_DURATION = 8 * 20;
	private static final double BRUTAL_ALCHEMY_DOT_DAMAGE = 1;
	private static final int BRUTAL_ALCHEMY_1_PERIOD = 2 * 20;
	private static final int BRUTAL_ALCHEMY_2_PERIOD = 1 * 20;
	private static final String BRUTAL_ALCHEMY_DOT_EFFECT_NAME = "BrutalAlchemyDamageOverTimeEffect";
	private static final String BRUTAL_ALCHEMY_SPREAD_EFFECT_NAME = "BrutalAlchemySpreadEffect";
	private static final double BRUTAL_ALCHEMY_ENHANCEMENT_DAMAGE_POTION = 0.2;
	private static final double BRUTAL_ALCHEMY_ENHANCEMENT_RANGE = 3;

	public static final String CHARM_POTION_DAMAGE = "Brutal Alchemy Potion Damage";
	public static final String CHARM_DOT_DAMAGE = "Brutal Alchemy DoT Damage";
	public static final String CHARM_DURATION = "Brutal Alchemy Duration";
	public static final String CHARM_RADIUS = "Brutal Alchemy Spread Radius";
	public static final String CHARM_MULTIPLIER = "Brutal Alchemy Potion Damage Multiplier";

	private int mPeriod;
	private double mDOTDamage;

	public BrutalAlchemy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Brutal Alchemy", CharmManager.calculateFlatAndPercentValue(player, CHARM_POTION_DAMAGE, BRUTAL_ALCHEMY_1_DAMAGE), CharmManager.calculateFlatAndPercentValue(player, CHARM_POTION_DAMAGE, BRUTAL_ALCHEMY_2_DAMAGE));
		mInfo.mLinkedSpell = ClassAbility.BRUTAL_ALCHEMY;
		mInfo.mScoreboardId = "BrutalAlchemy";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions deal +1 damage and apply 1 damage every 2 seconds for 8 seconds.");
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions now deal +2 damage and apply 1 damage every second instead.");
		mInfo.mDescriptions.add("The damage over time is increased by 20% of your base potion damage, when you kill a mob that had the Brutal Alchemy effect on them, the damage over time spreads to all mobs in a 3 block radius around it.");
		mDisplayItem = new ItemStack(Material.REDSTONE, 1);

		mPeriod = isLevelOne() ? BRUTAL_ALCHEMY_1_PERIOD : BRUTAL_ALCHEMY_2_PERIOD;
		mDOTDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_DAMAGE, BRUTAL_ALCHEMY_DOT_DAMAGE);

		if (isEnhanced()) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				AlchemistPotions alchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
				if (alchemistPotions != null) {
					mDOTDamage += alchemistPotions.getDamage() * (BRUTAL_ALCHEMY_ENHANCEMENT_DAMAGE_POTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MULTIPLIER));
				}
			}, 5);
		}
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome) {
		if (!isGruesome) {
			int duration = BRUTAL_ALCHEMY_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
			mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, new CustomDamageOverTime(duration, mDOTDamage, mPeriod, mPlayer, mInfo.mLinkedSpell, Particle.SQUID_INK));
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_SPREAD_EFFECT_NAME, new SpreadEffectOnDeath(duration, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, CharmManager.getRadius(mPlayer, CHARM_RADIUS, BRUTAL_ALCHEMY_ENHANCEMENT_RANGE), duration));
			}
		}
	}

}
