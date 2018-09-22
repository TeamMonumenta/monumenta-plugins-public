package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

/*
    AmplifyingHex
    Blasphemy
    CursedWound
    GraspingClaws
    SoulRend
    VersatileMagic
    ConsumingFlame
*/

public class WarlockClass extends BaseClass {
	private static final int AMPLIFYING_1_EFFECT_DAMAGE = 5;
	private static final int AMPLIFYING_2_EFFECT_DAMAGE = 7;
	private static final int AMPLIFYING_RADIUS = 8;
	private static final double AMPLIFYING_DOT_ANGLE = 0.33;
	private static final int AMPLIFYING_1_COOLDOWN = 12 * 20;
	private static final int AMPLIFYING_2_COOLDOWN = 10 * 20;
	private static final float AMPLIFYING_KNOCKBACK_SPEED = 0.12f;

	private static final List<PotionEffectType> DEBUFFS = Arrays.asList(
	                                                          PotionEffectType.WITHER,
	                                                          PotionEffectType.SLOW,
	                                                          PotionEffectType.WEAKNESS,
	                                                          PotionEffectType.SLOW_DIGGING,
	                                                          PotionEffectType.POISON,
	                                                          PotionEffectType.UNLUCK
	                                                      );

	private static final int BLASPHEMY_RADIUS = 3;
	private static final float BLASPHEMY_KNOCKBACK_SPEED = 0.3f;
	private static final int BLASPHEMY_1_VULN_LEVEL = 3;
	private static final int BLASPHEMY_2_VULN_LEVEL = 5;
	private static final int BLASPHEMY_VULN_DURATION = 6 * 20;

	private static final int CURSED_WOUND_EFFECT_LEVEL = 1;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final int CURSED_WOUND_1_DAMAGE = 1;
	private static final int CURSED_WOUND_2_DAMAGE = 2;

	private static final int GRASPING_CLAWS_RADIUS = 6;
	private static final float GRASPING_CLAWS_SPEED = 0.175f;
	private static final int GRASPING_CLAWS_EFFECT_LEVEL = 3;
	private static final int GRASPING_CLAWS_1_DAMAGE = 3;
	private static final int GRASPING_CLAWS_2_DAMAGE = 7;
	private static final int GRASPING_CLAWS_DURATION = 8 * 20;
	private static final int GRASPING_CLAWS_COOLDOWN = 16 * 20;

	private static final double SOUL_REND_HEAL_1_MULT = 0.4;
	private static final double SOUL_REND_HEAL_2_MULT = 0.5;
	private static final int SOUL_REND_RADIUS = 7;
	private static final int SOUL_REND_COOLDOWN = 6 * 20;

	private static final int CONSUMING_FLAMES_1_RADIUS = 5;
	private static final int CONSUMING_FLAMES_2_RADIUS = 7;
	private static final int CONSUMING_FLAMES_DAMAGE = 1;
	private static final int CONSUMING_FLAMES_DURATION = 7 * 20;
	private static final int CONSUMING_FLAMES_COOLDOWN = 10 * 20;

	private static final int PASSIVE_DURATION = 6 * 20;

	public WarlockClass(Plugin plugin, Random random) {
		super(plugin, random);
	}

	// PASSIVE
	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		if (EntityUtils.isHostileMob(killedEntity)) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();

