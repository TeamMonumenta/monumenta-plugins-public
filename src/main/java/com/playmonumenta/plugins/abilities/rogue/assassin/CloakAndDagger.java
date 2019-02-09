package com.playmonumenta.plugins.abilities.rogue.assassin;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;


/*
 * Cloak & Dagger: When you kill an enemy, you cloak
 *  yourself in Invisibility for 5 s, automatically
 *  making enemies un-target you, and preventing them
 *  from targeting you while invisible (dealing any
 *  damage cancels this effect). Your next sword attack
 *  after coming out of stealth deals 2 / 4 extra damage
 *  per s you have been invisible. At Level 2, you gain
 *  Speed II during the effect. Cooldown: 20 s
 */
public class CloakAndDagger extends Ability {

	public CloakAndDagger(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CloakAndDagger";
		mInfo.cooldown = 20 * 20;
		mInfo.linkedSpell = Spells.CLOAK_AND_DAGGER;
		mInfo.ignoreCooldown = true;
	}

	private boolean active = false;
	private int time = 0;

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (active) {
			active = false;
			time = 0;
			mPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
			double damage = getAbilityScore() == 1 ? 2 : 4;
			event.setDamage(event.getDamage() + (damage * time));
		}
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.CLOAK_AND_DAGGER)) {
			return;
		}

		active = true;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 5, 0, false, true));
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
		mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
		mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
		for (Mob mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 20)) {
			if (mob.getTarget() != null && mob.getTarget().getUniqueId().equals(mPlayer.getUniqueId())) {
				mob.setTarget(null);
			}
		}
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				if (t % 20 == 0) {
					time++;
				}
				if (t >= 20 * 5 || !active) {
					this.cancel();
					time = 0;
					active = false;
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1);
					mWorld.spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1);
				}
				t++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
	}

	@Override
	public void EntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		if (active) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}

}
