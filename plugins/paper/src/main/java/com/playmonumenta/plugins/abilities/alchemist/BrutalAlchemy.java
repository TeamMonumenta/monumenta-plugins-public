package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.SpreadEffectOnDeath;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
	private static final double BRUTAL_ALCHEMY_ENHANCEMENT_DAMAGE_POTION = 0.3;
	private static final double BRUTAL_ALCHEMY_ENHANCEMENT_RANGE = 3;

	public static final String CHARM_POTION_DAMAGE = "Brutal Alchemy Potion Damage";
	public static final String CHARM_DOT_DAMAGE = "Brutal Alchemy DoT Damage";
	public static final String CHARM_DURATION = "Brutal Alchemy Duration";
	public static final String CHARM_RADIUS = "Brutal Alchemy Spread Radius";
	public static final String CHARM_MULTIPLIER = "Brutal Alchemy Potion Damage Multiplier";

	private final int mPeriod;
	private final double mDOTDamage;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public BrutalAlchemy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Brutal Alchemy", CharmManager.calculateFlatAndPercentValue(player, CHARM_POTION_DAMAGE, BRUTAL_ALCHEMY_1_DAMAGE), CharmManager.calculateFlatAndPercentValue(player, CHARM_POTION_DAMAGE, BRUTAL_ALCHEMY_2_DAMAGE));
		mInfo.mLinkedSpell = ClassAbility.BRUTAL_ALCHEMY;
		mInfo.mScoreboardId = "BrutalAlchemy";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions deal +1 damage and apply 1 damage every 2 seconds for 8 seconds.");
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions now deal +2 damage and apply 1 damage every second instead.");
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions damage over time effect is increased by 30% of your base potion damage. Additionally, when a mob inflicted with this damage over time effect dies, the effect spreads to all mobs in a 3 block radius around it.");
		mDisplayItem = new ItemStack(Material.REDSTONE, 1);

		mPeriod = isLevelOne() ? BRUTAL_ALCHEMY_1_PERIOD : BRUTAL_ALCHEMY_2_PERIOD;
		mDOTDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_DAMAGE, BRUTAL_ALCHEMY_DOT_DAMAGE);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
		});
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!isGruesome) {
			int duration = BRUTAL_ALCHEMY_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

			double damage = mDOTDamage;
			if (isEnhanced() && mAlchemistPotions != null) {
				damage += mAlchemistPotions.getDamage() * (BRUTAL_ALCHEMY_ENHANCEMENT_DAMAGE_POTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_MULTIPLIER));
			}
			mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, new CustomDamageOverTime(duration, damage, mPeriod, mPlayer, mInfo.mLinkedSpell));
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_SPREAD_EFFECT_NAME, new SpreadEffectOnDeath(duration, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, CharmManager.getRadius(mPlayer, CHARM_RADIUS, BRUTAL_ALCHEMY_ENHANCEMENT_RANGE), duration, true));
			}
		}
	}

}
