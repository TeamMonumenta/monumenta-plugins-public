package pe.project.classes;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.EntityUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.MovementUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.ScoreboardUtils;

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
	private static int AMPLIFYING_1_EFFECT_DAMAGE = 5;
	private static int AMPLIFYING_2_EFFECT_DAMAGE = 7;
	private static int AMPLIFYING_RADIUS = 8;
	private static double AMPLIFYING_DOT_ANGLE = 0.33;
	private static int AMPLIFYING_1_COOLDOWN = 16 * 20;
	private static int AMPLIFYING_2_COOLDOWN = 12 * 20;
	private static float AMPLIFYING_KNOCKBACK_SPEED = 0.12f;

	private static int BLASPHEMY_RADIUS = 3;
	private static float BLASPHEMY_KNOCKBACK_SPEED = 0.5f;
	private static int BLASPHEMY_WITHER_LEVEL = 0;
	private static int BLASPHEMY_WITHER_DURATION = 8 * 20;
	private static int BLASPHEMY_1_COOLDOWN = 8 * 20;
	private static int BLASPHEMY_2_COOLDOWN = 6 * 20;

	private static int CURSED_WOUND_EFFECT_LEVEL = 1;
	private static int CURSED_WOUND_DURATION = 6 * 20;
	private static int CURSED_WOUND_RADIUS = 3;
//	private static int CURSED_WOUND_BURST_DAMAGE = 3;

	private static int GRASPING_CLAWS_RADIUS = 6;
	private static float GRASPING_CLAWS_SPEED = 0.25f;
	private static int GRASPING_CLAWS_DAMAGE = 7;
	private static int GRASPING_CLAWS_DURATION = 7 * 20;
	private static int GRASPING_CLAWS_COOLDOWN = 16 * 20;

	private static double SOUL_REND_HEAL_MULT = 0.4;
	private static int SOUL_REND_RADIUS = 7;
	private static int SOUL_REND_COOLDOWN = 6 * 20;

	private static int CONSUMING_FLAMES_1_RADIUS = 5;
	private static int CONSUMING_FLAMES_2_RADIUS = 7;
	private static int CONSUMING_FLAMES_DURATION = 7 * 20;
	private static int CONSUMING_FLAMES_COOLDOWN = 13 * 20;


	public WarlockClass(Plugin plugin, Random random) {
		super(plugin, random);
	}


	@Override
	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		_testItemsInHands(player, mainHand, offHand);
	}


/// VERSATILE MAGIC
	private void _testItemsInHands(Player player, ItemStack mainHand, ItemStack offHand) {
		mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.DAMAGE_RESISTANCE);
//		mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INCREASE_DAMAGE);
//		mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
		mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.SPEED);

		if (InventoryUtils.isWandItem(offHand)) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 0, true, false));
		}
		else if (InventoryUtils.isScytheItem(offHand)) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, 1000000, 0, true, false));
		}

	/*	if (InventoryUtils.isWandItem(mainHand)) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1000000, 0, true, false));
		}
		else if (InventoryUtils.isScytheItem(mainHand)) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, 0, true, false));
		}*/
	}


/// BLASPHEMOUS AURA
	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		if (!(damager instanceof Player)) {
			//	ABILITY: Blasphemous Aura

			int blasphemy = ScoreboardUtils.getScoreboardValue(player, "BlasphemousAura");
			if (blasphemy > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BLASPHEMY)) {
					Location loc = player.getLocation();
					World world = player.getWorld();

					ParticleUtils.playParticlesInWorld(world, Particle.SMOKE_NORMAL, loc.add(0, 1, 0), 30, 1.5, 0.6, 1.5, 0.001);
					ParticleUtils.playParticlesInWorld(world, Particle.SPELL, loc.add(0, 1, 0), 30, 1.5, 0.6, 1.5, 0.001);
					world.playSound(loc, "entity.player.attack.knockback", 0.8f, 0.6f);

					List<Entity> entities = player.getNearbyEntities(BLASPHEMY_RADIUS, BLASPHEMY_RADIUS, BLASPHEMY_RADIUS);
					for(int i = 0; i < entities.size(); i++) {
						Entity e = entities.get(i);
						if(EntityUtils.isHostileMob(e)) {
							LivingEntity mob = (LivingEntity)e;
							MovementUtils.KnockAway(player, mob, BLASPHEMY_KNOCKBACK_SPEED);
							if (blasphemy > 1) {
								mob.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, BLASPHEMY_WITHER_DURATION, BLASPHEMY_WITHER_LEVEL, true, false));
							}
						}
					}

					int bduration = (blasphemy == 1) ? BLASPHEMY_1_COOLDOWN : BLASPHEMY_2_COOLDOWN;
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BLASPHEMY, bduration);
				}
			}
		}
		return true;
	}


