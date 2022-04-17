package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.BrutalAlchemyEnhancementEffect;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.server.properties.ServerProperties;
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
	public static final String BRUTAL_ALCHEMY_DOT_EFFECT_NAME = "BrutalAlchemyDamageOverTimeEffect";
	private static final double BRUTAL_ALCHEMY_ENHANCEMENT_DAMAGE_POTION = 0.2;
	public static final double BRUTAL_ALCHEMY_ENHANCEMENT_RANGE = 3;

	private int mPeriod;
	private double mDOTDamage;
	private boolean mEnhanced;
	private AlchemistPotions mAlchPotions;

	public BrutalAlchemy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Brutal Alchemy", BRUTAL_ALCHEMY_1_DAMAGE, BRUTAL_ALCHEMY_2_DAMAGE);
		mInfo.mLinkedSpell = ClassAbility.BRUTAL_ALCHEMY;
		mInfo.mScoreboardId = "BrutalAlchemy";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions deal +1 damage and apply 1 damage every 2 seconds for 8 seconds.");
		mInfo.mDescriptions.add("Your Brutal Alchemist's Potions now deal +2 damage and apply 1 damage every second instead.");
		mInfo.mDescriptions.add("The damage over time is increased by 20% of your base potion damage, when you kill a mob that had the Brutal Alchemy effect on them, the damage over time spreads to all mobs in a 3 block radius around it.");
		mDisplayItem = new ItemStack(Material.REDSTONE, 1);

		mPeriod = isLevelOne() ? BRUTAL_ALCHEMY_1_PERIOD : BRUTAL_ALCHEMY_2_PERIOD;
		mEnhanced = isEnhanced();
		mDOTDamage = BRUTAL_ALCHEMY_DOT_DAMAGE;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			if (ServerProperties.getClassSpecializationsEnabled() && AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, EsotericEnhancements.class) != null) {
				mDOTDamage = EsotericEnhancements.BRUTAL_DOT_DAMAGE;
			}

		});

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			mAlchPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
		}, 5);
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome) {
		if (!isGruesome) {
			if (mEnhanced) {
				if (mAlchPotions != null) {
					mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, new BrutalAlchemyEnhancementEffect(BRUTAL_ALCHEMY_DURATION, mDOTDamage + (mAlchPotions.getDamage() * BRUTAL_ALCHEMY_ENHANCEMENT_DAMAGE_POTION), mPeriod, mPlayer, mInfo.mLinkedSpell, Particle.SQUID_INK));
					return;
				}
			}
			mPlugin.mEffectManager.addEffect(mob, BRUTAL_ALCHEMY_DOT_EFFECT_NAME, new CustomDamageOverTime(BRUTAL_ALCHEMY_DURATION, mDOTDamage, mPeriod, mPlayer, mInfo.mLinkedSpell, Particle.SQUID_INK));
		}
	}

}
