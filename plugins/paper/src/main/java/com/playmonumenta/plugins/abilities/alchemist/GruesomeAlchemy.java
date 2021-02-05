package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class GruesomeAlchemy extends PotionAbility {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final double GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0.1;
	private static final double GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 0.2;
	private static final int GRUESOME_ALCHEMY_1_WITHER_AMPLIFIER = 0;
	private static final int GRUESOME_ALCHEMY_2_WITHER_AMPLIFIER = 1;

	private final double mSlownessAmount;
	private final int mWitherAmplifier;

	public GruesomeAlchemy(Plugin plugin, Player player) {
		super(plugin, player, "Gruesome Alchemy", 0, 0);
		mInfo.mScoreboardId = "GruesomeAlchemy";
		mInfo.mShorthandName = "GA";
		mInfo.mDescriptions.add("Your Alchemist's Potions give 10% Slowness and Wither I for 8 seconds.");
		mInfo.mDescriptions.add("Your Alchemist's Potions now give 20% Slowness and Wither II.");

		mSlownessAmount = getAbilityScore() == 1 ? GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER : GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER;
		mWitherAmplifier = getAbilityScore() == 1 ? GRUESOME_ALCHEMY_1_WITHER_AMPLIFIER : GRUESOME_ALCHEMY_2_WITHER_AMPLIFIER;
	}

	@Override
	public void apply(LivingEntity mob) {
		EntityUtils.applySlow(mPlugin, GRUESOME_ALCHEMY_DURATION, mSlownessAmount, mob);
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, GRUESOME_ALCHEMY_DURATION, mWitherAmplifier, false, true));
	}

}
