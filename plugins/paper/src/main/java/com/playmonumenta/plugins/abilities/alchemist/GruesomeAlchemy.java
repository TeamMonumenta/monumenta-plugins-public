package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.utils.EntityUtils;

public class GruesomeAlchemy extends PotionAbility {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final double GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER = 0.1;
	private static final double GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER = 0.2;
	private static final int GRUESOME_ALCHEMY_DOT_DAMAGE = 1;
	private static final int GRUESOME_ALCHEMY_1_DOT_PERIOD = 40;
	private static final int GRUESOME_ALCHEMY_2_DOT_PERIOD = 20;
	private static final String DOT_EFFECT_NAME = "GruesomeAlchemyDamageOverTimeEffect";

	private final double mSlownessAmount;
	private final int mDOTPeriod;

	public GruesomeAlchemy(Plugin plugin, Player player) {
		super(plugin, player, "Gruesome Alchemy", 0, 0);
		mInfo.mScoreboardId = "GruesomeAlchemy";
		mInfo.mShorthandName = "GA";
		mInfo.mDescriptions.add("Your Alchemist's Potions give 10% Slowness and 1 damage every 2 seconds for 8 seconds.");
		mInfo.mDescriptions.add("Your Alchemist's Potions now give 20% Slowness and 1 damage every second.");

		mSlownessAmount = getAbilityScore() == 1 ? GRUESOME_ALCHEMY_1_SLOWNESS_AMPLIFIER : GRUESOME_ALCHEMY_2_SLOWNESS_AMPLIFIER;
		mDOTPeriod = getAbilityScore() == 1 ? GRUESOME_ALCHEMY_1_DOT_PERIOD : GRUESOME_ALCHEMY_2_DOT_PERIOD;
		mDisplayItem = new ItemStack(Material.SKELETON_SKULL, 1);
	}

	@Override
	public void apply(LivingEntity mob) {
		EntityUtils.applySlow(mPlugin, GRUESOME_ALCHEMY_DURATION, mSlownessAmount, mob);
		mPlugin.mEffectManager.addEffect(mob, DOT_EFFECT_NAME, new CustomDamageOverTime(GRUESOME_ALCHEMY_DURATION, GRUESOME_ALCHEMY_DOT_DAMAGE, mDOTPeriod, mPlayer, MagicType.ALCHEMY, null, Particle.SQUID_INK, mPlugin));
	}

}
