package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Collection;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class GruesomeAlchemy extends Ability {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final int GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0;
	private static final int GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 1;
	private static final int GRUESOME_ALCHEMY_1_WITHER_AMPLIFIER = 0;
	private static final int GRUESOME_ALCHEMY_2_WITHER_AMPLIFIER = 1;

	private final int mSlownessAmplifier;
	private final int mWitherAmplifier;

	public GruesomeAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Gruesome Alchemy");
		mInfo.scoreboardId = "GruesomeAlchemy";
		mInfo.mShorthandName = "GA";
		mInfo.mDescriptions.add("Your Alchemist's Potions give Slowness I and Wither I for 8 seconds.");
		mInfo.mDescriptions.add("Your Alchemist's Potions now give Slowness II and Wither II.");
		mSlownessAmplifier = getAbilityScore() == 1 ? GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER : GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER;
		mWitherAmplifier = getAbilityScore() == 1 ? GRUESOME_ALCHEMY_1_WITHER_AMPLIFIER : GRUESOME_ALCHEMY_2_WITHER_AMPLIFIER;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						apply(entity);
					}
				}
			}
		}
		return true;
	}

	public void apply(LivingEntity mob) {
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, GRUESOME_ALCHEMY_DURATION, mSlownessAmplifier, false, true));
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, GRUESOME_ALCHEMY_DURATION, mWitherAmplifier, false, true));
	}

}
