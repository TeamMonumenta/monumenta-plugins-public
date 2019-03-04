package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

public class BladeDance extends Ability {

	/*
	 * Blade Dance: Sprint Right Click to begin a blade dance.
	 * For 2 seconds, you are entirely unstoppable (you cannot
	 * take damage or be inflicted with negative debuffs but
	 * still take knockback) and unable to attack. After 2 seconds,
	 * end the dance with a finishing spin, dealing 18 damage to
	 * enemies within 4 blocks. At level 2, you get Speed 2 while
	 * dancing and the damage is increased to 25. Cooldown: 40
	 * seconds
	 */

	private boolean mActive = false;

	public BladeDance(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "BladeDance";
		mInfo.linkedSpell = Spells.BLADE_DANCE;
		mInfo.cooldown = 20 * 40;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;

		/*
		 * NOTE! Because BladeDance has two events (cast and damage), we
		 * need both events to trigger even when it is on cooldown. Therefor it
		 * needs to bypass the automatic cooldown check and manage cooldown
		 * itself
		 */
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean cast() {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.BLADE_DANCE)) {
			return false;
		}

		if (mPlayer.isSprinting()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (mainHand != null && InventoryUtils.isSwordItem(mainHand)) {
				mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
				mWorld.spawnParticle(Particle.SWEEP_ATTACK, mPlayer.getLocation(), 150, 4, 4, 4, 0);
				mPlayer.setInvulnerable(true);
				mActive = true;
				int bladeDance = getAbilityScore();
				double damage = bladeDance == 1 ? 9 : 12;
				if (bladeDance >= 2) {
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED,
					                                 20 * 2, 1, true, false));
				}
				new BukkitRunnable() {
					int i = 0;
					float pitch = 0;
					Location loc = mPlayer.getLocation();
					double y = loc.getY();
					double extraDamage = 0;
					@Override
					public void run() {
						i += 2;
						Location checkLoc = mPlayer.getLocation();
						checkLoc.setY(y);
						extraDamage += checkLoc.distance(loc);
						loc = mPlayer.getLocation();
						loc.setY(y);
						mWorld.spawnParticle(Particle.SWEEP_ATTACK, mPlayer.getLocation(), 10, 4, 4, 4, 0);
						mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, pitch);
						pitch += 0.2;
						new BukkitRunnable() {
							Location loc1 = mPlayer.getLocation().add(6, 6, 6);
							Location loc2 = mPlayer.getLocation().add(-6, -1, -6);

							double x1 = ThreadLocalRandom.current().nextDouble(loc2.getX(), loc1.getX());
							double y1 = ThreadLocalRandom.current().nextDouble(loc2.getY(), loc1.getY());
							double z1 = ThreadLocalRandom.current().nextDouble(loc2.getZ(), loc1.getZ());
							Location l1 = new Location(mWorld, x1, y1, z1);

							double x2 = ThreadLocalRandom.current().nextDouble(loc2.getX(), loc1.getX());
							double y2 = ThreadLocalRandom.current().nextDouble(loc2.getY(), loc1.getY());
							double z2 = ThreadLocalRandom.current().nextDouble(loc2.getZ(), loc1.getZ());
							Location l2 = new Location(mWorld, x2, y2, z2);

							Vector dir = LocationUtils.getDirectionTo(l2, l1);

							int t = 0;
							@Override
							public void run() {
								t++;
								l1.add(dir.clone().multiply(1.15));
								mWorld.spawnParticle(Particle.CRIT_MAGIC, l1, 4, 0, 0, 0, 0.35);
								mWorld.spawnParticle(Particle.CLOUD, l1, 1, 0, 0, 0, 0);
								mWorld.spawnParticle(Particle.SWEEP_ATTACK, l1, 1, 0, 0, 0, 0);
								if (t >= 10) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);

						if (i >= 40) {
							mPlayer.setInvulnerable(false);
							mActive = false;
							this.cancel();

							//Ultra flash
							new BukkitRunnable() {
								double rotation = 0;
								Location loc = mPlayer.getLocation();
								double radius = 0;
								double y = 2.5;
								double yminus = 0.35;

								@Override
								public void run() {

									radius += 1;
									for (int i = 0; i < 15; i += 1) {
										rotation += 24;
										double radian1 = Math.toRadians(rotation);
										loc.add(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);
										mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0);
										mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 3, 0.1, 0.1, 0.1, 0.1);
										loc.subtract(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);

									}
									y -= y * yminus;
									yminus += 0.02;
									if (yminus >= 1) {
										yminus = 1;
									}
									if (radius >= 7) {
										this.cancel();
									}

								}

							}.runTaskTimer(mPlugin, 0, 1);
							mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
							mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
							mWorld.spawnParticle(Particle.FLAME, mPlayer.getLocation(), 150, 0, 0, 0, 0.25);
							mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 70, 0, 0, 0, 0.25);
							mWorld.spawnParticle(Particle.SWEEP_ATTACK, mPlayer.getLocation(), 150, 4, 4, 4, 0);
							for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), 4, mPlayer)) {
								EntityUtils.damageEntity(mPlugin, le, damage + extraDamage, mPlayer);
							}
						}
					}
				}.runTaskTimer(mPlugin, 0, 2);
				putOnCooldown();
			}
		}
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mActive) {
			return false;
		}
		return true;
	}
}
