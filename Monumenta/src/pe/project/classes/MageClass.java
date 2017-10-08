package pe.project.classes;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import pe.project.Main;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.InventoryUtils;
import pe.project.utils.MessagingUtils;
import pe.project.utils.MovementUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.ScoreboardUtils;

/*
	WandMastery
	FrostNova
	Prismatic
	Magma
	ArcaneStrike
	ArcaneStrikeHits
	Elemental
	Intellect
*/

public class MageClass extends BaseClass {
	private static int WAND_MASTERY_1 = 0;
	private static int WAND_MASTERY_2 = 1;
	
	private static int FROST_NOVA_ID = 12;
	private static float FROST_NOVA_RADIUS = 6.0f;
	private static int FROST_NOVA_1_DAMAGE = 3;
	private static int FROST_NOVA_2_DAMAGE = 6;
	private static int FROST_NOVA_1_EFFECT_LVL = 2;
	private static int FROST_NOVA_2_EFFECT_LVL = 3;
	private static int FROST_NOVA_COOLDOWN = 18 * 20;
	private static int FROST_NOVA_DURATION = 8 * 20;
	
	private static int PRISMATIC_SHIELD_ID = 13;
	private static int PRISMATIC_SHIELD_TRIGGER_HEALTH = 6;
	private static int PRISMATIC_SHIELD_EFFECT_LVL_1 = 1;
	private static int PRISMATIC_SHIELD_EFFECT_LVL_2 = 2;
	private static int PRISMATIC_SHIELD_1_DURATION = 10 * 20;
	private static int PRISMATIC_SHIELD_2_DURATION = 10 * 20;
	private static int PRISMATIC_SHIELD_1_COOLDOWN = 3 * 60 * 20;
	private static int PRISMATIC_SHIELD_2_COOLDOWN = 2 * 60 * 20;
	
	private static int MAGMA_SHIELD_ID = 14;
	private static int MAGMA_SHIELD_1_COOLDOWN = 21 * 20;
	private static int MAGMA_SHIELD_2_COOLDOWN = 21 * 20;
	private static int MAGMA_SHIELD_RADIUS = 6;
	private static int MAGMA_SHIELD_FIRE_DURATION = 4 * 20;
	private static int MAGMA_SHIELD_1_DAMAGE = 5;
	private static int MAGMA_SHIELD_2_DAMAGE = 12;
	private static float MAGMA_SHIELD_KNOCKBACK_SPEED = 0.5f;
	private static double MAGMA_SHIELD_DOT_ANGLE = 0.33;
	
	private static int ARCANE_STRIKE_ID = 15;
	private static float ARCANE_STRIKE_RADIUS = 4.0f;
	private static int ARCANE_STRIKE_1_DAMAGE = 5;
	private static int ARCANE_STRIKE_2_DAMAGE = 8;
	private static int ARCANE_STRIKE_BURN_DAMAGE = 4;
	private static int ARCANE_STRIKE_COOLDOWN = 6 * 20;
	
	private static int ELEMENTAL_ARROWS_ICE_DURATION = 8 * 20;
	private static int ELEMENTAL_ARROWS_ICE_EFFECT_LVL = 1;
	private static int ELEMENTAL_ARROWS_FIRE_DURATION = 5 * 20;
	private static double ELEMENTAL_ARROWS_RADIUS = 3.0;
	
	private static int INTELLECT_XP = 3;
	private static int INTELLECT_AS_COOLDOWN = 4 * 20;
	private static int INTELLECT_FN_COOLDOWN = 8 * 20;
	private static int INTELLECT_MS_1_COOLDOWN = 14 * 20;
	private static int INTELLECT_MS_2_COOLDOWN = 14 * 20;
	
	public MageClass(Main plugin, Random random) {
		super(plugin, random);
	}
	
	@Override
	public void setupClassPotionEffects(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		_testItemInHand(player, mainHand);
	}
	
