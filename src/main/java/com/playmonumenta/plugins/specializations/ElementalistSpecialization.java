package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.specializations.objects.ElementalSpirit;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class ElementalistSpecialization extends BaseSpecialization {

	private World mWorld;

	public ElementalistSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	private Map<Player, ElementalSpirit> spirits = new HashMap<Player, ElementalSpirit>();

	private void launchMeteor(final Player player, final Location loc) {
		int meteorStrike = ScoreboardUtils.getScoreboardValue(player, "MeteorStrike");
		double damage = meteorStrike == 1 ? 12 : 24;
		loc.add(0, 40, 0);
		new BukkitRunnable() {
			double t = 0;
			double yminus = 0;

			public void run() {
				t = t + 0.85;

				double y = 40 + yminus;
				for (int i = 0; i < 8; i++) {
					yminus -= 0.25;
					loc.subtract(0, 0.25, 0);
					if (loc.getBlock().getType().isSolid()) {
						if (y <= 10) {
							loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
							mWorld.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.235F);
							mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.2F);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0, 0, 0, 0.2F);
							this.cancel();

							for (LivingEntity e : EntityUtils.getNearbyMobs(loc, 4)) {
								EntityUtils.damageEntity(mPlugin, e, damage, player, MagicType.FIRE);

								Vector v = e.getLocation().toVector().subtract(loc.toVector()).normalize();
								v.add(new Vector(0, 0.2, 0));
								e.setVelocity(v);
							}
							break;
						}
					}
				}
				loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				mWorld.spawnParticle(Particle.FLAME, loc, 25, 0.25F, 0.25F, 0.25F, 0.1F);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 5, 0.25F, 0.25F, 0.25F, 0.1F);

				if (t >= 50) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (InventoryUtils.isWandItem(itemInHand)) {
			if (action == Action.RIGHT_CLICK_AIR
			    || (action == Action.RIGHT_CLICK_BLOCK && !blockClicked.isInteractable())) {
				if (player.isSprinting()) {
					int glacialRift = ScoreboardUtils.getScoreboardValue(player, "GlacialRift");
					/*
					 * Glacial Rift: Sprint right click to make a 10 block dash
					 * in the direction you are looking and leave a frozen trail,
					 * mobs that walk on the trail are frozen for 4s and take 8/16 damage
					 * (Player is immune to knockback and damage during the dash 16s
					 * cooldown)
					 */
					if (glacialRift > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.GLACIAL_RIFT)) {
							Location loc = player.getLocation();
							Vector dir = loc.getDirection().normalize();
							dir.multiply(1.2);
							dir.add(new Vector(0, 0.5, 0));
							player.setVelocity(dir);
							player.setInvulnerable(true);
							mWorld.spawnParticle(Particle.CLOUD, loc, 15, 0, 0, 0, 0.15);
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 12, 0, 0, 0, 0.15);
							loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
							loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 1, 0.5f);
							new BukkitRunnable() {

								@Override
								public void run() {
									Block block = null;
									Location loc = player.getLocation();
									for (double i = 0; i < 8; i += 0.25) {
										loc.subtract(0, 0.25, 0);
										if (loc.getBlock().getType().isSolid()) {
											block = loc.getBlock();
											break;
										}
									}
									if (block != null) {
										Location bLoc = block.getLocation().add(0.5, 1, 0.5);
										new BukkitRunnable() {
											int t = 0;
											List<LivingEntity> affect = new ArrayList<LivingEntity>();
											@Override
											public void run() {
												t++;
												mWorld.spawnParticle(Particle.CLOUD, bLoc, 2, 0.5, 0, 0.5, 0.05);
												mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, bLoc, 2, 0.5, 0, 0.5, 0.05);
												for (LivingEntity e : EntityUtils.getNearbyMobs(bLoc, 0.65)) {
													if (!affect.contains(e)) {
														affect.add(e);
														EntityUtils.applyFreeze(mPlugin, 4, e);
														EntityUtils.damageEntity(mPlugin, e, 8, player);
													}
												}
												if (t >= 16 || player.isDead()) {
													this.cancel();
												}
											}

										}.runTaskTimer(mPlugin, 0, 5);
									}
									if (player.isOnGround()) {
										this.cancel();
										player.setInvulnerable(false);
									}
								}

							}.runTaskTimer(mPlugin, 0, 1);
						}
					}
				}
			} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {

				if (player.isSprinting()) {
					if (!player.isSneaking()) {
						int meteorStrike = ScoreboardUtils.getScoreboardValue(player, "MeteorStrike");
						/*
						 * Meteor Strike: Sprint Right click makes a meteor fall where the player is
						 * looking (this takes priority over mana lance)
						 */
						if (meteorStrike > 0) {
							if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.METEOR_STRIKE)) {
								Location loc = player.getEyeLocation();
								loc.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.85f);
								mWorld.spawnParticle(Particle.LAVA, player.getLocation(), 15, 0.25f, 0.1f, 0.25f);
								mWorld.spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.25f, 0.1f, 0.25f,
								                     0.15f);
								Vector dir = loc.getDirection().normalize();
								for (int i = 0; i < 25; i++) {
									loc.add(dir);

									player.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
									int size = EntityUtils.getNearbyMobs(loc, 2).size();
									if (loc.getBlock().getType().isSolid()) {
										launchMeteor(player, loc);
										break;
									} else if (i >= 24) {
										launchMeteor(player, loc);
										break;
									} else if (size > 0) {
										launchMeteor(player, loc);
										break;
									}
								}
								mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.METEOR_STRIKE, 20 * 12);
							}
						}
					}
				}

			}
		}
	}

	@Override
	public boolean EntityCustomDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, MagicType type,
	                                                CustomDamageEvent event) {
		if (spirits.containsKey(player)) {
			ElementalSpirit spirit = spirits.get(player);

			if (type == MagicType.FIRE || type == MagicType.ARCANE || type == MagicType.ICE) {
				spirit.setMagicType(type);
				spirit.getHurt().add(damagee);
			}
		}
		return true;
	}

	@Override
	public void PeriodicTrigger(Player player, boolean twoHertz, boolean oneSecond, boolean twoSeconds,
	                            boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		if (oneSecond) {
			int elementalSpirit = ScoreboardUtils.getScoreboardValue(player, "ElementalSpirit");

			if (elementalSpirit > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ELEMENTAL_SPIRIT)) {
					if (!spirits.containsKey(player)) {
						ElementalSpirit spirit = new ElementalSpirit(player);
						spirits.put(player, spirit);
						new BukkitRunnable() {
							float t = 0f;
							double rotation = 0;

							@Override
							public void run() {
								Location loc = player.getLocation().add(0, 1, 0);
								t += 0.1f;
								rotation += 10;
								double radian1 = Math.toRadians(rotation);
								loc.add(Math.cos(radian1), Math.sin(t) * 0.5, Math.sin(radian1));
								mWorld.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.01);
								mWorld.spawnParticle(Particle.SNOWBALL, loc, 1, 0, 0, 0, 0);

								int elementalSpirit = ScoreboardUtils.getScoreboardValue(player, "ElementalSpirit");

								BaseSpecialization spec = mPlugin.getSpecialization(player);
								if (elementalSpirit < 0 || !player.isOnline()
								    || !(spec instanceof ElementalistSpecialization)) {
									this.cancel();
									spirits.remove(player);
								}

								if (!spirits.containsKey(player)) {
									this.cancel();
								}

								ElementalSpirit spirit = spirits.get(player);
								if (spirit.getHurt().size() > 0) {
									List<LivingEntity> list = spirit.getHurt();

									LivingEntity target = null;
									if (list.size() > 0) {
										double dist = 100;
										for (LivingEntity e : list) {
											if (e.getLocation().distance(player.getLocation()) < dist) {
												dist = e.getLocation().distance(player.getLocation());
												target = e;
											}
										}
									}
									if (target != null) {
										spirit.damage(player, target, loc.clone());
										mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.ELEMENTAL_SPIRIT,
										                            20 * 6);
									}
									spirits.remove(player);
									this.cancel();
								}
								loc.subtract(Math.cos(radian1), Math.sin(t) * 0.5, Math.sin(radian1));
							}

						}.runTaskTimer(mPlugin, 0, 1);
					}
				}
			}
		}

	}

}
