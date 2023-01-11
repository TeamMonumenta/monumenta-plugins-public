package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageDealtSingle;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;

public class EmpoweringOdor extends PotionAbility {
	public static final int POTION_RECHARGE_TIME_REDUCTION_2 = 10;

	private static final int EMPOWERING_ODOR_DURATION = 8 * 20;
	private static final double EMPOWERING_ODOR_SPEED_AMPLIFIER = 0.1;
	private static final double EMPOWERING_ODOR_1_DAMAGE_AMPLIFIER = 0.1;
	private static final double EMPOWERING_ODOR_2_DAMAGE_AMPLIFIER = 0.1;
	private static final String EMPOWERING_ODOR_SPEED_EFFECT_NAME = "EmpoweringOdorSpeedEffect";
	private static final String EMPOWERING_ODOR_DAMAGE_EFFECT_NAME = "EmpoweringOdorDamageEffect";
	private static final String EMPOWERING_ODOR_ENHANCEMENT_EFFECT_NAME = "EmpoweringOdorEnhancementDamageEffect";
	private static final double EMPOWERING_ODOR_ENHANCEMENT_DAMAGE_AMPLIFIER = 0.1;

	public static final String CHARM_DURATION = "Empowering Odor Duration";
	public static final String CHARM_SPEED = "Empowering Odor Speed Modifier";
	public static final String CHARM_DAMAGE = "Empowering Odor Damage Bonus";
	public static final String CHARM_SINGLE_HIT_DAMAGE = "Empowering Odor Single Hit Damage";

	public static final AbilityInfo<EmpoweringOdor> INFO =
		new AbilityInfo<>(EmpoweringOdor.class, "Empowering Odor", EmpoweringOdor::new)
			.scoreboardId("EmpoweringOdor")
			.shorthandName("EO")
			.descriptions(
				"Other players hit by your Alchemist's Potions are given 10% speed and 10% damage from all sources for 8 seconds.",
				"Your potion recharge rate is decreased by 0.5s.",
				"The first hit a player would deal to an enemy after they gain this bonus is increased by 10%, refreshing on each application.")
			.displayItem(new ItemStack(Material.GLOWSTONE_DUST, 1));

	private final double mDamageAmplifier;

	public EmpoweringOdor(Plugin plugin, Player player) {
		super(plugin, player, INFO, 0, 0);
		mDamageAmplifier = (isLevelOne() ? EMPOWERING_ODOR_1_DAMAGE_AMPLIFIER : EMPOWERING_ODOR_2_DAMAGE_AMPLIFIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
	}

	@Override
	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {
		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, EMPOWERING_ODOR_DURATION);
		mPlugin.mEffectManager.addEffect(player, EMPOWERING_ODOR_SPEED_EFFECT_NAME, new PercentSpeed(duration, EMPOWERING_ODOR_SPEED_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), EMPOWERING_ODOR_SPEED_EFFECT_NAME));
		mPlugin.mEffectManager.addEffect(player, EMPOWERING_ODOR_DAMAGE_EFFECT_NAME, new PercentDamageDealt(duration, mDamageAmplifier));
		if (isEnhanced()) {
			mPlugin.mEffectManager.addEffect(player, EMPOWERING_ODOR_ENHANCEMENT_EFFECT_NAME, new PercentDamageDealtSingle(duration, EMPOWERING_ODOR_ENHANCEMENT_DAMAGE_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SINGLE_HIT_DAMAGE)));
		}
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1, 2);
		new PartialParticle(Particle.END_ROD, player.getLocation(), 15, 0.4, 0.6, 0.4, 0).spawnAsPlayerActive(mPlayer);
	}
}
