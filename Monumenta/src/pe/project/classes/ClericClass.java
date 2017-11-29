package pe.project.classes;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import pe.project.Plugin;
import pe.project.utils.EntityUtils;
import pe.project.utils.ItemUtils;
import pe.project.utils.MessagingUtils;
import pe.project.utils.MovementUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.PotionUtils;
import pe.project.utils.ScoreboardUtils;

/*
	Sanctified
	Rejuvenation
	HeavenlyBoon
Cleansing
	DivineJustice
	Celestial
	Healing
*/

public class ClericClass extends BaseClass {
	private static final double SANCTIFIED_1_DAMAGE = 3;
	private static final double SANCTIFIED_2_DAMAGE = 5;
	private static final int SANCTIFIED_EFFECT_LEVEL = 0;
	private static final int SANCTIFIED_EFFECT_DURATION = 10 * 20;
	private static final float SANCTIFIED_KNOCKBACK_SPEED = 0.35f;

	private static int REJUVENATION_RADIUS = 12;
	private static int REJUVENATION_HEAL_AMOUNT = 1;

	//	HEAVENLY_BOON
	private static double HEAVENLY_BOON_1_CHANCE = 0.05;
	private static double HEAVENLY_BOON_2_CHANCE = 0.08;
	private static double HEAVENLY_BOON_TRIGGER_RANGE = 1.0;
	private static double HEAVENLY_BOON_RADIUS = 12;

	//	CLEANSING

	private static int DIVINE_JUSTICE_DAMAGE = 5;
	private static int DIVINE_JUSTICE_HEAL = 4;
	private static int DIVINE_JUSTICE_CRIT_HEAL = 1;

	private static int CELESTIAL_ID = 36;
	public static int CELESTIAL_1_FAKE_ID = 100361;
	public static int CELESTIAL_2_FAKE_ID = 100362;
	private static int CELESTIAL_COOLDOWN = 40 * 20;
	private static int CELESTIAL_1_DURATION = 10 * 20;
	private static int CELESTIAL_2_DURATION = 12 * 20;
	private static double CELESTIAL_RADIUS = 12;
	public static String CELESTIAL_1_TAGNAME = "Celestial_1";
	public static String CELESTIAL_2_TAGNAME = "Celestial_2";
	private static double CELESTIAL_1_DAMAGE_MULTIPLIER = 1.20;
	private static double CELESTIAL_2_DAMAGE_MULTIPLIER = 1.30;

	private static int HEALING_ID = 37;
	private static int HEALING_RADIUS = 12;
	private static int HEALING_1_HEAL = 10;
	private static int HEALING_2_HEAL = 16;
	private static double HEALING_DOT_ANGLE = 0.33;
	private static int HEALING_1_COOLDOWN = 20 * 20;
	private static int HEALING_2_COOLDOWN = 15 * 20;

	public ClericClass(Plugin plugin, Random random) {
		super(plugin, random);
	}

	@Override
	public void AbilityOffCooldown(Player player, int abilityID) {
		if (abilityID == CELESTIAL_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Celestial Blessing is now off cooldown");
		} else if (abilityID == HEALING_ID) {
			MessagingUtils.sendActionBarMessage(mPlugin, player, "Hand of Light is now off cooldown");
		}
	}

	@Override
	public boolean has2SecondTrigger() {
		return true;
	}

