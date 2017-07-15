package pe.project.classes;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import pe.project.Main;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.EntityUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.ItemUtils;
import pe.project.utils.MessagingUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.ScoreboardUtils;

/*
	DuelWeilding
	Focus
	ViciousCombos
	SmokeScreen
PoisonTrap
	EscapeDeath
	Assassination
*/

public class RogueClass extends BaseClass {
	private static int DUAL_WIELDING_1_EFFECT_LVL = 1;
	private static int DUAL_WIELDING_2_EFFECT_LVL = 3;
	
	private static int FOCUS_ID = 42;
	private static int FOCUS_DURATION = 5 * 20;
	private static int FOCUS_1_STRENGTH_LVL = 1;
	private static int FOCUS_2_STRENGTH_LVL = 2;
	private static int FOCUS_COOLDOWN = 20 * 20;
	private static int FOCUS_RESISTENCE_LVL = 0;
	
	private static int VICIOUS_COMBOS_EFFECT_LEVEL = 0;
	private static int VICIOUS_COMBOS_EFFECT_DURATION = 15 * 20;
	private static int VICIOUS_COMBOS_DAMAGE = 24;
	private static double VICIOUS_COMBOS_RANGE = 5;
	
	
	private static int SMOKESCREEN_ID = 44;
	private static int SMOKESCREEN_COOLDOWN = 20 * 20;
	private static int SMOKESCREEN_RANGE = 6;
	private static int SMOKESCREEN_1_WEAKNESS_EFFECT_LEVEL = 0;
	private static int SMOKESCREEN_2_WEAKNESS_EFFECT_LEVEL = 1;
	private static int SMOKESCREEN_SLOWNESS_EFFECT_LEVEL = 1;
	private static int SMOKESCREEN_DURATION = 8 * 20;
	
	//	POISONTRAP
	
	private static int ESCAPE_DEATH_ID = 46;
	private static int ESCAPE_DEATH_HEALTH_TRIGGER = 10;
	private static int ESCAPE_DEATH_DURATION = 4 * 20;
	private static int ESCAPE_DEATH_DURATION_OTHER = 8 * 20;
	private static int ESCAPE_DEATH_DURATION_SLOWNESS = 5 * 20;
	private static int ESCAPE_DEATH_RESISTENCE_EFFECT_LVL = 1;
	private static int ESCAPE_DEATH_SPEED_EFFECT_LVL = 1;
	private static int ESCAPE_DEATH_JUMP_EFFECT_LVL = 2;
	private static int ESCAPE_DEATH_SLOWNESS_EFFECT_LVL = 4;
	private static int ESCAPE_DEATH_RANGE = 5;
	private static int ESCAPE_DEATH_COOLDOWN = 90 * 20;
	
	//	ASSASSINATION
	private static int ASSASSINATION_ID = 47;
	private static int ASSASSINATION_COOLDOWN = 10 * 20;
	private static int ASSASSINATION_1_DAMAGE = 12;
	private static int ASSASSINATION_2_DAMAGE = 24;
	private static int ASSASSINATION_ELITE_MODIFIER = 2;
	
	public RogueClass(Main plugin, Random random) {
		super(plugin, random);
	}
	
	@Override
	public void setupClassPotionEffects(Player player) {
		_testForDuelWielding(player);
	}
	
	@Override
	public void AbilityOffCooldown(Player player, int abilityID) {
		if (abilityID == ASSASSINATION_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Assassination is now off cooldown");
		} else if (abilityID == FOCUS_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Focus is now off cooldown");
		} else if (abilityID == ESCAPE_DEATH_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Escape Death is now off cooldown");
		} else if (abilityID == SMOKESCREEN_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Smokescreen is now off cooldown");
		}
	}
	
	@Override
	public void PlayerItemHeldEvent(Player player) {
		_testForDuelWielding(player);
	}
	
	@Override
	public void PlayerDropItemEvent(Player player) {
		_testForDuelWielding(player);
	}
	
	@Override
	public void PlayerItemBreakEvent(Player player) {
		_testForDuelWielding(player);
	}
	