/// CURSED WOUND and SOUL REND
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {

	// First we need to check if we're holding a scythe, if not we can bail out of both
		if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
			// Cursed Wound
			int cursedWound = ScoreboardUtils.getScoreboardValue(player, "CursedWound");
			if (cursedWound > 0 && EntityUtils.isHostileMob(damagee)) {
				ParticleUtils.playParticlesInWorld(player.getWorld(), Particle.LAVA, damagee.getLocation().add(0, 1, 0), 4, 0.15, 0.15, 0.15, 0.0);
				damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));

				if (PlayerUtils.isCritical(player) && cursedWound > 1) {
					List<Entity> entities = damagee.getNearbyEntities(CURSED_WOUND_RADIUS, CURSED_WOUND_RADIUS, CURSED_WOUND_RADIUS);
					for(int i = 0; i < entities.size(); i++) {
						Entity e = entities.get(i);
						if(EntityUtils.isHostileMob(e)) {
							LivingEntity mob = (LivingEntity)e;
							mob.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));

							//if (mob.getPotionEffect(PotionEffectType.WITHER) != null) {
							//	EntityUtils.damageEntity(mPlugin, mob, CURSED_WOUND_BURST_DAMAGE, player);
							//}
						}
					}
				}
			}

			// Soul Rend
			int soulRend = ScoreboardUtils.getScoreboardValue(player, "SoulRend");
			if (soulRend > 0) {
				if (PlayerUtils.isCritical(player) && EntityUtils.isHostileMob(damagee)) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.SOUL_REND)) {

						double soulHealValue = damage * SOUL_REND_HEAL_MULT;

						Location loc = player.getLocation();
						World world = player.getWorld();

						if (soulRend == 1) {ParticleUtils.playParticlesInWorld(world, Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.0);}
						else if (soulRend == 2) {ParticleUtils.playParticlesInWorld(world, Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 60, 2.0, 0.75, 2.0, 0.0);}
						world.playSound(loc, "entity.magmacube.squish", 1.0f, 0.66f);
						world.playSound(loc, "entity.player.attack.crit", 1.0f, 1.2f);

						List<Entity> entities = player.getNearbyEntities(SOUL_REND_RADIUS, SOUL_REND_RADIUS, SOUL_REND_RADIUS);
						entities.add(player);
						for (Entity entity : entities) {
							if (entity instanceof Player) {
								Player p = (Player)entity;

								//	If this is us or we're allowing anyone to get it.
								if (p == player || soulRend > 1) {
									PlayerUtils.healPlayer(p, soulHealValue);
								}
							}
						}

						//	Put Soul Rend on cooldown
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SOUL_REND, SOUL_REND_COOLDOWN);
					}
				}
			}
		}

		return true;
	}


	/// GRASPING CLAWS
	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int graspingClaws = ScoreboardUtils.getScoreboardValue(player, "GraspingClaws");
		if (graspingClaws > 0) {
			if (player.isSneaking()) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.GRASPING_CLAW)) {
					Location loc = damagee.getLocation();
					World world = damagee.getWorld();

					world.playSound(loc, "block.enchantment_table.use", 1.0f, 0.8f);
					ParticleUtils.playParticlesInWorld(world, Particle.ENCHANTMENT_TABLE, loc.add(0, 1, 0), 200, 3, 3, 3, 0.0);
					ParticleUtils.playParticlesInWorld(world, Particle.DRAGON_BREATH, loc.add(0, 1, 0), 50, 0.1, 0.1, 0.1, 0.0);

					int targetCount = 1;

					List<Entity> entities = damagee.getNearbyEntities(GRASPING_CLAWS_RADIUS, GRASPING_CLAWS_RADIUS, GRASPING_CLAWS_RADIUS);
					for (Entity entity : entities) {
						if (EntityUtils.isHostileMob(entity)) {
							targetCount++;

							LivingEntity mob = (LivingEntity)entity;
							if (graspingClaws > 1) {
								EntityUtils.damageEntity(mPlugin, mob, GRASPING_CLAWS_DAMAGE, player);
							}

							if (mob != damagee) {
								MovementUtils.PullTowards(damagee, mob, GRASPING_CLAWS_SPEED);
							}
						}
					}

					if (targetCount >= 1) {
						if (targetCount >= 7) {
							targetCount = 7;
						}

						for (Entity entity : entities) {
							if (EntityUtils.isHostileMob(entity)) {
								LivingEntity mob = (LivingEntity)entity;
								mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, GRASPING_CLAWS_DURATION, targetCount, true, false));
							}
						}
					}

					damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, GRASPING_CLAWS_DURATION, targetCount, true, false));
					if (graspingClaws > 1) {EntityUtils.damageEntity(mPlugin, damagee, GRASPING_CLAWS_DAMAGE, player);}

					//	Put Soul Rend on cooldown
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.GRASPING_CLAW, GRASPING_CLAWS_COOLDOWN);
				}
			}
		}
	}



	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {

		if (player.isSneaking()) {
		/// CONSUMING FLAMES
		// Sneak and right click with a scythe
			int consumingFlames = ScoreboardUtils.getScoreboardValue(player, "ConsumingFlames");
			if (consumingFlames > 0) {
				if (InventoryUtils.isScytheItem(player.getInventory().getItemInMainHand())) {
					if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CONSUMING_FLAMES)) {

							Location loc = player.getLocation();
							World world = player.getWorld();

							ParticleUtils.playParticlesInWorld(world, Particle.FLAME, loc.add(0, 1, 0), 60, 1.75, 0.75, 1.75, 0.0);

							world.playSound(loc, "entity.magmacube.squish", 1.0f, 0.66f);

							boolean effect = false;
							int radius = (consumingFlames == 1) ? CONSUMING_FLAMES_1_RADIUS : CONSUMING_FLAMES_2_RADIUS;
							List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
							for (Entity entity : entities) {
								if (EntityUtils.isHostileMob(entity)) {
									LivingEntity mob = (LivingEntity)entity;
									mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, CONSUMING_FLAMES_DURATION, 0, true, false));
									mob.setFireTicks(CONSUMING_FLAMES_DURATION);
									effect = true;
								}
							}

							if (consumingFlames > 1 && effect) {
								mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, CONSUMING_FLAMES_DURATION, 0, true, false));
							}

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CONSUMING_FLAMES, CONSUMING_FLAMES_COOLDOWN);
						}
					}
				}
			}

		/// AMPLIFYING HEX
		//	If sneaking and left click with a wand OR a scythe

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (InventoryUtils.isScytheItem(mainHand)) {
				if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
					int amplifyingHex = ScoreboardUtils.getScoreboardValue(player, "AmplifyingHex");
					if (amplifyingHex > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.AMPLIFYING)) {
							ParticleUtils.explodingConeEffect(mPlugin, player, AMPLIFYING_RADIUS, Particle.DRAGON_BREATH, 0.6f, Particle.SMOKE_NORMAL, 0.4f, AMPLIFYING_DOT_ANGLE);
							player.getWorld().playSound(player.getLocation(), "entity.polar_bear.warning", 1.0f, 1.6f);

							Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
							List<Entity> entities = player.getNearbyEntities(AMPLIFYING_RADIUS, AMPLIFYING_RADIUS, AMPLIFYING_RADIUS);
							for(Entity e : entities) {
								if(EntityUtils.isHostileMob(e)) {
									LivingEntity mob = (LivingEntity)e;

									Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
									if (playerDir.dot(toMobVector) > AMPLIFYING_DOT_ANGLE) {
										int debuffCount = 0;

										if (mob.getPotionEffect(PotionEffectType.WITHER) != null) {debuffCount++;}
										if (mob.getPotionEffect(PotionEffectType.SLOW) != null) {debuffCount++;}
										if (mob.getPotionEffect(PotionEffectType.WEAKNESS) != null) {debuffCount++;}
										if (mob.getPotionEffect(PotionEffectType.SLOW_DIGGING) != null) {debuffCount++;}
										if (mob.getPotionEffect(PotionEffectType.POISON) != null) {debuffCount++;}

										int damageMult = (amplifyingHex == 1) ? AMPLIFYING_1_EFFECT_DAMAGE : AMPLIFYING_2_EFFECT_DAMAGE;

										if (debuffCount > 0) {
											EntityUtils.damageEntity(mPlugin, mob, debuffCount * damageMult, player);
											MovementUtils.KnockAway(player, mob, AMPLIFYING_KNOCKBACK_SPEED);
										}
									}
								}
							}

							//	Put Amplifying Hex on cooldown
							int cooldown = (amplifyingHex == 1) ? AMPLIFYING_1_COOLDOWN : AMPLIFYING_2_COOLDOWN;
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.AMPLIFYING, cooldown);
						}
					}
				}
			}
		}
	}
}