	@Override
	public void AbilityOffCooldown(Player player, int abilityID) {
		if (abilityID == FROST_NOVA_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Frost Nova is now off cooldown");
		} else if (abilityID == MAGMA_SHIELD_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Magma Shield is now off cooldown");
		}else if (abilityID == PRISMATIC_SHIELD_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Prismatic Shield is now off cooldown");
		}
	}
	
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		//	Arcane Strike
		{
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (InventoryUtils.isWandItem(mainHand)) {
				int arcaneStrike = ScoreboardUtils.getScoreboardValue(player, "ArcaneStrike");
				if (arcaneStrike > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), ARCANE_STRIKE_ID)) {	
						int extraDamage = arcaneStrike == 1 ? ARCANE_STRIKE_1_DAMAGE : ARCANE_STRIKE_2_DAMAGE;
						
						List<Entity> entities = damagee.getNearbyEntities(ARCANE_STRIKE_RADIUS, ARCANE_STRIKE_RADIUS, ARCANE_STRIKE_RADIUS);
						entities.add(damagee);
						for(Entity e : entities) {
							if(e instanceof Monster) {
								Monster mob = (Monster)e;
								int dmg = extraDamage;
								
								if (arcaneStrike > 1 && mob.getFireTicks() > 0) {
									dmg += ARCANE_STRIKE_BURN_DAMAGE;
								}
								
								mob.damage(dmg);
							}
						}
						
						World world = Bukkit.getWorld(player.getWorld().getName());
						Location loc = damagee.getLocation();
						ParticleUtils.playParticlesInWorld(world, Particle.EXPLOSION_NORMAL, loc.add(0, 1, 0), 50, 2.5, 1, 2.5, 0.001);
						ParticleUtils.playParticlesInWorld(world, Particle.SPELL_WITCH, loc.add(0, 1, 0), 200, 2.5, 1, 2.5, 0.001);
						
						world.playSound(loc, "entity.enderdragon_fireball.explode", 0.5f, 1.5f);
						
						boolean intellectBonus = ScoreboardUtils.getScoreboardValue(player, "Intellect") == 2;
						int cooldown = intellectBonus ? INTELLECT_AS_COOLDOWN : ARCANE_STRIKE_COOLDOWN;
						
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), ARCANE_STRIKE_ID, cooldown);
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		_testItemInHand(player, mainHand);
	}
	
	@Override
	public void PlayerDropItemEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		_testItemInHand(player, mainHand);
	}
	
	@Override
	public void PlayerItemBreakEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		_testItemInHand(player, mainHand);
	}

	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause) {
		int intellect = ScoreboardUtils.getScoreboardValue(player, "Intellect");
		if (intellect > 0) {
			player.giveExp(INTELLECT_XP);
		}
	}
	
	@Override
	public void PlayerInteractEvent(Player player, Action action, Material material) {
		//	Magma Shield
		{
			//	If we're sneaking and we block with a shield we can attempt to trigger the ability.
			if (player.isSneaking()) {
				ItemStack offHand = player.getInventory().getItemInOffHand();
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if ((offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD)
						&& (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
					
					int magmaShield = ScoreboardUtils.getScoreboardValue(player, "Magma");
					if (magmaShield > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), MAGMA_SHIELD_ID)) {
							Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
							
							new BukkitRunnable() {
								Integer tick = 0;
								public void run() {
									if (++tick == 5) {
										if (player.isBlocking()) {
											List<Entity> entities = player.getNearbyEntities(MAGMA_SHIELD_RADIUS, MAGMA_SHIELD_RADIUS, MAGMA_SHIELD_RADIUS);
											for(Entity e : entities) {
												if(e instanceof Monster) {
													Monster mob = (Monster)e;
													
													Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
													if (playerDir.dot(toMobVector) > MAGMA_SHIELD_DOT_ANGLE) {
														MovementUtils.KnockAway(player, mob, MAGMA_SHIELD_KNOCKBACK_SPEED);
														mob.setFireTicks(MAGMA_SHIELD_FIRE_DURATION);
														
														int extraDamage = magmaShield == 1 ? MAGMA_SHIELD_1_DAMAGE : MAGMA_SHIELD_2_DAMAGE;
														mob.damage(extraDamage);
													}
												}
											}
											
											ParticleUtils.explodingConeEffect(mPlugin, player, MAGMA_SHIELD_RADIUS, Particle.FLAME, 0.75f, Particle.LAVA, 0.25f, MAGMA_SHIELD_DOT_ANGLE);
											
											boolean intellectBonus = ScoreboardUtils.getScoreboardValue(player, "Intellect") == 2;
											
											int cooldown = intellectBonus ? (magmaShield == 1 ? INTELLECT_MS_1_COOLDOWN : INTELLECT_MS_2_COOLDOWN) :
														(magmaShield == 1 ? MAGMA_SHIELD_1_COOLDOWN : MAGMA_SHIELD_2_COOLDOWN);
											
											
											mPlugin.mTimers.AddCooldown(player.getUniqueId(), MAGMA_SHIELD_ID, cooldown);

										}
										this.cancel();
									}
								}
							}.runTaskTimer(mPlugin, 0, 1);	
						}
					}
				}
			}
		}
		
		//	Frost Nova
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if (InventoryUtils.isWandItem(mainHand)) {
					int frostNova = ScoreboardUtils.getScoreboardValue(player, "FrostNova");
					if (frostNova > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), FROST_NOVA_ID)) {
							List<Entity> entities = player.getNearbyEntities(FROST_NOVA_RADIUS, FROST_NOVA_RADIUS, FROST_NOVA_RADIUS);
							entities.add(player);
							for(int i = 0; i < entities.size(); i++) {
								Entity e = entities.get(i);
								if (e instanceof Monster) {
									Monster mob = (Monster)(e);
										
										int extraDamage = frostNova == 1 ? FROST_NOVA_1_DAMAGE : FROST_NOVA_2_DAMAGE;
										int effectLevel = frostNova == 1 ? FROST_NOVA_1_EFFECT_LVL : FROST_NOVA_2_EFFECT_LVL;
										
										mob.damage(extraDamage);
										mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_NOVA_DURATION, effectLevel, true, false));
										
										mob.setFireTicks(0);
								} else if (e instanceof Player) {
									e.setFireTicks(0);
								}
							}
							
							int intellect = ScoreboardUtils.getScoreboardValue(player, "Intellect");
							int cooldown = intellect < 2 ? FROST_NOVA_COOLDOWN : INTELLECT_FN_COOLDOWN;
							
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), FROST_NOVA_ID, cooldown);
							
							World world = Bukkit.getWorld(player.getWorld().getName());
							Location loc = player.getLocation();
							ParticleUtils.playParticlesInWorld(world, Particle.SNOW_SHOVEL, loc.add(0, 1, 0), 400, 4, 1, 4, 0.001);
							ParticleUtils.playParticlesInWorld(world, Particle.CRIT_MAGIC, loc.add(0, 1, 0), 200, 4, 1, 4, 0.001);
							
							world.playSound(loc, "block.glass.break", 0.5f, 1.0f);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//	Elemental Arrows
		{
			int elementalArrows = ScoreboardUtils.getScoreboardValue(player, "Elemental");
			if (elementalArrows > 0) {
				if (arrow.hasMetadata("FireArrow")) {
					damagee.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
					
					if (elementalArrows == 2) {
						List<Entity> entities = damagee.getNearbyEntities(ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS);
						for (Entity entity : entities) {
							if (entity instanceof Monster) {
								Monster mob = (Monster)entity;
								mob.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
							}
						}
					}
				}
				else if (arrow.hasMetadata("IceArrow")) {
					damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));
				
					if (elementalArrows == 2) {
						List<Entity> entities = damagee.getNearbyEntities(ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS);
						for (Entity entity : entities) {
							if (entity instanceof Monster) {
								Monster mob = (Monster)entity;
								mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		//	Elemental Arrows
		{
			int elementalArrows = ScoreboardUtils.getScoreboardValue(player, "Elemental");
			if (elementalArrows > 0) {
				//	If sneaking, Ice Arrow
				if (player.isSneaking()) {
					arrow.setFireTicks(0);
					arrow.setMetadata("IceArrow", new FixedMetadataValue(mPlugin, 0));
					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SNOW_SHOVEL);
				}
				//	else Fire Arrow
				else {
					arrow.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
					arrow.setMetadata("FireArrow", new FixedMetadataValue(mPlugin, 0));
					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
				}
			}
		}
	}
	
	@Override
	public void PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		//	Prismatic Shield
		{
			double correctHealth = player.getHealth() - damage;
			if (correctHealth > 0 && correctHealth <= PRISMATIC_SHIELD_TRIGGER_HEALTH) {
				int prismatic = ScoreboardUtils.getScoreboardValue(player, "Prismatic");
				if (prismatic > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), PRISMATIC_SHIELD_ID)) {
						int effectLevel = prismatic == 1 ? PRISMATIC_SHIELD_EFFECT_LVL_1 : PRISMATIC_SHIELD_EFFECT_LVL_2;
						int duration = prismatic == 1 ? PRISMATIC_SHIELD_1_DURATION : PRISMATIC_SHIELD_2_DURATION;
						
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.ABSORPTION, duration, effectLevel, true, false));
						
						int cooldown = prismatic == 1 ? PRISMATIC_SHIELD_1_COOLDOWN : PRISMATIC_SHIELD_2_COOLDOWN;
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), PRISMATIC_SHIELD_ID, cooldown);
					}
				}
			}
		}	
	}
	
	private void _testItemInHand(Player player, ItemStack mainHand) {
		//	Wand Mastery
		int wandMastery = ScoreboardUtils.getScoreboardValue(player, "WandMastery");
		if (wandMastery > 0) {
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INCREASE_DAMAGE);
			
			if (InventoryUtils.isWandItem(mainHand)) {
				int strengthAmp = wandMastery == 1 ? WAND_MASTERY_1 : WAND_MASTERY_2;
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1000000, strengthAmp, true, false));
			}
		}
	}
}
