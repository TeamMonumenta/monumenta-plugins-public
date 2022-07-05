package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PhlegmaticResolve extends Ability {

	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "PhlegmaticPercentDamageResistEffect";
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "PhlegmaticPercentKnockbackResistEffect";
	private static final double PERCENT_DAMAGE_RESIST_1 = -0.015;
	private static final double PERCENT_DAMAGE_RESIST_2 = -0.025;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.05;
	private static final int RADIUS = 7;

	private final double mPercentDamageResist;

	public PhlegmaticResolve(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Phlegmatic Resolve");
		mInfo.mScoreboardId = "Phlegmatic";
		mInfo.mShorthandName = "PR";
		mInfo.mDescriptions.add("For each spell on cooldown, gain +1.5% Damage Reduction and +0.5 Knockback Resistance.");
		mInfo.mDescriptions.add("Increase to +2.5% Damage Reduction per spell on cooldown, and players within 7 blocks are given 33% of your bonuses. (Does not stack with multiple Warlocks.)");
		mDisplayItem = new ItemStack(Material.SHIELD, 1);
		mPercentDamageResist = getAbilityScore() == 1 ? PERCENT_DAMAGE_RESIST_1 : PERCENT_DAMAGE_RESIST_2;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second

		if (mPlayer == null) {
			return;
		}

		int cooldowns = 0;
		for (Integer ability : mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId())) {
			if (ability > 0) {
				cooldowns++;
			}
		}
		if (cooldowns == 0) {
			return;
		}

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(20, mPercentDamageResist * cooldowns));
		mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, PERCENT_KNOCKBACK_RESIST * cooldowns, KNOCKBACK_RESIST_EFFECT_NAME));

		if (getAbilityScore() > 1) {
			for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), RADIUS, false)) {
				mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(20, mPercentDamageResist * cooldowns / 3.0));
				mPlugin.mEffectManager.addEffect(p, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, PERCENT_KNOCKBACK_RESIST * cooldowns / 3.0, KNOCKBACK_RESIST_EFFECT_NAME));
			}
		}
	}
}
