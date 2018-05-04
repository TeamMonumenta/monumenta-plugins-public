package pe.project.classes;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;

import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.Plugin;
import pe.project.utils.EntityUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.ItemUtils;
import pe.project.utils.MovementUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.PotionUtils;
import pe.project.utils.PotionUtils.PotionInfo;
import pe.project.utils.particlelib.ParticleEffect;
import pe.project.utils.ScoreboardUtils;

/*
	BasiliskPoison
	PowerInjection
	ToxicTrail
	Caustic Blade
	BombArrow
	IronTincture
*/

public class AlchemistClass extends BaseClass {
	private static final double GRUESOME_ALCHEMY_CHANCE = 0.25f;
	private static final int GRUESOME_ALCHEMY_1_STACK_SIZE = 16;
	private static final int GRUESOME_ALCHEMY_2_STACK_SIZE = 32;
	private static final int GRUESOME_ALCHEMY_COOLDOWN = 3 * 60 * 20;

	private static final String PUTRID_FUMES_1_TAG = "PutridFumes1";
	private static final String PUTRID_FUMES_2_TAG = "PutridFumes2";
	private static final float PUTRID_FUMES_1_RADIUS = 3;
	private static final float PUTRID_FUMES_2_RADIUS = 5;
	private static final int PUTRID_FUMES_DURATION = 15 * 20;
	private static final int PUTRID_FUMES_COOLDOWN = 10 * 20;

	private static final int CAUSTIC_MIXTURE_1_DAMAGE = 6;
	private static final int CAUSTIC_MIXTURE_2_DAMAGE = 12;
	private static final String CAUSTIC_MIXTURE_TAG = "CausticMixture";

	private static final int IRON_TINCTURE_THROW_COOLDOWN = 10 * 20;
	private static final int IRON_TINCTURE_USE_COOLDOWN = 40 * 20;
	private static final double IRON_TINCTURE_VELOCITY = 0.7;

	private static final int BOMB_ARROW_COOLDOWN = 30 * 20;
	private static final int BOMB_ARROW_TRIGGER_RANGE = 32;
	private static final int BOMB_ARROW_ID = 67;
	public static final String BOMB_ARROW_TAG_NAME = "TagBearer";
	private static final int BOMB_ARROW_DURATION = 4 * 20;
	private static final float BOMB_ARROW_KNOCKBACK_SPEED = 0.5f;
	private static final int BOMB_ARROW_1_DAMAGE = 12;
	private static final int BOMB_ARROW_2_DAMAGE = 20;
	private static final int BOMB_ARROW_RADIUS = 3;

	private static final int CAUSTIC_BLADE_1_DURATION = 10 * 20;
	private static final int CAUSTIC_BLADE_2_DURATION = 15 * 20;
	private static final int CAUSTIC_BLADE_COOLDOWN = 6 * 20;
	private static final double CAUSTIC_BLADE_DISTANCE_MULT = 0.5;

	private static final int BASILISK_POISON_1_EFFECT_LVL = 0;
	private static final int BASILISK_POISON_2_EFFECT_LVL = 1;
	private static final int BASILISK_POISON_1_DURATION = 15 * 20;
	private static final int BASILISK_POISON_2_DURATION = 12 * 20;

	private static final int POWER_INJECTION_RANGE = 16;
	private static final int POWER_INJECTION_1_STRENGTH_EFFECT_LVL = 1;
	private static final int POWER_INJECTION_2_STRENGTH_EFFECT_LVL = 2;
	private static final int POWER_INJECTION_SPEED_EFFECT_LVL = 0;
	private static final int POWER_INJECTION_DURATION = 20 * 20;
	private static final int POWER_INJECTION_COOLDOWN = 30 * 20;

	private static final int INVIGORATING_ODOR_RESISTANCE_EFFECT_LVL = 0;
	private static final int INVIGORATING_ODOR_SPEED_EFFECT_LVL = 0;
	private static final int INVIGORATING_ODOR_REGENERATION_EFFECT_LVL = 0;
	private static final int INVIGORATING_ODOR_1_DURATION = 12 * 20;
	private static final int INVIGORATING_ODOR_2_DURATION = 15 * 20;

