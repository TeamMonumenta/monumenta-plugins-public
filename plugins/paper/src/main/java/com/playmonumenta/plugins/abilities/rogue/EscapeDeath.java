package com.playmonumenta.plugins.abilities.rogue;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class EscapeDeath extends Ability {

	private static final double ESCAPE_DEATH_HEALTH_TRIGGER = 10;
	private static final int ESCAPE_DEATH_DURATION = 5 * 20;
	private static final int ESCAPE_DEATH_DURATION_OTHER = 8 * 20;
	private static final int ESCAPE_DEATH_ABSORBTION_EFFECT_LVL = 1;
	private static final int ESCAPE_DEATH_SPEED_EFFECT_LVL = 1;
	private static final int ESCAPE_DEATH_JUMP_EFFECT_LVL = 2;
	private static final int ESCAPE_DEATH_RANGE = 5;
	private static final int ESCAPE_DEATH_DURATION_SLOWNESS = 5 * 20;
	private static final int ESCAPE_DEATH_SLOWNESS_EFFECT_LVL = 4;
	private static final int ESCAPE_DEATH_WEAKNES_EFFECT_LEVEL = 2;
	private static final int ESCAPE_DEATH_COOLDOWN = 90 * 20;

	public EscapeDeath(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Escape Death");
		mInfo.linkedSpell = Spells.ESCAPE_DEATH;
		mInfo.scoreboardId = "EscapeDeath";
		mInfo.mShorthandName = "ED";
		mInfo.mDescriptions.add("When your health drops below 5 hearts, you throw a paralyzing grenade, afflicting all nearby enemies (5 blocks) with Slowness V and Weakness III for 5 s. (cooldown: 90 s).");
		mInfo.mDescriptions.add("When this skill is triggered, you gain 5 s of Absorption II as well as 8 s of Speed II and Jump Boost III");
		mInfo.cooldown = ESCAPE_DEATH_COOLDOWN;
	}

	/*
	 * Only activates when taking damage from mobs
	 */
	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		int escapeDeath = getAbilityScore();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), ESCAPE_DEATH_RANGE, mPlayer)) {
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, ESCAPE_DEATH_DURATION_SLOWNESS,
			                                                       ESCAPE_DEATH_SLOWNESS_EFFECT_LVL, true, false));
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, ESCAPE_DEATH_DURATION_SLOWNESS,
			                                                       ESCAPE_DEATH_WEAKNES_EFFECT_LEVEL, true, false));
		}

		if (escapeDeath > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.ABSORPTION, ESCAPE_DEATH_DURATION,
			                                                  ESCAPE_DEATH_ABSORBTION_EFFECT_LVL, true, true));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED, ESCAPE_DEATH_DURATION_OTHER,
			                                                  ESCAPE_DEATH_SPEED_EFFECT_LVL, true, true));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.JUMP, ESCAPE_DEATH_DURATION_OTHER,
			                                                  ESCAPE_DEATH_JUMP_EFFECT_LVL, true, true));
		}

		Location loc = mPlayer.getLocation();
		loc.add(0, 1, 0);

		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 80, 0, 0, 0, 0.25);
		mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 125, 0, 0, 0, 0.3);

		mWorld.playSound(loc, Sound.ITEM_TOTEM_USE, 0.75f, 1.5f);
		mWorld.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 0f);

		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Escape Death has been activated");
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		EntityDamageEvent lastDamage = mPlayer.getLastDamageCause();
		if (lastDamage != null && lastDamage.getCause() == DamageCause.ENTITY_ATTACK) {
			double correctHealth = mPlayer.getHealth() - lastDamage.getFinalDamage();
			if (!mPlayer.isDead() && correctHealth > 0 && correctHealth <= ESCAPE_DEATH_HEALTH_TRIGGER) {
				return true;
			}
		}
		return false;
	}
}
