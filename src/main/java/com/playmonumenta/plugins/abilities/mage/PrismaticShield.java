package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class PrismaticShield extends Ability {

	private static final float PRISMATIC_SHIELD_RADIUS = 4.0f;
	private static final int PRISMATIC_SHIELD_TRIGGER_HEALTH = 6;
	private static final int PRISMATIC_SHIELD_EFFECT_LVL_1 = 1;
	private static final int PRISMATIC_SHIELD_EFFECT_LVL_2 = 2;
	private static final int PRISMATIC_SHIELD_1_DURATION = 12 * 20;
	private static final int PRISMATIC_SHIELD_2_DURATION = 12 * 20;
	private static final int PRISMATIC_SHIELD_COOLDOWN = 80 * 20;
	private static final float PRISMATIC_SHIELD_KNOCKBACK_SPEED = 0.7f;
	private static final int PRISMATIC_SHIELD_1_DAMAGE = 3;
	private static final int PRISMATIC_SHIELD_2_DAMAGE = 6;

	public PrismaticShield(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.PRISMATIC_SHIELD;
		mInfo.scoreboardId = "Prismatic";
		mInfo.cooldown = PRISMATIC_SHIELD_COOLDOWN;
	}

	/*
	 * Works against all types of damage
	 */
	@Override
	public boolean PlayerDamagedEvent(EntityDamageEvent event) {
		// Do not process cancelled damage events
		if (event.isCancelled()) {
			return true;
		}

		// Calculate whether this effect should not be run based on player health
		double correctHealth = mPlayer.getHealth() - event.getFinalDamage();
		if (mPlayer.isDead() || correctHealth <= 0 || correctHealth > PRISMATIC_SHIELD_TRIGGER_HEALTH) {
			return true;
		}

		// Conditions match - prismatic shield
		int prismatic = getAbilityScore();
		int effectLevel = prismatic == 1 ? PRISMATIC_SHIELD_EFFECT_LVL_1 : PRISMATIC_SHIELD_EFFECT_LVL_2;
		int duration = prismatic == 1 ? PRISMATIC_SHIELD_1_DURATION : PRISMATIC_SHIELD_2_DURATION;
		float prisDamage = prismatic == 1 ? PRISMATIC_SHIELD_1_DAMAGE : PRISMATIC_SHIELD_2_DAMAGE;

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), PRISMATIC_SHIELD_RADIUS)) {
			Spellshock.spellDamageMob(mPlugin, mob, prisDamage, mPlayer, MagicType.ARCANE);
			MovementUtils.KnockAway(mPlayer, mob, PRISMATIC_SHIELD_KNOCKBACK_SPEED);
		}

		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.ABSORPTION, duration, effectLevel, true, true));
		mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Prismatic Shield has been activated");

		PlayerUtils.callAbilityCastEvent(mPlayer, Spells.PRISMATIC_SHIELD);
		putOnCooldown();
		return true;
	}
}
