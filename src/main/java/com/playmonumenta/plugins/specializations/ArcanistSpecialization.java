package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

import java.util.Random;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

public class ArcanistSpecialization extends BaseSpecialization {
	private static final int ARCANIST_INSIGHT = (int)(1 * 20);
	private static final int FSWORD_1_DAMAGE = 3;
	private static final int FSWORD_2_DAMAGE = 5;
	private static final int FSWORD_1_SWIPE = 3;
	private static final int FSWORD_2_SWIPE = 3;
	private static final int FSWORD_RADIUS = 5;
	private static final int FSWORD_COOLDOWN = 8;
	private static final float FSWORD_KNOCKBACK_SPEED = 0.12f;
	private static final double FSWORD_DOT_ANGLE = 0.33;
	private static final Particle.DustOptions FSWORD_COLOR1 = new Particle.DustOptions(Color.fromRGB(106, 203, 255), 1.0f);
	private static final Particle.DustOptions FSWORD_COLOR2 = new Particle.DustOptions(Color.fromRGB(168, 226, 255), 1.0f);

	private World mWorld;

	public ArcanistSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	@Override
	public boolean EntityCustomDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage,
	                                                MagicType type, CustomDamageEvent event) {
		int overload = ScoreboardUtils.getScoreboardValue(player, "Overload");
		/*
		 * Overload: Your spells deal an additional 1.5 damage
		 * for each other spell already on cooldown.
		 * At Level 2, the extra damage is increased to 3.
		 */
		if (overload > 0) {
			Set<Spells> cds = mPlugin.mTimers.getCooldowns(player.getUniqueId());
			if (cds != null) {
				if (cds.size() > 0) {
					int mult = cds.size();
					double dmg = overload == 1 ? 1.5 : 3;
					event.setDamage(event.getDamage() + (dmg * mult));
				}
			}
		}
		return false;
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		int flashSword = ScoreboardUtils.getScoreboardValue(player, "FlashSword");
		/*
		 * Flash Sword: Sprint left clicking with a wand causes a wave of Arcane blades
		 * to cut down all the foes in your path. Each enemy within a 4 block cone takes
		 * 3 damage 2 times in rapid succession, being knocked back with the last swipe.
		 * At level 2 this abilities damage increases to 4 damage 3 times. (CD: 8
		 * seconds)
		 */
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSprinting()) {
				if (InventoryUtils.isWandItem(mainHand)) {
					if (flashSword > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.FSWORD)) {
							int swings = flashSword == 1 ? 2 : 3;
							new BukkitRunnable() {
								int t = 0;
								float pitch = 1.2f;
								int sw = 0;
								@Override
								public void run() {
									t++;
									sw++;
									Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
									for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(),
									                                                  FSWORD_RADIUS)) {
										Vector toMobVector = mob.getLocation().toVector()
										                     .subtract(player.getLocation().toVector()).setY(0).normalize();
										if (playerDir.dot(toMobVector) > FSWORD_DOT_ANGLE) {
											int damageMult = (flashSword == 1) ? FSWORD_1_DAMAGE : FSWORD_2_DAMAGE;
											mob.setNoDamageTicks(0);
											EntityUtils.damageEntity(mPlugin, mob, damageMult, player);
											if (t >= swings) {
												MovementUtils.KnockAway(player, mob, 0.4f);
											}
										}
									}

									if (t >= swings) {
										pitch = 1.45f;
									}
									player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP,
									                            1.0f, 0.8f);
									player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT,
									                            1.0f, pitch);
									Location loc = player.getLocation();
									new BukkitRunnable() {
										final int i = sw;
										double roll;
										double d = 45;
										boolean init = false;
										@Override
										public void run() {
											if (!init) {
												if (i % 2 == 0) {
													roll = -8;
													d = 45;
												} else {
													roll = 8;
													d = 135;
												}
												init = true;
											}
											if (i % 2 == 0) {
												Vector vec;
												for (double r = 1; r < 5; r += 0.5) {
													for (double degree = d; degree < d + 30; degree += 5) {
														double radian1 = Math.toRadians(degree);
														vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
														vec = VectorUtils.rotateZAxis(vec, roll);
														vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
														vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

														Location l = loc.clone().add(0, 1.25, 0).add(vec);
														mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR1);
														mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR2);
													}
												}

												d += 30;
											} else {
												Vector vec;
												for (double r = 1; r < 5; r += 0.5) {
													for (double degree = d; degree > d - 30; degree -= 5) {
														double radian1 = Math.toRadians(degree);
														vec = new Vector(Math.cos(radian1) * r, 0, Math.sin(radian1) * r);
														vec = VectorUtils.rotateZAxis(vec, roll);
														vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
														vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

														Location l = loc.clone().add(0, 1.25, 0).add(vec);
														mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR1);
														mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, FSWORD_COLOR2);
													}
												}
												d -= 30;
											}

											if ((d >= 135 && i % 2 == 0) || (d <= 45 && i % 2 > 0)) {
												this.cancel();
											}
										}

									}.runTaskTimer(mPlugin, 0, 1);
									if (t >= swings) {
										this.cancel();
									}
								}

							}.runTaskTimer(mPlugin, 0, 7);
							PlayerUtils.callAbilityCastEvent(player, Spells.FSWORD);
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.FSWORD, 20 * 8);
						}
					}
				}
			}
		}
	}

	@Override
	public void AbilityCastEvent(Player player, AbilityCastEvent event) {
		int Insight = ScoreboardUtils.getScoreboardValue(player, "SagesInsight");
		if (Insight > 0) {
			int timeReduction = ARCANIST_INSIGHT;
			mPlugin.mTimers.UpdateCooldowns(player, timeReduction);
		}
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		int Insight = ScoreboardUtils.getScoreboardValue(player, "SagesInsight");
		if (Insight > 1) {
			int timeReduction = ARCANIST_INSIGHT;
			mPlugin.mTimers.UpdateCooldowns(player, timeReduction);
		}
		return true;
	}
}
