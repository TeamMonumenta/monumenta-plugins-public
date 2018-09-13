package com.playmonumenta.plugins.specializations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;

public class TenebristSpecialization extends BaseSpecialization {

	public TenebristSpecialization(Plugin plugin, Random random) {
		super(plugin, random);
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				if (player.getLocation().getPitch() > 75 && InventoryUtils.isScytheItem(itemInHand)) {
					int hypnoticRage = ScoreboardUtils.getScoreboardValue(player, "HypnoticRage");
					/*
					 * Hypnotic Rage: Shifting and hitting the block below you
					 * with a scythe creates a 4 block radius circle that causes
					 * mobs within the circle to aggro on each other. Level 2
					 * gives 15% vulnerability to mobs in the circle and regen 1
					 * to players in the circle. Circle lasts for 15 seconds.
					 * 40 second cooldown.
					 */
					if (hypnoticRage > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.HYPNOTIC_RAGE)) {
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 0.75f);
							new BukkitRunnable() {
								Location loc = player.getLocation();
								double rotation = 0;
								double radius = 4;
								int i = 0;
								@Override
								public void run() {
									i++;
									for (double i = 0; i < 64; i++) {
										rotation += 5;
										double cRadian = Math.toRadians(rotation);
										loc.add(Math.cos(cRadian) * radius, 0, Math.sin(cRadian) * radius);
										ParticleEffect.SPELL_WITCH.display(0, 0, 0, 0, 1, loc, 40);
										if (rotation % 90 == 0) {
											ParticleEffect.SPELL_WITCH.display(0, 0, 0, 1, 12, loc, 40);
										}
										loc.subtract(Math.cos(cRadian) * radius, 0, Math.sin(cRadian) * radius);
									}

									for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
										if (EntityUtils.isHostileMob(e)) {
											Creature c = (Creature) e;
											if (c.getLocation().toVector().isInSphere(loc.toVector(), radius)) {
												if (hypnoticRage > 1) {
													c.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 20 * 3, 2, true, false));
												}

												if (c.getTarget() != null) {
													LivingEntity target = c.getTarget();
													if (target instanceof Player) {
														List<Entity> entities = new ArrayList<Entity>();
														for (Entity te : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
															if (!te.equals(e) && EntityUtils.isHostileMob(te) && !EntityUtils.isBoss(te)) {
																entities.add(te);
															}
														}
														if (entities.size() > 0) {
															Entity tar = entities.get(mRandom.nextInt(entities.size()));
															while (!EntityUtils.isHostileMob(tar) || tar.equals(c)) {
																tar = entities.get(mRandom.nextInt(entities.size()));
															}
															for (Entity te : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
																if (te.equals(e)) {
																	continue;
																}
																if (EntityUtils.isHostileMob(te)) {
																	LivingEntity newTar = (LivingEntity) te;
																	c.setTarget(newTar);
																	break;
																}
															}
														}
													}
												}
											}
										} else {
											if (hypnoticRage > 1 && e instanceof Player) {
												Player p = (Player) e;
												mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 20 * 2, 0, true, true));
											}
										}
									}

									if (i >= 20 * 15) {
										this.cancel();
										for (Entity e : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
											if (EntityUtils.isHostileMob(e)) {
												Creature c = (Creature) e;
												if (c.getLocation().toVector().isInSphere(loc.toVector(), radius)) {
													if (c.getTarget() != null) {
														c.setTarget(null);
													}
												}
											}
										}
									}

								}

							}.runTaskTimer(mPlugin, 0, 1);
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.HYPNOTIC_RAGE, 20 * 40);
						}
					}
				}
			}
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (player.getLocation().getPitch() < -75 && player.isSneaking()) {
				int blackClouds = ScoreboardUtils.getScoreboardValue(player, "BlackClouds");
				/*
				 * Black Clouds: Block with a shield while looking upwards and
				 * summon a black cloud over you and all allies in a 12 block
				 * radius. If a player is hit, a lightning bolt is summoned,
				 * damaging nearby enemies by 22/28 and granting strength 2 for
				 * 15/30 seconds. This clears the cloud. Level 2 grants resistance
				 * to players with cloud overhead. 45 (or higher) second cooldown.
				 * This cloud lasts for 5 seconds.
				 */
				if (blackClouds > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BLACK_CLOUDS)) {
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 0.65f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 0.85f);

						double dmg = blackClouds == 1 ? 22 : 28;
						int duration = blackClouds == 1 ? 15 * 20 : 30 * 20;
						for (Player p : PlayerUtils.getNearbyPlayers(player.getLocation().add(0, 6, 0), 12)) {
							if (blackClouds > 1) {
								mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 5, 1, true, true));
							}
						}
						new BukkitRunnable() {
							Location loc = player.getLocation().add(0, 6, 0);
							int i = 0;
							@Override
							public void run() {
								i++;
								ParticleEffect.SMOKE_LARGE.display(12, 0.2f, 12, 0.1f, 75, loc, 40);
								ParticleEffect.CLOUD.display(12, 0.2f, 12, 0.05f, 10, loc, 40);

								for (Player p : PlayerUtils.getNearbyPlayers(loc, 12)) {
									if (MetadataUtils.happenedThisTick(mPlugin, p, Constants.PLAYER_DAMAGE_NONCE_METAKEY, -1)) {
										Location l = p.getLocation();
										p.getWorld().strikeLightningEffect(l);
										ParticleEffect.FLAME.display(0, 0, 0, 0.35f, 250, l, 40);
										ParticleEffect.SMOKE_LARGE.display(0, 0, 0, 0.25f, 75, l, 40);
										p.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);
										p.getWorld().playSound(l, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 0.9f);

										for (Entity e : p.getNearbyEntities(3, 3, 3)) {
											if (EntityUtils.isHostileMob(e)) {
												LivingEntity le = (LivingEntity) e;
												EntityUtils.damageEntity(mPlugin, le, dmg, p);
												mPlugin.mPotionManager.addPotion(p, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0, true, true));
											}
										}
										this.cancel();
										break;
									}
								}
								if (i >= 20 * 5) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BLACK_CLOUDS, 20 * 45);
					}
				}
			}
			if (player.isSprinting() && InventoryUtils.isScytheItem(itemInHand)) {
				int sableSphere = ScoreboardUtils.getScoreboardValue(player, "SableSphere");
				/*
				 * Sable Sphere: With Scythe in main hand, Right-click while
				 * sprinting to hurl a slow-moving ball of dark energy (Ghast
				 * fireball). Sphere does 30/45 damage upon impact. Cooldown
				 * 20/12 seconds
				 */
				if (sableSphere > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.SABLE_SPHERE)) {
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0.25f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 0.25f);

						double dmg = sableSphere == 1 ? 30 : 45;
						int cooldown = sableSphere == 1 ? 20 * 20 : 20 * 12;
						new BukkitRunnable() {
							Location loc = player.getEyeLocation();
							Vector dir = loc.getDirection().normalize();
							int t = 0;
							@Override
							public void run() {
								t++;
								loc.add(dir.clone().multiply(0.15));
								ParticleEffect.SPELL_WITCH.display(0.35f, 0.35f, 0.35f, 1, 30, loc, 40);
								ParticleEffect.SPELL_MOB.display(0.35f, 0.35f, 0.35f, 0, 25, loc, 40);

								for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.85, 0.85, 0.85)) {
									if (EntityUtils.isHostileMob(e)) {
										EntityUtils.damageEntity(mPlugin, (LivingEntity) e, dmg, player);
										ParticleEffect.DRAGONBREATH.display(0, 0, 0, 0.25f, 150, loc, 40);
										ParticleEffect.SMOKE_LARGE.display(0, 0, 0, 0.2f, 80, loc, 40);
										loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
										this.cancel();
										break;
									}
								}
								if (loc.getBlock().getType().isSolid()) {
									ParticleEffect.DRAGONBREATH.display(0, 0, 0, 0.25f, 150, loc, 40);
									ParticleEffect.SMOKE_LARGE.display(0, 0, 0, 0.2f, 80, loc, 40);
									loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
									this.cancel();
								}
								if (t >= 20 * 5) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SABLE_SPHERE, cooldown);
					}
				}
			}
		}
	}

}
