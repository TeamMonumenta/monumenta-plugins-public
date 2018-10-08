package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

import java.util.Random;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

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
		super(plugin, world, random, player);
		mInfo.classId = 4;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.ESCAPE_DEATH;
		mInfo.scoreboardId = "EscapeDeath";
		mInfo.cooldown = ESCAPE_DEATH_COOLDOWN;
	}

	/*
	 * Should we also make this escape death from general mob damage? (Includes projectile, mob spells, mob melee)
	 * TODO: Yes, probably want a generic player damage event instead
	 */
	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		int escapeDeath = getAbilityScore(player);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), ESCAPE_DEATH_RANGE)) {
			mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ESCAPE_DEATH_DURATION_SLOWNESS,
			                                     ESCAPE_DEATH_SLOWNESS_EFFECT_LVL, true, false));
			mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ESCAPE_DEATH_DURATION_SLOWNESS,
			                                     ESCAPE_DEATH_WEAKNES_EFFECT_LEVEL, true, false));
		}

		if (escapeDeath > 1) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.ABSORPTION, ESCAPE_DEATH_DURATION,
			                                                  ESCAPE_DEATH_ABSORBTION_EFFECT_LVL, true, false));
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED, ESCAPE_DEATH_DURATION_OTHER,
			                                                  ESCAPE_DEATH_SPEED_EFFECT_LVL, true, false));
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.JUMP, ESCAPE_DEATH_DURATION_OTHER,
			                                                  ESCAPE_DEATH_JUMP_EFFECT_LVL, true, false));
		}

		World world = player.getWorld();
		Location loc = player.getLocation();
		loc.add(0, 1, 0);

		double offset = escapeDeath == 1 ? 1 : ESCAPE_DEATH_RANGE;
		int particles = escapeDeath == 1 ? 30 : 500;

		mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, particles, offset, offset, offset, 0.001);

		if (escapeDeath > 1) {
			mWorld.spawnParticle(Particle.CLOUD, loc, particles, offset, offset, offset, 0.001);
		}

		world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.5f, 0.5f);

		MessagingUtils.sendActionBarMessage(mPlugin, player, "Escape Death has been activated");
		putOnCooldown(player);
		return true;
	}

	@Override
	public boolean runCheck(Player player) {
		EntityDamageEvent lastDamage = player.getLastDamageCause();
		if (lastDamage.getCause() == DamageCause.ENTITY_ATTACK) {
			double correctHealth = player.getHealth() - lastDamage.getFinalDamage();
			if (!player.isDead() && correctHealth > 0 && correctHealth <= ESCAPE_DEATH_HEALTH_TRIGGER) {
				return true;
			}
		}
		return false;
	}
}