			if (InventoryUtils.isScytheItem(mainHand) || InventoryUtils.isScytheItem(offHand)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, PASSIVE_DURATION, 0, false, true));
			}
		}
	}

	// BLASPHEMOUS AURA
	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		if (!(damager instanceof Player)) {
			// ABILITY: Blasphemous Aura
			if (damager instanceof Skeleton) {
				Skeleton skelly = (Skeleton)damager;
				ItemStack mainHand = skelly.getEquipment().getItemInMainHand();
				if (mainHand != null && mainHand.getType() == Material.BOW) {
					return true;
				}
			}

			int blasphemy = ScoreboardUtils.getScoreboardValue(player, "BlasphemousAura");
			if (blasphemy > 0) {
				Location loc = player.getLocation();
				World world = player.getWorld();

				world.spawnParticle(Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 30, 1.5, 0.6, 1.5, 0.001);
				world.spawnParticle(Particle.SPELL, loc.add(0, 1, 0), 30, 1.5, 0.6, 1.5, 0.001);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.6f);

				MovementUtils.KnockAway(player, damager, BLASPHEMY_KNOCKBACK_SPEED);
				int vulnLevel = (blasphemy == 1) ? BLASPHEMY_1_VULN_LEVEL : BLASPHEMY_2_VULN_LEVEL;

				for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), BLASPHEMY_RADIUS)) {
					mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, BLASPHEMY_VULN_DURATION, vulnLevel, false, true));
				}
			}
		}
		return true;
	}

	// CURSED WOUND and SOUL REND
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {

		// First we need to check if we're holding a scythe, if not we can bail out of both
		if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
			// Cursed Wound
			int cursedWound = ScoreboardUtils.getScoreboardValue(player, "CursedWound");
			if (cursedWound > 0) {
				if (EntityUtils.isHostileMob(damagee)) {
					player.getWorld().spawnParticle(Particle.LAVA, damagee.getLocation().add(0, 1, 0), 4, 0.15, 0.15, 0.15, 0.0);
					damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, false, true));
					int damageMult = (cursedWound == 1) ? CURSED_WOUND_1_DAMAGE : CURSED_WOUND_2_DAMAGE;
					EntityUtils.damageEntity(mPlugin, damagee, damageMult, player);
				}

				if (cursedWound > 1 && PlayerUtils.isCritical(player)) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), CURSED_WOUND_RADIUS)) {
						mob.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));
					}
				}
			}

			// Soul Rend
			int soulRend = ScoreboardUtils.getScoreboardValue(player, "SoulRend");
			if (soulRend > 0) {
				if (PlayerUtils.isCritical(player) && EntityUtils.isHostileMob(damagee)) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.SOUL_REND)) {
						double healMult = (soulRend == 1) ? SOUL_REND_HEAL_1_MULT : SOUL_REND_HEAL_2_MULT;
						double soulHealValue = damage * healMult;

						Location loc = player.getLocation();
						World world = player.getWorld();

						if (soulRend == 1) {
							world.spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0);
						} else if (soulRend == 2) {
							world.spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 60, 2.0, 0.75, 2.0, 0.0);
						}

						world.playSound(loc, Sound.ENTITY_MAGMA_CUBE_SQUISH, 1.0f, 0.66f);
						world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);

						for (Player p : PlayerUtils.getNearbyPlayers(player, SOUL_REND_RADIUS, true)) {
							// If this is us or we're allowing anyone to get it.
							if (p == player || soulRend > 1) {
								PlayerUtils.healPlayer(p, soulHealValue);
							}
						}

						// Put Soul Rend on cooldown
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SOUL_REND, SOUL_REND_COOLDOWN);
					}
				}
			}
		}
		return true;
	}



	// GRASPING CLAWS
	@Override
	public void ProjectileHitEvent(Player player, Arrow arrow) {
		int graspingClaws = ScoreboardUtils.getScoreboardValue(player, "GraspingClaws");
		if (graspingClaws > 0) {
			if (player.isSneaking()) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.GRASPING_CLAW)) {

					Location loc = arrow.getLocation();
					World world = arrow.getWorld();

					world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
					world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc.add(0, 1, 0), 200, 3, 3, 3, 0.0);
					world.spawnParticle(Particle.DRAGON_BREATH, loc, 75, 1, 1, 1, 0.0);

					int damage = (graspingClaws == 1) ? GRASPING_CLAWS_1_DAMAGE : GRASPING_CLAWS_2_DAMAGE;

					for (LivingEntity mob : EntityUtils.getNearbyMobs(arrow.getLocation(), GRASPING_CLAWS_RADIUS)) {
						EntityUtils.damageEntity(mPlugin, mob, damage, player);
						MovementUtils.PullTowards(arrow, mob, GRASPING_CLAWS_SPEED);
						mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, GRASPING_CLAWS_DURATION, GRASPING_CLAWS_EFFECT_LEVEL, false, true));
						EntityUtils.damageEntity(mPlugin, mob, damage, player);
					}

					// Put Grasping Claws on cooldown
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.GRASPING_CLAW, GRASPING_CLAWS_COOLDOWN);
				}
			}
		}
	}



	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (player.isSneaking()) {
			/// CONSUMING FLAMES - Sneak and right click with a scythe
			int consumingFlames = ScoreboardUtils.getScoreboardValue(player, "ConsumingFlames");
			if (consumingFlames > 0) {
				if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
					if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CONSUMING_FLAMES)) {
							Location loc = player.getLocation();
							World world = player.getWorld();

							world.spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 60, 1.75, 0.75, 1.75, 0.0);
							world.playSound(loc, Sound.ENTITY_MAGMA_CUBE_SQUISH, 1.0f, 0.66f);

							boolean effect = false;
							int radius = (consumingFlames == 1) ? CONSUMING_FLAMES_1_RADIUS : CONSUMING_FLAMES_2_RADIUS;
							for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), radius)) {
								mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, CONSUMING_FLAMES_DURATION, 0, false, true));
								mob.setFireTicks(CONSUMING_FLAMES_DURATION);

								EntityUtils.damageEntity(mPlugin, mob, CONSUMING_FLAMES_DAMAGE, player);
								effect = true;
							}

							if (consumingFlames > 1 && effect) {
								mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, CONSUMING_FLAMES_DURATION, 0, false, true));
							}

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CONSUMING_FLAMES, CONSUMING_FLAMES_COOLDOWN);
						}
					}
				}
			}

			// AMPLIFYING HEX
			// If sneaking and left click with a wand OR a scythe
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if (InventoryUtils.isScytheItem(mainHand)) {
					int amplifyingHex = ScoreboardUtils.getScoreboardValue(player, "AmplifyingHex");
					if (amplifyingHex > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.AMPLIFYING)) {
							ParticleUtils.explodingConeEffect(mPlugin, player, AMPLIFYING_RADIUS, Particle.DRAGON_BREATH, 0.6f, Particle.SMOKE_NORMAL, 0.4f, AMPLIFYING_DOT_ANGLE);
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 1.6f);

							Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
							for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), AMPLIFYING_RADIUS)) {
								Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
								if (playerDir.dot(toMobVector) > AMPLIFYING_DOT_ANGLE) {
									int debuffCount = (int)DEBUFFS.stream()
													  .filter(effect -> (mob.getPotionEffect(effect) != null))
													  .count();
									int damageMult = (amplifyingHex == 1) ? AMPLIFYING_1_EFFECT_DAMAGE : AMPLIFYING_2_EFFECT_DAMAGE;
									if (consumingFlames > 1 && mob.getFireTicks() > 0) {
										debuffCount++;
									}
									if (debuffCount > 0) {
										EntityUtils.damageEntity(mPlugin, mob, debuffCount * damageMult, player);
										MovementUtils.KnockAway(player, mob, AMPLIFYING_KNOCKBACK_SPEED);
									}
								}
							}

							// Put Amplifying Hex on cooldown
							int cooldown = (amplifyingHex == 1) ? AMPLIFYING_1_COOLDOWN : AMPLIFYING_2_COOLDOWN;
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.AMPLIFYING, cooldown);
						}
					}
				}
			}
		}
	}
}
