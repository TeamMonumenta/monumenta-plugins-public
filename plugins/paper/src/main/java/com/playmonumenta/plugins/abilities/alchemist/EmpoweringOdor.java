package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EmpoweringOdor extends PotionAbility {
	public static final double POTION_RECHARGE_TIME_REDUCTION_2 = 10;

	private static final int EMPOWERING_ODOR_DURATION = 8 * 20;
	private static final double EMPOWERING_ODOR_SPEED_AMPLIFIER = 0.1;
	private static final double EMPOWERING_ODOR_1_DAMAGE_AMPLIFIER = 0.1;
	private static final double EMPOWERING_ODOR_2_DAMAGE_AMPLIFIER = 0.15;
	private static final String EMPOWERING_ODOR_SPEED_EFFECT_NAME = "EmpoweringOdorSpeedEffect";
	private static final String EMPOWERING_ODOR_DAMAGE_EFFECT_NAME = "EmpoweringOdorDamageEffect";

	private double mDamageAmplifier;

	public EmpoweringOdor(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Empowering Odor", 0, 0);
		mInfo.mScoreboardId = "EmpoweringOdor";
		mInfo.mShorthandName = "EO";
		mInfo.mDescriptions.add("Other players hit by your Alchemist's Potions are given 10% speed and 10% damage from all sources for 8 seconds.");
		mInfo.mDescriptions.add("The damage is increased to 15%. Your potion recharge rate is decreased by 0.5s.");
		mDisplayItem = new ItemStack(Material.GLOWSTONE_DUST, 1);

		mDamageAmplifier = getAbilityScore() == 1 ? EMPOWERING_ODOR_1_DAMAGE_AMPLIFIER : EMPOWERING_ODOR_2_DAMAGE_AMPLIFIER;
	}

	@Override
	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {
		mPlugin.mEffectManager.addEffect(player, EMPOWERING_ODOR_SPEED_EFFECT_NAME, new PercentSpeed(EMPOWERING_ODOR_DURATION, EMPOWERING_ODOR_SPEED_AMPLIFIER, EMPOWERING_ODOR_SPEED_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(player, EMPOWERING_ODOR_DAMAGE_EFFECT_NAME, new PercentDamageDealt(EMPOWERING_ODOR_DURATION, mDamageAmplifier));
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 2);
		player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 15, 0.4, 0.6, 0.4, 0);
	}
}
