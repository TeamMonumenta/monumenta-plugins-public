package pe.project.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import pe.project.Main;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.EntityUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.MessagingUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.ScoreboardUtils;

/*
	Agility
	Swiftness
	Exploration
	BowMastery
	Tinkering
	Volley
	StandardBearer
*/

public class ScoutClass extends BaseClass {
	private static int SWIFTNESS_EFFECT_SPEED_LVL = 0;
	private static int SWIFTNESS_EFFECT_JUMP_LVL = 2;
	
	private static int AGILITY_1_EFFECT_LVL = 0;
	private static int AGILITY_2_EFFECT_LVL = 1;
	private static int AGILITY_1_RESISTANCE_LEVEL = 0;
	private static int AGILITY_2_RESISTANCE_LEVEL = 1;
	
	private static int BOW_MASTER_1_DAMAGE = 3;
	private static int BOW_MASTER_2_DAMAGE = 6;
	
	private static int EAGLE_EYE_ID = 64;
	private static String EAGLE_EYE_TAG_NAME = "TagEagleEye";
	private static int EAGLE_EYE_EFFECT_LVL = 0;
	private static int EAGLE_EYE_DURATION = 10 * 20;
	private static int EAGLE_EYE_COOLDOWN = 30 * 20;
	private static int EAGLE_EYE_1_EXTRA_DAMAGE = 3;
	private static int EAGLE_EYE_2_EXTRA_DAMAGE = 6;
	private static int EAGLE_EYE_RADIUS = 20;
	private static double EAGLE_EYE_DOT_ANGLE = 0.33;
	
	private static int VOLLEY_ID = 65;
	private static int VOLLEY_COOLDOWN = 15 * 20;
	private static int VOLLEY_1_ARROW_COUNT = 7;
	private static int VOLLEY_2_ARROW_COUNT = 10;
	private static double VOLLEY_1_DAMAGE_INCREASE = 0.75;
	private static double VOLLEY_2_DAMAGE_INCREASE = 1.5;
	
	private static int STANDARD_BEARER_ID = 67;
	public static int STANDARD_BEARER_FAKE_ID = 10078;
	public static String STANDARD_BEARER_TAG_NAME = "TagBearer";
	private static double STANDARD_BEARER_ARMOR = 2;
	private static double STANDARD_BEARER_TRIGGER_RANGE = 2;
	private static int STANDARD_BEARER_COOLDOWN = 60 * 20;
	private static int STANDARD_BEARER_DURATION = 30 * 20;
	private static int STANDARD_BEARER_TRIGGER_RADIUS = 12;
	private static double STANDARD_BEARER_DAMAGE_MULTIPLIER = 1.25;
	
	public ScoutClass(Main plugin, Random random) {
		super(plugin, random);
	}
	
	@Override
	public void setupClassPotionEffects(Player player) {
		_testForAgility(player);
		_testForSwiftness(player);
		
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		_testItemInHand(player, mainHand);
	}
	
	@Override
	public void AbilityOffCooldown(Player player, int abilityID) {
		if (abilityID == VOLLEY_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Volley is now off cooldown");
		} else if (abilityID == STANDARD_BEARER_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Standard Bearer is now off cooldown");
		} else if (abilityID == EAGLE_EYE_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Eagle Eye is now off cooldown");
		}
	}
	
	@Override
	public void PlayerRespawnEvent(Player player) {
		_testForAgility(player);
		_testForSwiftness(player);
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
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		{
			//	Volley
			int volley = ScoreboardUtils.getScoreboardValue(player, "Volley");
			if (volley > 0) {
				if (arrow.hasMetadata("Volley")) {
					double damageMultiplier = volley == 1 ? VOLLEY_1_DAMAGE_INCREASE : VOLLEY_2_DAMAGE_INCREASE;
					double oldDamage = event.getDamage();
					
					double newDamage = oldDamage + (oldDamage * damageMultiplier);
					event.setDamage(newDamage);
				}
			}
		}
		{
			//	Bow Mastery
			int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
			if (bowMastery > 0) {
				if (arrow.isCritical()) {
					int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
					damagee.damage(bonusDamage);
				}
			}
		}
	}
	
	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		List<Projectile> projectiles;
		
		boolean wasCritical = arrow.isCritical();
		
