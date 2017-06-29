package pe.project.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import pe.project.Main;
import pe.project.utils.EntityUtils;
import pe.project.utils.ItemUtils;
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
	private static int AGILITY_EFFECT_SPEED_LVL = 0;
	private static int AGILITY_EFFECT_JUMP_LVL = 2;
	
	private static int SWIFTNESS_1_EFFECT_LVL = 0;
	private static int SWIFTNESS_2_EFFECT_LVL = 1;
	
	//private static int EXPLORATION_1_EFFECT_LVL = 0;
	//private static int EXPLORATION_2_EFFECT_LVL = 1;
	
	private static int BOW_MASTER_1_DAMAGE = 3;
	private static int BOW_MASTER_2_DAMAGE = 6;
	
	private static double TINKERING_1_CHANCE_OF_SLOWNESS = 0.25f;
	private static double TINKERING_2_CHANCE_OF_SLOWNESS = 0.5f;
	private static double TINKERING_1_CHANCE_OF_WEAKNESS = 0.25f;
	private static double TINKERING_2_CHANCE_OF_WEAKNESS = 0.5f;
	
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
	public void AbilityOffCooldown(Player player, int abilityID) {
		if (abilityID == VOLLEY_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Volley is now off cooldown");
		} else if (abilityID == STANDARD_BEARER_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Standard Bearer is now off cooldown");
		}
	}
	
	@Override
	public void PlayerRespawnEvent(Player player) {
		//	Agility
		{
			int agility = ScoreboardUtils.getScoreboardValue(player, "Agility");
			if (agility > 0) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, AGILITY_EFFECT_SPEED_LVL, true, false));
				
				if (agility > 1) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, AGILITY_EFFECT_JUMP_LVL, true, false));
				}
			}
		}
		
		//	Swiftness
		{
			int swiftness = ScoreboardUtils.getScoreboardValue(player, "Swiftness");
			if (swiftness > 0) {
				int effectLevel = swiftness == 1 ? SWIFTNESS_1_EFFECT_LVL : SWIFTNESS_2_EFFECT_LVL;
				player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLevel, true, false));
			}
		}
		
		//	Exploration
		/*{
			int exploration = ScoreboardUtil.getScoreboardValue(player, "Exploration");
			if (exploration > 0) {
				int effectLevel = exploration == 1 ? EXPLORATION_1_EFFECT_LVL : EXPLORATION_2_EFFECT_LVL;
				player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 1000000, effectLevel, true, false));
			}
		}*/
	}
	
	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity) {
		int tinkering = ScoreboardUtils.getScoreboardValue(player, "Tinkering");
		if (tinkering > 0) {
			double chanceOfSlowness = tinkering == 1 ? TINKERING_1_CHANCE_OF_SLOWNESS : TINKERING_2_CHANCE_OF_SLOWNESS;
			double chanceOfWeakness = tinkering == 1 ? TINKERING_1_CHANCE_OF_WEAKNESS : TINKERING_2_CHANCE_OF_WEAKNESS;
			
			//	Try to find Slowness Arrow.
			if (mRandom.nextDouble() < chanceOfSlowness) {
				PotionData data = new PotionData(PotionType.SLOWNESS, false, false);
				ItemStack arrow = ItemUtils.createTippedArrows(PotionType.SLOWNESS, 1, data);

				player.getWorld().dropItemNaturally(killedEntity.getLocation(), arrow);
			}
			//	Try to find Weakness Arrow.
			else if (mRandom.nextDouble() < chanceOfWeakness) {
				PotionData data = new PotionData(PotionType.WEAKNESS, false, false);
				ItemStack arrow = ItemUtils.createTippedArrows(PotionType.WEAKNESS, 1, data);
				
				World world = Bukkit.getWorld(player.getWorld().getName());
				world.dropItemNaturally(killedEntity.getLocation(), arrow);
			}
		}
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
		{
			//	Tinkering
			int tinkering = ScoreboardUtils.getScoreboardValue(player, "Tinkering");
			if (tinkering > 1) {
				if (arrow instanceof TippedArrow) {
					TippedArrow tArrow = (TippedArrow)arrow;
					PotionData data = tArrow.getBasePotionData();
					
					if (data.getType() == PotionType.SLOWNESS) {
						damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 11 * 20, 1, false, true));
					} else if (data.getType() == PotionType.WEAKNESS) {
						damagee.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 11 * 20, 1, false, true));
					}
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
	public void ModifyDamage(Player player, BaseClass owner, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(STANDARD_BEARER_TAG_NAME)) {
			int standardBearer = ScoreboardUtils.getScoreboardValue(player, "StandardBearer");
			if (standardBearer > 0) {
				double damage = event.getDamage();
				damage *= STANDARD_BEARER_DAMAGE_MULTIPLIER;
				event.setDamage(damage);
			}
		}
	}
}
