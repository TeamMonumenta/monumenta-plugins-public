package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Level 1: When you hit an enemy with another skill they gain 15% vulnerability
 * for 5 seconds. Level 2: The vulnerability given is increased to 25% and the
 * warlock gains +1 armor and +.5 armor toughness for every ability they have on
 * cooldown lasting until skills come off cooldown.
 */

public class BlasphemousAura extends Ability {

	private static final int BLASPHEMY_1_VULN_LEVEL = 2;
	private static final int BLASPHEMY_2_VULN_LEVEL = 4;
	private static final int BLASPHEMY_VULN_DURATION = 5 * 20;
	private static final double BLASPHEMY_ARMOR_INCREMENT = 1;
	private static final double BLASPHEMY_TOUGHNESS_INCREMENT = 0.5;

	private double baseArmor = 0;
	private double baseToughness = 0;
	private int oldBonus = 0;

	public BlasphemousAura(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "BlasphemousAura";
	}

	@Override
	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) {
		LivingEntity damagee = event.getDamaged();
		int amp = getAbilityScore() == 1 ? BLASPHEMY_1_VULN_LEVEL : BLASPHEMY_2_VULN_LEVEL;
		PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.UNLUCK, BLASPHEMY_VULN_DURATION, amp, false, true));
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (getAbilityScore() > 1) {
			if (oldBonus == 0) {
				baseArmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR).getBaseValue();
				baseToughness = mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getBaseValue();
			}

			GraspingClaws gc = (GraspingClaws) AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
			Ability[] abilities = new Ability[3];
			abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
			abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, ConsumingFlames.class);
			abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);

			int bonus = 0;
			if (gc != null && gc.onCooldown()) {
				bonus++;
			}
			for (int i = 0; i < abilities.length; i++) {
				if (abilities[i] != null && abilities[i].isOnCooldown()) {
					bonus++;
				}
			}
			oldBonus = bonus;

			AttributeInstance armor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
			armor.setBaseValue(baseArmor + bonus * BLASPHEMY_ARMOR_INCREMENT);
			AttributeInstance toughness = mPlayer.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
			toughness.setBaseValue(baseToughness + bonus * BLASPHEMY_TOUGHNESS_INCREMENT);
		}
	}
}
