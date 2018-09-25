package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class HierophantSpecialization extends BaseSpecialization {
	private static final Particle.DustOptions THURIBLE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 175), 1.0f);
	private World mWorld;

	public HierophantSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	public static final String PLAYER_THURIBLE_METAKEY = "PlayerIncensedThuribleMetakey";
	@Override
	public boolean PlayerDamagedByLivingEntityRadiusEvent(Player player, Player caster, LivingEntity damager, EntityDamageByEntityEvent event) {
		int holyVengeance = ScoreboardUtils.getScoreboardValue(player, "HolyVengeance");
		if (holyVengeance > 0) {
			/*
			 * Holy Vengeance: When an ally takes melee damage within 12
			 * blocks of you, the mob who attacked them takes 3/5 damage
			 * and gets Slowness 2/3 for 5 seconds
			 */
			if (!player.equals(caster) && caster.getLocation().distance(player.getLocation()) <= 12) {
				int damage = holyVengeance == 1 ? 3 : 5;
				int amplifier = holyVengeance == 1 ? 1 : 2;

				if (EntityUtils.isHostileMob(damager)) {
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, damager.getLocation().add(0, damager.getHeight() / 2, 0), 4, 0, 0, 0, 0.15f);
					EntityUtils.damageEntity(mPlugin, damager, damage, caster);
					damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, amplifier, false, true));

				}
			}
		}
		return true;
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			int incensedThurible = ScoreboardUtils.getScoreboardValue(player, "IncensedThurible");
			/*
			 *Level 1 - Blocking with a shield in either hand procedurally builds up passive buffs,
			 *which are applied to all players (except the user) within 20 blocks for as long as
			 *the block is held. The Hierophant moves at ~80% normal walking speed when blocking
			 *to use this ability.
			 *
			 *Progression: Speed 1 (2 Seconds of Blocking), Strength 1 (4 seconds of blocking), Resistance 1
			 *(6 seconds of blocking)
			 *
			 *Level 2 - The radius of the buffs increase to 30, and the Hiero moves at normal walk
			 *speed while blocking.
			 *
			 *Progression: In addition to the previous progression, after blocking for 10 seconds,
			 *all players (including the user) within the range are given 10 seconds of Absorption 1,
			 * Speed 1, Strength 1, and Resistance 1.
			 */
			if (incensedThurible > 0) {
				if (!player.hasMetadata(PLAYER_THURIBLE_METAKEY)) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1, 1);
					player.setMetadata(PLAYER_THURIBLE_METAKEY, new FixedMetadataValue(mPlugin, 0));
					new BukkitRunnable() {
						int t = 0;
						int seconds = 0;
						double rot = 0;
						int strands = 3;
						double rotAdd = 360 / strands;
						double radius = 3;
						@Override
						public void run() {
							t++;
							rot += 10;
							radius -= 0.1428;
							Location ploc = player.getLocation();
							for (int i = 0; i < strands; i++) {
								double radian = Math.toRadians(rot + (rotAdd * i));
								ploc.add(Math.cos(radian)*radius, 0, Math.sin(radian)*radius);
								mWorld.spawnParticle(Particle.REDSTONE, ploc, 15, 0.1, 0.1, 0.1, THURIBLE_COLOR);
								mWorld.spawnParticle(Particle.SPELL_INSTANT, ploc, 5, 0.1f, 0.1f, 0.1f, 0);
								ploc.subtract(Math.cos(radian)*radius, 0, Math.sin(radian)*radius);
							}
							if (t > 20) {
								radius = 3;
								t = 0;
								seconds++;
								player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.85f, 1.15f);
								mWorld.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1.15, 40), 15, 2, 0.4f, 2, 0);
							}

							int radius = incensedThurible == 1 ? 20 : 30;
							int duration = 20 * 5;
							List<Player> players = PlayerUtils.getNearbyPlayers(player.getLocation(), radius);
							if (incensedThurible < 2) {
								if (players.contains(player) && seconds < 10) {
									players.remove(player);
								}
							}
							if (seconds >= 2) {
								for (Player pl : players) {
									mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, duration, 0, true, true));
								}
							}

							if (seconds >= 4) {
								for (Player pl : players) {
									mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, true, true));
								}
							}

							if (seconds >= 6) {
								for (Player pl : players) {
									mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0, true, true));
								}
							}
							if (incensedThurible > 1) {
								if (seconds >= 10) {
									for (Player pl : players) {
										mPlugin.mPotionManager.addPotion(pl, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.ABSORPTION, duration, 0, true, true));
									}
								}
							}
							if (t >= 2) {
								if (player.isDead() || (!player.isHandRaised() && !player.isBlocking())) {
									this.cancel();
									player.removeMetadata(PLAYER_THURIBLE_METAKEY, mPlugin);
								}
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

			if (player.isSprinting()) {
				int choirBells = ScoreboardUtils.getScoreboardValue(player, "ChoirBells");
				/*
				 * Choir Bells: Right-Clicking while sprinting causes the Cleric to
				 * become the target of any undead within a cone area in front of them.
				 * Affected Undead are stricken with slowness II (level 1) and 30%
				 * Vulnerability (level 2). Cooldown 30/20s
				 */

				if (choirBells > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CHOIR_BELLS)) {
						int cooldown = choirBells == 1 ? 30 * 20 : 20 * 20;
						ParticleUtils.explodingConeEffect(mPlugin, player, 8, Particle.VILLAGER_HAPPY, 0.5f, Particle.SPELL_INSTANT, 0.5f, 0.33);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.4f);
						Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
						for (Entity e : player.getNearbyEntities(8, 8, 8)) {
							if (e instanceof LivingEntity) {
								LivingEntity le = (LivingEntity) e;
								if (EntityUtils.isUndead(le)) {
									Vector toMobVector = le.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
									if (playerDir.dot(toMobVector) > 0.33) {
										((Monster) le).setTarget(player);
										if (choirBells > 0) {
											le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1, true, false));
										}
										if (choirBells > 1) {
											le.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 2, 5, true, false));
										}
									}
								}
							}
						}

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CHOIR_BELLS, cooldown);
					}
				}
			}
		}
	}
}