	@Override
	public void PlayerInteractEvent(Player player, Action action, Material material) {
		if (action.equals(Action.RIGHT_CLICK_AIR) || (action.equals(Action.RIGHT_CLICK_BLOCK) && !ItemUtils.isInteractable(material))) {
			//	FOCUS
			{
				int focus = ScoreboardUtils.getScoreboardValue(player, "Focus");		
				if (focus > 0) {
					if (_testForSwordsInHand(player)) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), FOCUS_ID)) {
							int effectLvl = focus == 1 ? FOCUS_1_STRENGTH_LVL : FOCUS_2_STRENGTH_LVL;
							mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, FOCUS_DURATION, effectLvl, true, false));
							
							if (focus > 1) {
								mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, FOCUS_DURATION, FOCUS_RESISTENCE_LVL, true, false));
							}
							
							World world = player.getWorld();
							Location loc = player.getLocation();
							loc.add(0, 1, 0);
							
							ParticleUtils.playParticlesInWorld(world, Particle.SPELL, loc, 75, 1.0, 0.5, 1.0, 0.001);
							world.playSound(loc, "entity.horse.breathe", 1.0f, 0.5f);
							
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), FOCUS_ID, FOCUS_COOLDOWN);
						}
					}
				}
			}
		} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				int smokeScreen = ScoreboardUtils.getScoreboardValue(player, "SmokeScreen");
				if (smokeScreen > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), SMOKESCREEN_ID)) {
						ItemStack mainHand = player.getInventory().getItemInMainHand();
						if (mainHand != null && mainHand.getType() != Material.BOW) {
							List<Entity> entities = player.getNearbyEntities(SMOKESCREEN_RANGE, SMOKESCREEN_RANGE, SMOKESCREEN_RANGE);
							for (Entity entity : entities) {
								if (entity instanceof LivingEntity && !(entity instanceof Player)) {
									LivingEntity mob = (LivingEntity)entity;
									
									int weaknessLevel = smokeScreen == 1 ? SMOKESCREEN_1_WEAKNESS_EFFECT_LEVEL : SMOKESCREEN_2_WEAKNESS_EFFECT_LEVEL;
								
									mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, SMOKESCREEN_DURATION, weaknessLevel, false, true));
									mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SMOKESCREEN_DURATION, SMOKESCREEN_SLOWNESS_EFFECT_LEVEL, false, true));
								}
							}
							
							Location loc = player.getLocation();
							World world = player.getWorld();
							ParticleUtils.playParticlesInWorld(world, Particle.SMOKE_LARGE, loc.add(0, 1, 0), 150, 2.0, 0.8, 2.0, 0.001);
							world.playSound(loc, "entity.blaze.shoot", 1.0f, 0.35f);
							
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), SMOKESCREEN_ID, SMOKESCREEN_COOLDOWN);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		double correctHealth = player.getHealth() - damage;
		if (correctHealth > 0 && correctHealth <= ESCAPE_DEATH_HEALTH_TRIGGER) {
			int escapeDeath = ScoreboardUtils.getScoreboardValue(player, "EscapeDeath");
			if (escapeDeath > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), ESCAPE_DEATH_ID)) {
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, ESCAPE_DEATH_DURATION, ESCAPE_DEATH_RESISTENCE_EFFECT_LVL, true, false));
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, ESCAPE_DEATH_DURATION_OTHER, ESCAPE_DEATH_SPEED_EFFECT_LVL, true, false));
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, ESCAPE_DEATH_DURATION_OTHER, ESCAPE_DEATH_JUMP_EFFECT_LVL, true, false));
					
					if (escapeDeath > 1) {
						List<Entity> entities = player.getNearbyEntities(ESCAPE_DEATH_RANGE, ESCAPE_DEATH_RANGE, ESCAPE_DEATH_RANGE);
						for (Entity entity : entities) {
							if (entity instanceof LivingEntity && !(entity instanceof Player)) {
								LivingEntity mob = (LivingEntity)entity;
								mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ESCAPE_DEATH_DURATION_SLOWNESS, ESCAPE_DEATH_SLOWNESS_EFFECT_LVL, true, false));
							}
						}
					}
					
					World world = player.getWorld();
					Location loc = player.getLocation();
					loc.add(0, 1, 0);
					
					int val = escapeDeath == 1 ? 1 : ESCAPE_DEATH_RANGE;
					Vector offset = new Vector(val, val, val);
					int particles = escapeDeath == 1 ? 30 : 500;
					
					ParticleUtils.playParticlesInWorld(world, Particle.SPELL_INSTANT, loc, particles, offset.getX(), offset.getY(), offset.getZ(), 0.001);
					
					if (escapeDeath > 1) {
						ParticleUtils.playParticlesInWorld(world, Particle.CLOUD, loc, particles, offset.getX(), offset.getY(), offset.getZ(), 0.001);
					}
					
					world.playSound(loc, "item.totem.use", 1.0f, 0.5f);
					
					MessagingUtils.sendActionBarMessage(mPlugin, player, "Escape Death has been activated");
					
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), ESCAPE_DEATH_ID, ESCAPE_DEATH_COOLDOWN);
				}	
			}
		}
	}
	
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage) {
		if (PlayerUtils.isCritical(player)) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();
			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				int assassination = ScoreboardUtils.getScoreboardValue(player, "Assassination");
				if (assassination > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), ASSASSINATION_ID)) {
						int extraDamage = assassination == 1 ? ASSASSINATION_1_DAMAGE : ASSASSINATION_2_DAMAGE;
						
						boolean isEliteBoss = EntityUtils.isEliteBoss(damagee);
						if (isEliteBoss) {
							extraDamage *= ASSASSINATION_ELITE_MODIFIER;
						}
						
						damagee.damage(extraDamage);
						
						if (damagee.getHealth() <= 0) {
							_viciousCombos(player, damagee);
						}
						
						World world = player.getWorld();
						Location loc = damagee.getLocation();
						loc.add(0, 1, 0);
						ParticleUtils.playParticlesInWorld(world, Particle.SPELL_MOB, loc, 15, 0.25, 0.5, 0.5, 0.001);
						ParticleUtils.playParticlesInWorld(world, Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001);
						world.playSound(loc, "item.shield.break", 2.0f, 0.5f);
						
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), ASSASSINATION_ID, ASSASSINATION_COOLDOWN);
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity) {
		_viciousCombos(player, killedEntity);
	}
	
	private void _testForDuelWielding(Player player) {
		int dualWielding = ScoreboardUtils.getScoreboardValue(player, "DualWielding");
		if (dualWielding > 0) {
			if (_testForSwordsInHand(player)) {	
				int effectLvl = dualWielding == 1 ? DUAL_WIELDING_1_EFFECT_LVL : DUAL_WIELDING_2_EFFECT_LVL;
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLvl, true, false));
			} else {
				mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
			}
		}
	}
	
	private boolean _testForSwordsInHand(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		
		return InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand);
	}
	
	private void _viciousCombos(Player player, LivingEntity killedEntity) {
		if (EntityUtils.isEliteBoss(killedEntity)) {
			int viciousCombos = ScoreboardUtils.getScoreboardValue(player, "ViciousCombos");
			if (viciousCombos > 0) {
				World world = player.getWorld();
				Location loc = killedEntity.getLocation();
				loc = loc.add(0, 0.5, 0);
				
				List<Entity> entities = player.getNearbyEntities(VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE);
				for (Entity entity : entities) {
					if (entity instanceof LivingEntity && !(entity instanceof Player)) {
						LivingEntity mob = (LivingEntity)entity;
						mob.damage(VICIOUS_COMBOS_DAMAGE);
					}
				}
				
				if (viciousCombos > 1) {
					mPlugin.mTimers.removeCooldowns(player.getUniqueId());
					MessagingUtils.sendActionBarMessage(mPlugin, player, "All your cooldowns have been reset");
					
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, VICIOUS_COMBOS_EFFECT_DURATION, VICIOUS_COMBOS_EFFECT_LEVEL, true, false));
				}
				
				ParticleUtils.playParticlesInWorld(world, Particle.SWEEP_ATTACK, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001);
				ParticleUtils.playParticlesInWorld(world, Particle.SPELL_MOB, loc, 350, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, VICIOUS_COMBOS_RANGE, 0.001);
			}
		}
	}
}