	public static final int TOXIC_TRAIL_ID = 56;
	public static final String TOXIC_TRAIL_TAG_NAME = "TagToxic";
	private static final int TOXIC_TRAIL_1_DURATION = 3 * 20;
	private static final int TOXIC_TRAIL_2_DURATION = 5 * 20;
	private static final int TOXIC_TRAIL_COOLDOWN = 20;
	private static final int TOXIC_TRAIL_RADIUS = 3;
	private static final double TOXIC_TRAIL_DAMAGE = 2;
	private static final int TOXIC_TRAIL_SLOW_LEVEL = 1;

	Arrow blinkArrow = null;

	public AlchemistClass(Plugin plugin, Random random) {
		super(plugin, random);
	}


	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		//	GruesomeAlchemy
		if (shouldGenDrops) {
			int gruesomeAlchemy = ScoreboardUtils.getScoreboardValue(player, "GruesomeAlchemy");
			if (gruesomeAlchemy > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.GRUESOME_ALCHEMY)) {
					if (mRandom.nextFloat() < GRUESOME_ALCHEMY_CHANCE) {
						int count = gruesomeAlchemy == 1 ? GRUESOME_ALCHEMY_1_STACK_SIZE : GRUESOME_ALCHEMY_2_STACK_SIZE;
						ItemStack stack = new ItemStack(Material.SPLASH_POTION, count);

						int rand = mRandom.nextInt(4);
						if (rand == 0) {
							ItemUtils.setPotionMeta(stack, "Splash Potion of Harming", PotionEffectType.INCREASE_DAMAGE.getColor());
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.HARM, 0, 1, false, true));
						} else if (rand == 1) {
							ItemUtils.setPotionMeta(stack, "Potion of Decay", Color.fromRGB(6178631));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.SLOW, 32 * 20, 0, false, true));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WEAKNESS, 32 * 20, 0, false, true));
						} else if (rand == 2) {
							ItemUtils.setPotionMeta(stack, "Scorpion Venom", Color.fromRGB(4610355));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WITHER, 18 * 20, 1, false, true));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.POISON, 18 * 20, 0, false, true));
						} else {
							ItemUtils.setPotionMeta(stack, "Lesser Frost Bomb", Color.fromRGB(3294553));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.SLOW, 20 * 20, 1, false, true));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WITHER, 20 * 20, 0, false, true));
						}

						World world = Bukkit.getWorld(player.getWorld().getName());
						world.dropItemNaturally(killedEntity.getLocation(), stack);

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.GRUESOME_ALCHEMY, GRUESOME_ALCHEMY_COOLDOWN);
					}
				}
			}
		}
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//	BasiliskPoison
		{
			int basiliskPoison = ScoreboardUtils.getScoreboardValue(player, "BasiliskPoison");
			if (basiliskPoison > 0) {
				int effectLvl = basiliskPoison == 1 ? BASILISK_POISON_1_EFFECT_LVL : BASILISK_POISON_2_EFFECT_LVL;
				int duration = basiliskPoison == 1 ? BASILISK_POISON_1_DURATION : BASILISK_POISON_2_DURATION;
				damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, effectLvl, false, true));
			}
		}
	}

	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		//	PowerInjection
		{
			if (arrow.isCritical() && player.isSneaking()) {
				int powerInjection = ScoreboardUtils.getScoreboardValue(player, "PowerInjection");
				if (powerInjection > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.POWER_INJECTION)) {
						LivingEntity targetEntity = EntityUtils.GetEntityAtCursor(player, POWER_INJECTION_RANGE, true, true, true);
						if (targetEntity != null && targetEntity instanceof Player) {
							Player targetPlayer = (Player) targetEntity;
							if (targetPlayer.getGameMode() != GameMode.SPECTATOR) {
								int effectLvl = powerInjection == 1 ? POWER_INJECTION_1_STRENGTH_EFFECT_LVL : POWER_INJECTION_2_STRENGTH_EFFECT_LVL;

								mPlugin.mPotionManager.addPotion(targetPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, effectLvl, false, true));

								if (powerInjection > 1) {
									mPlugin.mPotionManager.addPotion(targetPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_EFFECT_LVL, false, true));
								}

								mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.POWER_INJECTION, POWER_INJECTION_COOLDOWN);

								arrow.remove();
								return;
							}
						}
					}
				}
			}
		}

		//	BasiliskPoison
		{
			int basiliskPoison = ScoreboardUtils.getScoreboardValue(player, "BasiliskPoison");
			if (basiliskPoison > 0) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
			}
		}
	}

	@Override
	public void ProjectileHitEvent(Player player, Arrow arrow) {
		int bombArrow = ScoreboardUtils.getScoreboardValue(player, "BombArrow");
		if (bombArrow > 0 && player.getGameMode() != GameMode.ADVENTURE && player.isSneaking()) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BOMB_ARROW)) {
				double range = arrow.getLocation().distance(player.getLocation());
				if (range <= BOMB_ARROW_TRIGGER_RANGE) {
					mPlugin.mPulseEffectTimers.AddPulseEffect(player, this, BOMB_ARROW_ID, BOMB_ARROW_TAG_NAME, BOMB_ARROW_DURATION, 20, arrow.getLocation(), 0, false);

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BOMB_ARROW, BOMB_ARROW_COOLDOWN);
					arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
					blinkArrow = arrow;
				}
			}
		}
	}


	@Override
	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {
		if (player.isSneaking()) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PUTRID_FUMES)) {
				if (!PotionUtils.hasPositiveEffects(potion.getEffects())) {
					int putridFumes = ScoreboardUtils.getScoreboardValue(player, "PutridFumes");
					if (putridFumes > 0) {
						String meta = putridFumes == 1 ? PUTRID_FUMES_1_TAG : PUTRID_FUMES_2_TAG;
						potion.setMetadata(meta, new FixedMetadataValue(mPlugin, 0));

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PUTRID_FUMES, PUTRID_FUMES_COOLDOWN);
					}
				}
			}
		}
	}


	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
										   ThrownPotion potion, PotionSplashEvent event) {
		//  All affected players need to have the effect added to their potion manager.
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player) {
				// Thrown by a player - negative effects are not allowed for other players
				// Special for alchemist - don't apply negative effects to self either
				if (PotionUtils.hasNegativeEffects(potion.getEffects())) {
					// If a thrown potion contains any negative effects, don't apply *any* effects to other players
					event.setIntensity(entity, 0);
				}

				mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, potion.getEffects(),
												 event.getIntensity(entity));
			}
		}

		if (potion.hasMetadata(PUTRID_FUMES_1_TAG)) {
			AreaEffectCloud cloud = EntityUtils.spawnAreaEffectCloud(player.getWorld(), potion.getLocation(), potion.getEffects(), PUTRID_FUMES_1_RADIUS, PUTRID_FUMES_DURATION);
			cloud.setSource(player);
		} else if (potion.hasMetadata(PUTRID_FUMES_2_TAG)) {
			AreaEffectCloud cloud = EntityUtils.spawnAreaEffectCloud(player.getWorld(), potion.getLocation(), potion.getEffects(), PUTRID_FUMES_2_RADIUS, PUTRID_FUMES_DURATION);
			cloud.setSource(player);
		}

		World world = player.getWorld();
		int causticMixture = ScoreboardUtils.getScoreboardValue(player, "CausticMixture");
		int invigoratingOdor = ScoreboardUtils.getScoreboardValue(player, "InvigoratingOdor");

		boolean hitMonster = false;

		if (affectedEntities != null) {
			for (LivingEntity entity : affectedEntities) {
				if (EntityUtils.isHostileMob(entity)) {
					//	Caustic Mixture

					if (causticMixture > 0) {
						int damage = causticMixture == 1 ? CAUSTIC_MIXTURE_1_DAMAGE : CAUSTIC_MIXTURE_2_DAMAGE;
						if (!entity.hasMetadata(CAUSTIC_MIXTURE_TAG)) {
							EntityUtils.damageEntity(mPlugin, entity, damage, player);
							entity.setMetadata(CAUSTIC_MIXTURE_TAG, new FixedMetadataValue(mPlugin, 0));

							Location loc = entity.getLocation();
							ParticleUtils.playParticlesInWorld(world, Particle.TOTEM, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.001);
						}
					}

					//	Invigorating Odor
					if (invigoratingOdor > 0 && !hitMonster) {
						int duration = (invigoratingOdor == 1) ? INVIGORATING_ODOR_1_DURATION : INVIGORATING_ODOR_2_DURATION;

						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, duration, INVIGORATING_ODOR_SPEED_EFFECT_LVL, true, false));
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, INVIGORATING_ODOR_RESISTANCE_EFFECT_LVL, true, false));

						if (invigoratingOdor > 1) {
							mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, duration, INVIGORATING_ODOR_REGENERATION_EFFECT_LVL, true, false));
						}
					}

					hitMonster = true;
				}
			}
		}

		return true;
	}

	@Override
	public void AreaEffectCloudApplyEvent(Collection<LivingEntity> entities, Player player) {
		int causticMixture = ScoreboardUtils.getScoreboardValue(player, "CausticMixture");
		if (causticMixture > 0) {
			World world = player.getWorld();

			int damage = causticMixture == 1 ? CAUSTIC_MIXTURE_1_DAMAGE : CAUSTIC_MIXTURE_2_DAMAGE;
			for (LivingEntity entity : entities) {
				if (EntityUtils.isHostileMob(entity)) {
					if (!entity.hasMetadata(CAUSTIC_MIXTURE_TAG)) {
						EntityUtils.damageEntity(mPlugin, entity, damage, player);
						entity.setMetadata(CAUSTIC_MIXTURE_TAG, new FixedMetadataValue(mPlugin, 0));

						Location loc = entity.getLocation();
						ParticleUtils.playParticlesInWorld(world, Particle.TOTEM, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.001);
					}
				}
			}
		}
	}

	// =================
	// = IRON TINCTURE =
	// =================

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		{
			if (player.isSneaking()) {
				if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
					int ironTincture = ScoreboardUtils.getScoreboardValue(player, "IronTincture");
					if (ironTincture > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.IRON_TINCTURE)) {

							Location loc = player.getLocation().add(0,1.8,0);
							ItemStack itemTincture = new ItemStack(Material.SPLASH_POTION);

							Item tincture = (player.getWorld()).dropItem(loc, itemTincture);
							tincture.setPickupDelay(40);

							Vector vel = player.getEyeLocation().getDirection().normalize();
							vel.multiply(IRON_TINCTURE_VELOCITY);

							tincture.setVelocity(vel);
							tincture.setCustomName("IronTinctureTrigger");
							tincture.setCustomNameVisible(true);
							tincture.setGlowing(true);


							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.IRON_TINCTURE, IRON_TINCTURE_THROW_COOLDOWN);
						}
					}
				}
			}
		}
	}

	// =================
	// = CAUSTIC BLADE =
	// =================

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		int causticBlade = ScoreboardUtils.getScoreboardValue(player, "CausticBlade");
		if (causticBlade > 0 && EntityUtils.isHostileMob(damagee)) {
			if (PlayerUtils.isCritical(player)) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CAUSTIC_BLADE)) {

					int potionDuration = causticBlade == 0 ? CAUSTIC_BLADE_1_DURATION : CAUSTIC_BLADE_2_DURATION;

					ItemStack potion1;
					ItemStack potion2 = null;

					int rand = mRandom.nextInt(3);
					if (rand == 0) {
						potion1 = ItemUtils.createStackedPotions(PotionEffectType.SLOW, 3, potionDuration, 0, "Splash Potion of Slowness");
					} else if (rand == 1) {
						potion1 = ItemUtils.createStackedPotions(PotionEffectType.WEAKNESS, 1, potionDuration, 0, "Splash Potion of Weakness");
					} else {
						potion1 = ItemUtils.createStackedPotions(PotionEffectType.WITHER, 2, potionDuration, 0, "Splash Potion of Wither");
					}

					if (causticBlade > 1) {
						rand = (rand + mRandom.nextInt(2)) % 3;
						if (rand == 0) {
							potion2 = ItemUtils.createStackedPotions(PotionEffectType.SLOW, 3, potionDuration, 0, "Splash Potion of Slowness");
						} else if (rand == 1) {
							potion2 = ItemUtils.createStackedPotions(PotionEffectType.WEAKNESS, 1, potionDuration, 0, "Splash Potion of Weakness");
						} else {
							potion2 = ItemUtils.createStackedPotions(PotionEffectType.WITHER, 2, potionDuration, 0, "Splash Potion of Wither");
						}

					}


					World world = Bukkit.getWorld(player.getWorld().getName());
					Location pos = damagee.getLocation().add(((damagee.getLocation()).subtract(player.getLocation())).multiply(CAUSTIC_BLADE_DISTANCE_MULT));
					pos.setY(damagee.getLocation().getY()+2.0);

					EntityUtils.spawnCustomSplashPotion(world, player, potion1, pos);
					if (causticBlade > 1) {EntityUtils.spawnCustomSplashPotion(world, player, potion2, pos.add(0.1,0.1,0.1));}
					ParticleUtils.playParticlesInWorld(player.getWorld(), Particle.TOTEM, damagee.getLocation().add(0, 1, 0), 4, 0.15, 0.15, 0.15, 0.0);

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CAUSTIC_BLADE, CAUSTIC_BLADE_COOLDOWN);
				}
			}
		}

		return true;
	}


    // ===============
	// = TOXIC TRAIL =
	// ===============

	@Override
	public void PeriodicTrigger(Player player, boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		if (oneSecond) {
			if (!player.isDead()) {
				int toxicTrail = ScoreboardUtils.getScoreboardValue(player, "ToxicTrail");
				if (toxicTrail > 0) {// && player.getGameMode() != GameMode.SPECTATOR) {
					int toxicDuration = toxicTrail == 1? TOXIC_TRAIL_1_DURATION : TOXIC_TRAIL_2_DURATION;
					Location loc = player.getLocation();
					loc = loc.subtract((player.getEyeLocation().getDirection().normalize()).multiply(0.25));

					mPlugin.mPulseEffectTimers.AddPulseEffect(player, this, TOXIC_TRAIL_ID, TOXIC_TRAIL_TAG_NAME, toxicDuration, TOXIC_TRAIL_COOLDOWN, loc, TOXIC_TRAIL_RADIUS, false);
				}
			}
		}
	}

	@Override
	public void PulseEffectApplyEffect(Player owner, Location loc, Entity effectedEntity, int abilityID) {
		{
			if (abilityID == TOXIC_TRAIL_ID) {
				int toxicTrail = ScoreboardUtils.getScoreboardValue(owner, "ToxicTrail");
				if (toxicTrail > 0) {
					if (owner.getGameMode() == GameMode.SURVIVAL || owner.getGameMode() == GameMode.ADVENTURE) {
						double x = loc.getX();
						double y = loc.getY() + 0.25;
						double z = loc.getZ();
						Location newLoc = new Location(loc.getWorld(), x, y, z);

						ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.SPELL_MOB_AMBIENT, newLoc, 30, 0.75, 0.3, 0.75, 0.001);
						if (effectedEntity instanceof LivingEntity) {
							LivingEntity e = (LivingEntity)effectedEntity;
							EntityUtils.damageEntity(mPlugin, e, TOXIC_TRAIL_DAMAGE, null);

							if (toxicTrail > 1) {
								e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, TOXIC_TRAIL_2_DURATION, TOXIC_TRAIL_SLOW_LEVEL, true, false));
							}
						}
					}
				}
			}

			if (abilityID == BOMB_ARROW_ID) {
				int bombArrow = ScoreboardUtils.getScoreboardValue(owner, "BombArrow");
				if (bombArrow > 0) {
					ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.FLAME, loc, 8, 0.3, 0.3, 0.3, 0.001);
					ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.SMOKE_NORMAL, loc, 30, 0.5, 0.5, 0.5, 0.001);
					owner.getWorld().playSound(loc, "entity.tnt.primed", 5.0f, 0.25f);
				}
			}
		}
	}

	@Override
	public void PulseEffectComplete(Player owner, Location loc, Entity marker, int abilityID) {
		if (abilityID == BOMB_ARROW_ID) {
			int bombArrow = ScoreboardUtils.getScoreboardValue(owner, "BombArrow");
			if (bombArrow > 0) {
				if (blinkArrow != null) {
					loc = blinkArrow.getLocation();
					blinkArrow.remove();
					blinkArrow = null;
				}

				loc = loc.add(0,1.2,0);
				owner.getWorld().playSound(loc, "entity.generic.explode", 0.7f, 1.0f);
				owner.getWorld().playSound(loc, "entity.generic.explode", 0.9f, 1.0f);

				ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.EXPLOSION_HUGE, loc, 3, 0.02, 0.02, 0.02, 0.001);
				List<Entity> entities = marker.getNearbyEntities(BOMB_ARROW_RADIUS, BOMB_ARROW_RADIUS, BOMB_ARROW_RADIUS);

				int baseDamage = bombArrow == 1 ? BOMB_ARROW_1_DAMAGE : BOMB_ARROW_2_DAMAGE;

				for(int i = 0; i < entities.size(); i++) {
					Entity e = entities.get(i);
					if(EntityUtils.isHostileMob(e)) {
						double d = e.getLocation().distance(loc);
						double ds = Math.min(1, 1.5 - d/3);

						LivingEntity mob = (LivingEntity)e;
						EntityUtils.damageEntity(mPlugin, mob, baseDamage * ds, owner);
						MovementUtils.KnockAway((LivingEntity)marker, mob, BOMB_ARROW_KNOCKBACK_SPEED);
					}
				}
			}
		}
	}
}