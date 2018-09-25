package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

/*
    CounterStrike
    Frenzy
    Riposte             [formerly Obliteration]
    DefensiveLine
    BruteForce
    Toughness
    WeaponMastery
*/

public class WarriorClass extends BaseClass {
	private static final float COUNTER_STRIKE_RADIUS = 5.0f;

	private static final int FRENZY_DURATION = 5 * 20;

	private static final int RIPOSTE_COOLDOWN = 10 * 20;
	private static final int RIPOSTE_SWORD_EFFECT_LEVEL = 1;
	private static final int RIPOSTE_SWORD_DURATION = 5 * 20;
	private static final int RIPOSTE_AXE_DURATION = 3 * 20;
	private static final int RIPOSTE_AXE_EFFECT_LEVEL = 6;
	private static final double RIPOSTE_SQRADIUS = 6.25;    //radius = 2.5, this is it squared
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;

	private static final Integer DEFENSIVE_LINE_DURATION = 14 * 20;
	private static final float DEFENSIVE_LINE_RADIUS = 8.0f;
	private static final Integer DEFENSIVE_LINE_1_COOLDOWN = 50 * 20;
	private static final Integer DEFENSIVE_LINE_2_COOLDOWN = 30 * 20;

	private static final float BRUTE_FORCE_RADIUS = 2.0f;
	private static final Integer BRUTE_FORCE_1_DAMAGE = 2;
	private static final Integer BRUTE_FORCE_2_DAMAGE = 4;
	private static final float BRUTE_FORCE_KNOCKBACK_SPEED = 0.5f;

	private static final double PASSIVE_KNOCKBACK_RESISTANCE = 0.2;

	private static final int WEAPON_MASTERY_AXE_1_DAMAGE = 2;
	private static final int WEAPON_MASTERY_AXE_2_DAMAGE = 4;
	private static final int WEAPON_MASTERY_SWORD_2_DAMAGE = 1;

	private World mWorld;

	public WarriorClass(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	@Override
	public void setupClassPotionEffects(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		_testItemInHand(player, mainHand);
		_testToughness(player);
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(Player player, LivingEntity damager, double damage) {
		if (!(damager instanceof Player)) {
			//  ABILITY: Counter Strike
			{
				//  If we're not going to succeed in our Random we probably don't want to attempt to grab the scoreboard value anyways.
				if (mRandom.nextFloat() < 0.15f) {
					int counterStrike = ScoreboardUtils.getScoreboardValue(player, "CounterStrike");
					if (counterStrike > 0) {
						Location loc = player.getLocation();
						player.spawnParticle(Particle.SWEEP_ATTACK, loc.getX(), loc.getY() + 1.5D, loc.getZ(), 20, 1.5D, 1.5D, 1.5D);
						player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.7f);

						double csDamage = counterStrike == 1 ? 12D : 24D;

						for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), COUNTER_STRIKE_RADIUS)) {
							EntityUtils.damageEntity(mPlugin, mob, csDamage, player);
						}
					}
				}
			}

			// ABILITY: Riposte
			{
				if ((player.getLocation()).distanceSquared(damager.getLocation()) < RIPOSTE_SQRADIUS) {
					// currently leaving the scoreboard as Obliteration for back-compatibility
					int riposte = ScoreboardUtils.getScoreboardValue(player, "Obliteration");
					if (riposte > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.RIPOSTE)) {
							if (!(damager instanceof Creeper)) {
								ItemStack mainHand = player.getInventory().getItemInMainHand();
								MovementUtils.KnockAway(player, damager, RIPOSTE_KNOCKBACK_SPEED);

								if (InventoryUtils.isAxeItem(mainHand) || InventoryUtils.isSwordItem(mainHand)) {
									if (riposte > 1) {
										if (InventoryUtils.isSwordItem(mainHand)) {
											player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, RIPOSTE_SWORD_DURATION, RIPOSTE_SWORD_EFFECT_LEVEL, true, true));
										} else if (InventoryUtils.isAxeItem(mainHand)) {
											damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, RIPOSTE_AXE_DURATION, RIPOSTE_AXE_EFFECT_LEVEL, true, false));
										}
									}