	@Override
	public void PeriodicTrigger(Player player, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		//	Don't trigger this if dead!
		if (!player.isDead()) {
			boolean threeSeconds = ((originalTime % 3) == 0);

			//	Rejuvenation
			if (threeSeconds) {
				int rejuvenation = ScoreboardUtils.getScoreboardValue(player, "Rejuvenation");
				if (rejuvenation > 0) {
					List<Entity> entities = player.getNearbyEntities(REJUVENATION_RADIUS, REJUVENATION_RADIUS, REJUVENATION_RADIUS);
					entities.add(player);
					for (Entity entity : entities) {
						if (entity instanceof Player) {
							Player p = (Player)entity;

							//	If this is us or we're allowing anyone to get it.
							if (p == player || rejuvenation > 1) {
								PlayerUtils.healPlayer(p, REJUVENATION_HEAL_AMOUNT);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		//	Sanctified
		if (EntityUtils.isUndead(damager)) {
			if (damager instanceof Skeleton) {
				Skeleton skelly = (Skeleton)damager;
				ItemStack mainHand = skelly.getEquipment().getItemInMainHand();
				if (mainHand != null && mainHand.getType() == Material.BOW) {
					return;
				}
			}

			int sanctified = ScoreboardUtils.getScoreboardValue(player, "Sanctified");
			if (sanctified > 0) {
				double extraDamage = sanctified == 1 ? SANCTIFIED_1_DAMAGE : SANCTIFIED_2_DAMAGE;
				damager.damage(extraDamage, player);

				MovementUtils.KnockAway(player, damager, SANCTIFIED_KNOCKBACK_SPEED);

				Location loc = damager.getLocation();
				ParticleUtils.playParticlesInWorld(player.getWorld(), Particle.END_ROD, loc.add(0, 1, 0), 5, 0.35, 0.35, 0.35, 0.001);

				if (sanctified > 1) {
					damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SANCTIFIED_EFFECT_DURATION, SANCTIFIED_EFFECT_LEVEL, true, false));
				}
			}
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		//	DivineJustice
		{
			if (PlayerUtils.isCritical(player) && cause != DamageCause.PROJECTILE) {
				if (EntityUtils.isUndead(damagee)) {
					int divineJustice = ScoreboardUtils.getScoreboardValue(player, "DivineJustice");
					if (divineJustice > 0) {
						damagee.damage(DIVINE_JUSTICE_DAMAGE, player);

						PlayerUtils.healPlayer(player, DIVINE_JUSTICE_CRIT_HEAL);

						World world = player.getWorld();
						Location loc = damagee.getLocation();
						ParticleUtils.playParticlesInWorld(world, Particle.DRAGON_BREATH, loc.add(0, 1, 0), 50, 0.25, 0.5, 0.5, 0.001);
						world.playSound(loc, "block.anvil.land", 0.15f, 1.5f);
					}
				}
			}
		}

		return true;
	}

	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		//	DivineJustice
		{
			if (PlayerUtils.isCritical(player) && cause != DamageCause.PROJECTILE) {
				if (EntityUtils.isUndead(killedEntity)) {
					int divineJustice = ScoreboardUtils.getScoreboardValue(player, "DivineJustice");
					if (divineJustice > 1) {
						PlayerUtils.healPlayer(player, DIVINE_JUSTICE_HEAL);

						World world = player.getWorld();
						Location loc = killedEntity.getLocation();
						ParticleUtils.playParticlesInWorld(world, Particle.HEART, loc.add(0, 1, 0), 5, 0.25, 0.25, 0.25, 0.001);
						player.getWorld().playSound(loc, "block.enchantment_table.use", 1.5f, 1.5f);

					}
				}
			}
		}

		//	HeavenlyBoon
		if (shouldGenDrops) {
			if (EntityUtils.isUndead(killedEntity)) {
				int heavenlyBoon = ScoreboardUtils.getScoreboardValue(player, "HeavenlyBoon");
				if (heavenlyBoon > 0) {
					double chance = heavenlyBoon == 1 ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE;

					if (mRandom.nextDouble() < chance) {
						ItemStack potions;

						if (heavenlyBoon == 1) {
							int rand = mRandom.nextInt(3);
							if (rand == 0) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.HEAL, 1, 0, 0, "Splash Potion of Healing");
							} else if (rand == 1) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, 45 * 20, 0, "Splash Potion of Regeneration");
							} else {
								potions = ItemUtils.createStackedPotions(PotionEffectType.JUMP, 1, 180 * 20, 1, "Splash Potion of Leaping");
							}
						} else {
							int rand = mRandom.nextInt(5);
							if (rand == 0) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, 60 * 20, 0, "Splash Potion of Regeneration");
							} else if (rand == 1) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, 90 * 20, 0, "Splash Potion of Absorption");
							} else if (rand == 2) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, 120 * 20, 0, "Splash Potion of Speed");
							} else if (rand == 3) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.INCREASE_DAMAGE, 1, 120 * 20, 0, "Splash Potion of Strength");
							} else {
								potions = ItemUtils.createStackedPotions(PotionEffectType.DAMAGE_RESISTANCE, 1, 120 * 20, 0, "Splash Potion of Resistance");
							}
						}

						World world = Bukkit.getWorld(player.getWorld().getName());
						world.dropItemNaturally(killedEntity.getLocation(), potions);
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities, ThrownPotion potion) {
		//	HeavenlyBoon
		{
			int heavenlyBoon = ScoreboardUtils.getScoreboardValue(player, "HeavenlyBoon");
			if (heavenlyBoon > 0) {
				double range = potion.getLocation().distance(player.getLocation());
				if (range <= HEAVENLY_BOON_TRIGGER_RANGE) {
					PotionMeta meta = (PotionMeta)potion.getItem().getItemMeta();

					List<Entity> entities = player.getNearbyEntities(HEAVENLY_BOON_RADIUS, HEAVENLY_BOON_RADIUS, HEAVENLY_BOON_RADIUS);
					entities.add(player);
					for(int i = 0; i < entities.size(); i++) {
						Entity e = entities.get(i);
						if(e instanceof Player) {
							Player p = (Player)(e);

							List<PotionEffect> effectList = PotionUtils.getEffects(meta);
							for (PotionEffect effect : effectList) {
								PotionUtils.applyPotion(mPlugin, p, effect);
							}
						}
					}

					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				int celestial = ScoreboardUtils.getScoreboardValue(player, "Celestial");
				if (celestial > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), CELESTIAL_ID)) {
						World world = player.getWorld();

						List<Entity> entities = player.getNearbyEntities(CELESTIAL_RADIUS, CELESTIAL_RADIUS, CELESTIAL_RADIUS);
						entities.add(player);
						for(int i = 0; i < entities.size(); i++) {
							Entity e = entities.get(i);
							if(e instanceof Player) {
								Player p = (Player)(e);

								int fakeID = celestial == 1 ? CELESTIAL_1_FAKE_ID : CELESTIAL_2_FAKE_ID;

								int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;

								mPlugin.mTimers.AddCooldown(p.getUniqueId(), fakeID, duration);

								p.setMetadata(celestial == 1 ? CELESTIAL_1_TAGNAME : CELESTIAL_2_TAGNAME, new FixedMetadataValue(mPlugin, 0));

								Location loc = p.getLocation();
								ParticleUtils.playParticlesInWorld(world, Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 15, 0.75, 0.75, 0.75, 0.001);
								world.playSound(loc, "entity.player.levelup", 0.15f, 1.5f);
							}
						}

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), CELESTIAL_ID, CELESTIAL_COOLDOWN);
					}
				}
			} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				ItemStack offHand = player.getInventory().getItemInOffHand();
				if (offHand != null && offHand.getType() == Material.SHIELD) {
					int healing = ScoreboardUtils.getScoreboardValue(player, "Healing");
					if (healing > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), HEALING_ID)) {
							Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
							World world = player.getWorld();

							List<Entity> entities = player.getNearbyEntities(HEALING_RADIUS, HEALING_RADIUS, HEALING_RADIUS);
							for(Entity e : entities) {
								if(e instanceof Player) {
									Player p = (Player)e;
									if (p != player) {
										Vector toMobVector = p.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
										if (playerDir.dot(toMobVector) > HEALING_DOT_ANGLE) {
											int healAmount = healing == 1 ? HEALING_1_HEAL : HEALING_2_HEAL;
											PlayerUtils.healPlayer(p, healAmount);

											Location loc = p.getLocation();

											ParticleUtils.playParticlesInWorld(world, Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
											ParticleUtils.playParticlesInWorld(world, Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
											player.getWorld().playSound(loc, "block.enchantment_table.use", 2.0f, 1.6f);
											player.getWorld().playSound(loc, "entity.player.levelup", 0.05f, 1.0f);
										}
									}
								}
							}

							player.getWorld().playSound(player.getLocation(), "block.enchantment_table.use", 2.0f, 1.6f);
							player.getWorld().playSound(player.getLocation(), "entity.player.levelup", 0.05f, 1.0f);

							ParticleUtils.explodingConeEffect(mPlugin, player, HEALING_RADIUS, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, HEALING_DOT_ANGLE);

							int cooldown = healing == 1 ? HEALING_1_COOLDOWN : HEALING_2_COOLDOWN;
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), HEALING_ID, cooldown);
						}
					}
				}
			}
		}
	}

	@Override
	public void ModifyDamage(Player player, BaseClass owner, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(CELESTIAL_1_TAGNAME)) {
			double damage = event.getDamage();
			damage *= CELESTIAL_1_DAMAGE_MULTIPLIER;
			event.setDamage(damage);
		} else if (player.hasMetadata(CELESTIAL_2_TAGNAME)) {
			double damage = event.getDamage();
			damage *= CELESTIAL_2_DAMAGE_MULTIPLIER;
			event.setDamage(damage);
		}
	}
}
