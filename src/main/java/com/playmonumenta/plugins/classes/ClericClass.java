package com.playmonumenta.plugins.classes;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

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

	private static final int REJUVENATION_RADIUS = 12;
	private static final int REJUVENATION_HEAL_AMOUNT = 1;

	//	HEAVENLY_BOON
	private static final double HEAVENLY_BOON_1_CHANCE = 0.06;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_TRIGGER_RANGE = 2.0;
	private static final double HEAVENLY_BOON_RADIUS = 12;

	//	CLEANSING
	private static final int CLEANSING_DURATION = 15 * 20;
	private static final int CLEANSING_RESIST_LEVEL = 0;
	private static final int CLEANSING_STRENGTH_LEVEL = 0;
	private static final int CLEANSING_EFFECT_DURATION = 3 * 20;
	private static final int CLEANSING_RADIUS = 4;
	private static final int CLEANSING_1_COOLDOWN = 45 * 20;
	private static final int CLEANSING_2_COOLDOWN = 30 * 20;
	private static final double CLEANSING_ANGLE = 50.0;

	private static final int DIVINE_JUSTICE_DAMAGE = 5;
	private static final int DIVINE_JUSTICE_HEAL = 4;
	private static final int DIVINE_JUSTICE_CRIT_HEAL = 1;

	public static final int CELESTIAL_1_FAKE_ID = 100361;
	public static final int CELESTIAL_2_FAKE_ID = 100362;
	private static final int CELESTIAL_COOLDOWN = 40 * 20;
	private static final int CELESTIAL_1_DURATION = 10 * 20;
	private static final int CELESTIAL_2_DURATION = 12 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	public static final String CELESTIAL_1_TAGNAME = "Celestial_1";
	public static final String CELESTIAL_2_TAGNAME = "Celestial_2";
	private static final double CELESTIAL_1_DAMAGE_MULTIPLIER = 1.20;
	private static final double CELESTIAL_2_DAMAGE_MULTIPLIER = 1.35;

	private static final int HEALING_RADIUS = 12;
	private static final int HEALING_1_HEAL = 10;
	private static final int HEALING_2_HEAL = 16;
	private static final double HEALING_DOT_ANGLE = 0.33;
	private static final int HEALING_1_COOLDOWN = 20 * 20;
	private static final int HEALING_2_COOLDOWN = 15 * 20;

	private static final int PASSIVE_HEAL_AMOUNT = 1;
	private static final int PASSIVE_HEAL_RADIUS = 5;
	private static final double PASSIVE_HP_THRESHOLD = 10.0;

	public ClericClass(Plugin plugin, Random random) {
		super(plugin, random);
	}


	@Override
	public boolean has2SecondTrigger() {
		return true;
	}

	@Override
	public boolean has1SecondTrigger() {
		return true;
	}

	@Override
	public void PeriodicTrigger(Player player, boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		//	Don't trigger this if dead!
		if (!player.isDead()) {
			boolean threeSeconds = ((originalTime % 3) == 0);

			if (threeSeconds) {
				// Passive Heal Radius
				World world = player.getWorld();
				for (Player p : PlayerUtils.getNearbyPlayers(player, PASSIVE_HEAL_RADIUS, false)) {
					if (p.getHealth() <= PASSIVE_HP_THRESHOLD) {
						PlayerUtils.healPlayer(p, PASSIVE_HEAL_AMOUNT);
						ParticleUtils.playParticlesInWorld(world, Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.03, 0.03, 0.03, 0.001);
					}
				}

				// 	Rejuvenation
				int rejuvenation = ScoreboardUtils.getScoreboardValue(player, "Rejuvenation");
				if (rejuvenation > 0) {
					for (Player p : PlayerUtils.getNearbyPlayers(player, REJUVENATION_RADIUS, true)) {
						//	If this is us or we're allowing anyone to get it.
						if (p == player || rejuvenation > 1) {
							double oldHealth = p.getHealth();
							PlayerUtils.healPlayer(p, REJUVENATION_HEAL_AMOUNT);
							if (p.getHealth() > oldHealth) {
								ParticleUtils.playParticlesInWorld(world, Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
							}
						}
					}
				}
			}

			if (twoHertz) {
				int cleansing = ScoreboardUtils.getScoreboardValue(player, "Cleansing");
				if (cleansing > 0) {
					if (mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CLEANSING_FAKE)) {
						ParticleUtils.playParticlesInWorld(player.getWorld(), Particle.WATER_DROP, player.getLocation().add(0, 2, 0), 150, 2.5, 2, 2.5, 0.001);
						ParticleUtils.playParticlesInWorld(player.getWorld(), Particle.VILLAGER_HAPPY, player.getLocation().add(0, 2, 0), 20, 2, 1.5, 2, 0.001);

						for (Player e : PlayerUtils.getNearbyPlayers(player, CLEANSING_RADIUS, true)) {
							PotionUtils.clearNegatives(mPlugin, e);

							if (e.getFireTicks() > 1) {
								e.setFireTicks(1);
							}

							// TODO: This should use the potion manager!
							e.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, CLEANSING_EFFECT_DURATION, CLEANSING_STRENGTH_LEVEL, true, true));
							if (cleansing > 1) {
								e.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, CLEANSING_EFFECT_DURATION, CLEANSING_RESIST_LEVEL, true, true));
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		//	Sanctified
		if (EntityUtils.isUndead(damager)) {
			if (damager instanceof Skeleton) {
				Skeleton skelly = (Skeleton)damager;
				ItemStack mainHand = skelly.getEquipment().getItemInMainHand();
				if (mainHand != null && mainHand.getType() == Material.BOW) {
					return true;
				}
			}

			int sanctified = ScoreboardUtils.getScoreboardValue(player, "Sanctified");
			if (sanctified > 0) {
				double extraDamage = sanctified == 1 ? SANCTIFIED_1_DAMAGE : SANCTIFIED_2_DAMAGE;
				EntityUtils.damageEntity(mPlugin, damager, extraDamage, player);

				MovementUtils.KnockAway(player, damager, SANCTIFIED_KNOCKBACK_SPEED);

				Location loc = damager.getLocation();
				ParticleUtils.playParticlesInWorld(player.getWorld(), Particle.END_ROD, loc.add(0, 1, 0), 5, 0.35, 0.35, 0.35, 0.001);

				if (sanctified > 1) {
					damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SANCTIFIED_EFFECT_DURATION, SANCTIFIED_EFFECT_LEVEL, false, true));
				}
			}
		}
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		//	DivineJustice
		{
			if (PlayerUtils.isCritical(player) && cause != DamageCause.PROJECTILE) {
				if (EntityUtils.isUndead(damagee)) {

					int divineJustice = ScoreboardUtils.getScoreboardValue(player, "DivineJustice");
					if (divineJustice > 0) {
						EntityUtils.damageEntity(mPlugin, damagee, DIVINE_JUSTICE_DAMAGE, player);

						PlayerUtils.healPlayer(player, DIVINE_JUSTICE_CRIT_HEAL);

						World world = player.getWorld();
						Location loc = damagee.getLocation();
						ParticleUtils.playParticlesInWorld(world, Particle.CRIT_MAGIC, loc.add(0, 1, 0), 20, 0.25, 0.5, 0.5, 0.001);
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
						ParticleUtils.playParticlesInWorld(world, Particle.CRIT_MAGIC, loc.add(0, 1, 0), 20, 0.25, 0.25, 0.25, 0.001);
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
							int rand = mRandom.nextInt(4);
							if (rand == 0 || rand == 1) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, 20 * 20, 0, "Splash Potion of Regeneration");
							} else if (rand == 2) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, 20 * 20, 0, "Splash Potion of Absorption");
							} else {
								potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, 20 * 20, 0, "Splash Potion of Speed");
							}
						} else {
							int rand = mRandom.nextInt(5);
							if (rand == 0) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, 50 * 20, 0, "Splash Potion of Regeneration");
							} else if (rand == 1) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, 50 * 20, 0, "Splash Potion of Absorption");
							} else if (rand == 2) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, 50 * 20, 0, "Splash Potion of Speed");
							} else if (rand == 3) {
								potions = ItemUtils.createStackedPotions(PotionEffectType.INCREASE_DAMAGE, 1, 50 * 20, 0, "Splash Potion of Strength");
							} else {
								potions = ItemUtils.createStackedPotions(PotionEffectType.DAMAGE_RESISTANCE, 1, 50 * 20, 0, "Splash Potion of Resistance");
							}
						}

						ItemUtils.addPotionEffect(potions, PotionInfo.HEALING);

						World world = Bukkit.getWorld(player.getWorld().getName());
						Location pos = (player.getLocation()).add(0,2,0);
						//EntityUtils.spawnCustomSplashPotion(world, player, healPot, pos);
						EntityUtils.spawnCustomSplashPotion(world, player, potions, pos);
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
										   ThrownPotion potion, PotionSplashEvent event) {
		// Call the base class to make sure effects are correctly applied to other players
		super.PlayerSplashPotionEvent(player, affectedEntities, potion, event);

		// HeavenlyBoon
		int heavenlyBoon = ScoreboardUtils.getScoreboardValue(player, "HeavenlyBoon");
		if (heavenlyBoon > 0) {
			double range = potion.getLocation().distance(player.getLocation());
			if (range <= HEAVENLY_BOON_TRIGGER_RANGE) {
				PotionMeta meta = (PotionMeta)potion.getItem().getItemMeta();
				List<PotionEffect> effectList = PotionUtils.getEffects(meta);

				for (Player p : PlayerUtils.getNearbyPlayers(player, HEAVENLY_BOON_RADIUS, true)) {
					for (PotionEffect effect : effectList) {
						PotionUtils.applyPotion(mPlugin, p, effect);
					}
				}

				return false;
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
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CELESTIAL_BLESSING)) {
						World world = player.getWorld();
						Spells fakeID = celestial == 1 ? Spells.CELESTIAL_FAKE_1 : Spells.CELESTIAL_FAKE_2;
						int duration = celestial == 1 ? CELESTIAL_1_DURATION : CELESTIAL_2_DURATION;

						for (Player p : PlayerUtils.getNearbyPlayers(player, CELESTIAL_RADIUS, true)) {
							mPlugin.mTimers.AddCooldown(p.getUniqueId(), fakeID, duration);

							p.setMetadata(celestial == 1 ? CELESTIAL_1_TAGNAME : CELESTIAL_2_TAGNAME, new FixedMetadataValue(mPlugin, 0));

							Location loc = p.getLocation();
							ParticleUtils.playParticlesInWorld(world, Particle.VILLAGER_HAPPY, loc.add(0, 1, 0), 100, 2.0, 0.75, 2.0, 0.001);
							world.playSound(loc, "entity.player.levelup", 0.4f, 1.5f);
						}

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CELESTIAL_BLESSING, CELESTIAL_COOLDOWN);
					}
				}
			} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {

				ItemStack offHand = player.getInventory().getItemInOffHand();
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if ((offHand != null && offHand.getType() == Material.SHIELD) || (mainHand != null && mainHand.getType() == Material.SHIELD)) {
					int cleansing = ScoreboardUtils.getScoreboardValue(player, "Cleansing");
					int healing = ScoreboardUtils.getScoreboardValue(player, "Healing");
					if (cleansing > 0 && (player.getLocation()).getPitch() < -CLEANSING_ANGLE) {
						activateCleansing(player, cleansing);
					} else if (healing > 0) {
						activateHealing(player, healing);
					}
				}
			}
		}
	}

	private void activateCleansing(Player player, int cleansing) {
		if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.CLEANSING)) {
			int cooldown = (cleansing == 1) ? CLEANSING_1_COOLDOWN : CLEANSING_2_COOLDOWN;
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.45f, 0.8f);
			mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CLEANSING, cooldown);
			mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.CLEANSING_FAKE, CLEANSING_DURATION);
		}
	}

	private void activateHealing(Player player, int healing) {
		if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.HEALING)) {
			Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
			World world = player.getWorld();
			int healAmount = healing == 1 ? HEALING_1_HEAL : HEALING_2_HEAL;

			for (Player p : PlayerUtils.getNearbyPlayers(player, HEALING_RADIUS, false)) {
				Vector toMobVector = p.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
				// Only heal players in the correct direction
				// Only heal players that have a class score > 0 (so it doesn't work on arena contenders)
				if (playerDir.dot(toMobVector) > HEALING_DOT_ANGLE && ScoreboardUtils.getScoreboardValue(player, "Class") > 0) {
					PlayerUtils.healPlayer(p, healAmount);

					Location loc = p.getLocation();

					ParticleUtils.playParticlesInWorld(world, Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
					ParticleUtils.playParticlesInWorld(world, Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001);
					player.getWorld().playSound(loc, "block.enchantment_table.use", 2.0f, 1.6f);
					player.getWorld().playSound(loc, "entity.player.levelup", 0.05f, 1.0f);
				}
			}

			player.getWorld().playSound(player.getLocation(), "block.enchantment_table.use", 2.0f, 1.6f);
			player.getWorld().playSound(player.getLocation(), "entity.player.levelup", 0.05f, 1.0f);

			ParticleUtils.explodingConeEffect(mPlugin, player, HEALING_RADIUS, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, HEALING_DOT_ANGLE);

			int cooldown = healing == 1 ? HEALING_1_COOLDOWN : HEALING_2_COOLDOWN;
			mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.HEALING, cooldown);
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