		//	Volley
		if (player.isSneaking()) {
			int volley = ScoreboardUtils.getScoreboardValue(player, "Volley");
			if (volley > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), VOLLEY_ID)) {
					boolean isTippedArrow = arrow instanceof TippedArrow;
					boolean isCustomArrow = false;
					boolean slownessArrow = false;
					boolean weaknessArrow = false;
					boolean isCritical = arrow.isCritical();
					int fireTicks = arrow.getFireTicks();
					int knockbackStrength = arrow.getKnockbackStrength();
					
					if (isTippedArrow) {
						TippedArrow tArrow = (TippedArrow)arrow;
						isCustomArrow = tArrow.hasCustomEffects();
						
						if (!isCustomArrow) {
							PotionData baseData = tArrow.getBasePotionData();
	
							if (baseData.getType() == PotionType.SLOWNESS) {
								slownessArrow = true;
							} else if (baseData.getType() == PotionType.WEAKNESS) {
								weaknessArrow = true;
							}
						} else {
							if (tArrow.hasCustomEffect(PotionEffectType.SLOW)) {
								slownessArrow = true;
							} else if (tArrow.hasCustomEffect(PotionEffectType.WEAKNESS)) {
								weaknessArrow = true;
							}
						}
					}
					
					int numArrows = volley == 1 ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;
					
					if (slownessArrow || weaknessArrow) {
						projectiles = EntityUtils.spawnTippedArrowVolley(mPlugin, player, numArrows, 1.5, 5);
					} else {
						projectiles = EntityUtils.spawnArrowVolley(mPlugin, player, numArrows, 1.5, 5);
					}
	
					for (Projectile proj : projectiles) {
						Arrow _arrow = (Arrow)proj;
						
						proj.setMetadata("Volley", new FixedMetadataValue(mPlugin, 0));
						
						if (isTippedArrow) {
							TippedArrow baseArrow = (TippedArrow)arrow;
							TippedArrow tArrow = (TippedArrow)proj;
							
							if (!isCustomArrow) {
								PotionData baseData = baseArrow.getBasePotionData();
	
								if (slownessArrow) {
									PotionData data = new PotionData(PotionType.SLOWNESS, baseData.isExtended(), baseData.isUpgraded());
									tArrow.setBasePotionData(data);
								} else if (weaknessArrow) {
									PotionData data = new PotionData(PotionType.WEAKNESS, baseData.isExtended(), baseData.isUpgraded());
									tArrow.setBasePotionData(data);
								}
							} else {
								List<PotionEffect> effects = baseArrow.getCustomEffects();
								
								for (PotionEffect effect : effects) {		
									if (effect.getType().getName() == PotionEffectType.SLOW.getName() || effect.getType().getName() == PotionEffectType.WEAKNESS.getName()) {			
										PotionEffect newEffect = new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), false, false);
										tArrow.addCustomEffect(newEffect, true);
									}
								}
							}
						}
						
						_arrow.setCritical(isCritical);
						_arrow.setFireTicks(fireTicks);
						_arrow.setKnockbackStrength(knockbackStrength);
						
						mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);
					}
					
					//	I hate this so much, you don't even know... [Rock]
					Location jankWorkAround = player.getLocation();
					jankWorkAround.setY(-15);
					arrow.teleport(jankWorkAround);
					
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), VOLLEY_ID, VOLLEY_COOLDOWN);
				} else {
					projectiles = new ArrayList<Projectile>();
					projectiles.add(arrow);	
				}
			} else {
				projectiles = new ArrayList<Projectile>();
				projectiles.add(arrow);	
			}
		} else {
			projectiles = new ArrayList<Projectile>();
			projectiles.add(arrow);
		}
		
		//	Bow Mastery
		int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
		if (bowMastery > 0) {
			if (wasCritical) {
				for (Projectile proj : projectiles) {
					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.CLOUD);
				}
			}
		}
	}
	
	@Override
	public void ProjectileHitEvent(Player player, Arrow arrow) {
		int standardBearer = ScoreboardUtils.getScoreboardValue(player, "StandardBearer");
		if (standardBearer > 0 && player.getGameMode() != GameMode.ADVENTURE) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), STANDARD_BEARER_ID)) {
				double range = arrow.getLocation().distance(player.getLocation());
				if (range <= STANDARD_BEARER_TRIGGER_RANGE) {
					mPlugin.mPulseEffectTimers.AddPulseEffect(player, this, STANDARD_BEARER_ID, STANDARD_BEARER_TAG_NAME, STANDARD_BEARER_DURATION, player.getLocation(), STANDARD_BEARER_TRIGGER_RADIUS);
					
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), STANDARD_BEARER_ID, STANDARD_BEARER_COOLDOWN);
				}
			}
		}
	}
	
	@Override
	public void PulseEffectApplyEffect(Player owner, Location loc, Player effectedPlayer, int abilityID) {
		//	StandardBearer
		{
			if (abilityID == STANDARD_BEARER_ID) {
				int standardBearer = ScoreboardUtils.getScoreboardValue(owner, "StandardBearer");
				if (standardBearer > 0) {
					double x = loc.getX();
					double y = loc.getY() + 1;
					double z = loc.getZ();
					Location newLoc = new Location(loc.getWorld(), x, y, z);
					
					ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.VILLAGER_HAPPY, newLoc, 15, 0.75, 0.75, 0.75, 0.001);
					
					if (standardBearer > 1) {
						AttributeInstance att = effectedPlayer.getAttribute(Attribute.GENERIC_ARMOR);
						double baseValue = att.getBaseValue(); 
						att.setBaseValue(Math.min(baseValue + STANDARD_BEARER_ARMOR, 30));
					}
				}
			}
		}
	}
	
	@Override
	public void PulseEffectRemoveEffect(Player owner, Location loc, Player effectedPlayer, int abilityID) {
		//	StandardBearer
		{
			if (abilityID == STANDARD_BEARER_ID) {
				int standardBearer = ScoreboardUtils.getScoreboardValue(owner, "StandardBearer");
				if (standardBearer > 0) {
					if (standardBearer > 1) {
						AttributeInstance att = effectedPlayer.getAttribute(Attribute.GENERIC_ARMOR);
						double baseValue = att.getBaseValue(); 
						att.setBaseValue(Math.max(baseValue - STANDARD_BEARER_ARMOR, 0));
					}
				}
			}
		}
	}
	
	@Override
	public void PlayerInteractEvent(Player player, Action action, Material material) {
		if (player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				int eagleEye = ScoreboardUtils.getScoreboardValue(player, "Tinkering");
				if (eagleEye > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), EAGLE_EYE_ID)) {
						Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
						World world = player.getWorld();
						
						List<Entity> entities = player.getNearbyEntities(EAGLE_EYE_RADIUS, EAGLE_EYE_RADIUS, EAGLE_EYE_RADIUS);
						for(Entity e : entities) {
							if(EntityUtils.isHostileMob(e)) {
								LivingEntity mob = (LivingEntity)e;
								
								Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
								if (playerDir.dot(toMobVector) > EAGLE_EYE_DOT_ANGLE) {
									mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, EAGLE_EYE_DURATION, EAGLE_EYE_EFFECT_LVL, true, false));
									mob.setMetadata(EAGLE_EYE_TAG_NAME, new FixedMetadataValue(mPlugin, 1));
									
									ParticleUtils.playParticlesInWorld(world, Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
									world.playSound(player.getLocation(), "entity.parrot.imitate.shulker", 0.4f, 1.7f);
								}
							}
						}
						
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), EAGLE_EYE_ID, EAGLE_EYE_COOLDOWN);
					}
				}
			}
		}
	}
	
	@Override
	public void ModifyDamage(Player player, BaseClass owner, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(STANDARD_BEARER_TAG_NAME)) {
			int standardBearer = ScoreboardUtils.getScoreboardValue(player, "StandardBearer");
			if (standardBearer > 0) {
				double damage = event.getDamage();
				damage *= STANDARD_BEARER_DAMAGE_MULTIPLIER;
				event.setDamage(damage);
			}
		}
		
		Entity entity = event.getEntity();
		if (EntityUtils.isHostileMob(entity)) {
			LivingEntity mob = (LivingEntity)entity;
			
			int eagleEye = ScoreboardUtils.getScoreboardValue(player, "Tinkering");
			if (eagleEye > 0) {
				//	TODO: In the future we need to have the entities meta data remove after the durations....for now...we be lazy.
				if (mob.hasMetadata(EAGLE_EYE_TAG_NAME) && mob.hasPotionEffect(PotionEffectType.GLOWING)) {
					int extraDamage = (eagleEye == 1) ? EAGLE_EYE_1_EXTRA_DAMAGE : EAGLE_EYE_2_EXTRA_DAMAGE;
					event.setDamage(event.getDamage() + extraDamage);
				}
			}
		}	
	}
	
	public void _testForAgility(Player player) {
		int agility = ScoreboardUtils.getScoreboardValue(player, "Agility");
		if (agility > 0) {
			int effectLevel = agility == 1 ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL;
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLevel, true, false));
		}
	}
	
	public void _testForSwiftness(Player player) {
		int swiftness = ScoreboardUtils.getScoreboardValue(player, "Swiftness");
		if (swiftness > 0) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, 1000000, SWIFTNESS_EFFECT_SPEED_LVL, true, false));
			
			if (swiftness > 1) {
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
			}
		}
	}
	
	private void _testItemInHand(Player player, ItemStack mainHand) {
		int agility = ScoreboardUtils.getScoreboardValue(player, "Agility");
		if (agility > 0) {
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.DAMAGE_RESISTANCE);
			
			if (InventoryUtils.isPickaxeItem(mainHand)) {
				int effectLevel = agility == 1 ? AGILITY_1_RESISTANCE_LEVEL : AGILITY_2_RESISTANCE_LEVEL;
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, effectLevel, true, false));
			}
		}
	}
}