									mWorld.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
									mWorld.spawnParticle(Particle.SWEEP_ATTACK, (player.getLocation()).add(0, 1, 0), 18, 0.75, 0.5, 0.75, 0.001);
									mWorld.spawnParticle(Particle.CRIT_MAGIC, (player.getLocation()).add(0, 1, 0), 20, 0.75, 0.5, 0.75, 0.001);
									mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.RIPOSTE, RIPOSTE_COOLDOWN);

									return false;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();

		// The extra damage that will be applied to the hit damagee at the end of this function
		double extraDamage = 0;

		// BRUTE FORCE
		int bruteForce = ScoreboardUtils.getScoreboardValue(player, "BruteForce");
		if (bruteForce > 0 && PlayerUtils.isCritical(player) && cause != DamageCause.PROJECTILE &&
		    (InventoryUtils.isAxeItem(mainHand) || InventoryUtils.isSwordItem(mainHand) || InventoryUtils.isScytheItem(mainHand))) {

			double bruteForceDamage = bruteForce == 1 ? BRUTE_FORCE_1_DAMAGE : BRUTE_FORCE_2_DAMAGE;
			extraDamage += bruteForceDamage;

			Location loc = damagee.getLocation().add(0, damagee.getHeight() / 2, 0);
			mWorld.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 1);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.135);

			// Damage those non-hit nearby entities and knock them away
			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), BRUTE_FORCE_RADIUS)) {
				if (mob != damagee) {
					EntityUtils.damageEntity(mPlugin, mob, bruteForceDamage, player);
					MovementUtils.KnockAway(player, mob, BRUTE_FORCE_KNOCKBACK_SPEED);
				}
			}

			// Knock away just the hit entity
			MovementUtils.KnockAway(player, damagee, BRUTE_FORCE_KNOCKBACK_SPEED);
		}

		// WEAPON MASTERY
		int weaponMastery = ScoreboardUtils.getScoreboardValue(player, "WeaponMastery");
		if (InventoryUtils.isAxeItem(mainHand) && weaponMastery >= 1) {
			extraDamage += (weaponMastery == 1) ? WEAPON_MASTERY_AXE_1_DAMAGE : WEAPON_MASTERY_AXE_2_DAMAGE;
		} else if (InventoryUtils.isSwordItem(mainHand) && weaponMastery >= 2) {
			extraDamage += WEAPON_MASTERY_SWORD_2_DAMAGE;
		}

		if (extraDamage > 0) {
			EntityUtils.damageEntity(mPlugin, damagee, extraDamage, player);
		}

		return false;
	}

	@Override
	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		_testItemInHand(player, mainHand);
	}

	@Override
	public void PlayerRespawnEvent(Player player) {
		_testToughness(player);

		AttributeInstance att = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		att.setBaseValue(0);
		att.setBaseValue(PASSIVE_KNOCKBACK_RESISTANCE);
	}

	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		int frenzy = ScoreboardUtils.getScoreboardValue(player, "Frenzy");
		if (frenzy > 0) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (!InventoryUtils.isPickaxeItem(mainHand)) {
				int hasteAmp = frenzy == 1 ? 2 : 3;

				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FAST_DIGGING, FRENZY_DURATION, hasteAmp, true, true));

				Location loc = player.getLocation();
				mWorld.playSound(loc, Sound.ENTITY_POLAR_BEAR_HURT, 0.1f, 1.0f);

				if (frenzy > 1) {
					mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, FRENZY_DURATION, 0, true, true));
				}
			}
		}
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		//  Defensive Line
		{
			//  If we're sneaking and we block with a shield we can attempt to trigger the ability.
			if (player.isSneaking()) {
				ItemStack offHand = player.getInventory().getItemInOffHand();
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if ((offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD) && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
					int defensiveLine = ScoreboardUtils.getScoreboardValue(player, "DefensiveLine");
					if (defensiveLine > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.DEFENSIVE_LINE)) {
							for (Player target : PlayerUtils.getNearbyPlayers(player, DEFENSIVE_LINE_RADIUS, true)) {
								Location loc = target.getLocation();

								target.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.4f, 1.0f);
								mPlugin.mPotionManager.addPotion(target, PotionID.APPLIED_POTION,
								                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
								                                                  DEFENSIVE_LINE_DURATION,
								                                                  1, true, true));
							}

							ParticleUtils.explodingSphereEffect(mPlugin, player, DEFENSIVE_LINE_RADIUS, Particle.FIREWORKS_SPARK, 1.0f, Particle.CRIT, 1.0f);

							Integer cooldown = defensiveLine == 1 ? DEFENSIVE_LINE_1_COOLDOWN : DEFENSIVE_LINE_2_COOLDOWN;
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.DEFENSIVE_LINE, cooldown);
						}
					}
				}
			}
		}
	}

	private void _testItemInHand(Player player, ItemStack mainHand) {
		int frenzy = ScoreboardUtils.getScoreboardValue(player, "Frenzy");
		if (frenzy > 0) {
			if (InventoryUtils.isPickaxeItem(mainHand)) {
				mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
				mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.FAST_DIGGING);
			}
		}

		int weaponMastery = ScoreboardUtils.getScoreboardValue(player, "WeaponMastery");
		if (weaponMastery > 0) {
			mPlugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.DAMAGE_RESISTANCE);

			//  Player has an sword in their mainHand.
			if (InventoryUtils.isSwordItem(mainHand)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 0, true, false));
			}
		}
	}

	private void _testToughness(Player player) {
		int toughness = ScoreboardUtils.getScoreboardValue(player, "Toughness");
		if (toughness > 0) {
			int healthBoost = toughness == 1 ? 0 : 1;
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.HEALTH_BOOST, 1000000, healthBoost, true, false));
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 100, 4, true, false));
		}
	}
}
