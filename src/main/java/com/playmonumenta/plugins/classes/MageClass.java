package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

/*
    ManaLance
    FrostNova
    Prismatic
    Magma
    ArcaneStrike
    ArcaneStrikeHits
    Elemental
    SpellShock
*/

public class MageClass extends BaseClass {
	public class SpellShockedMob {
		public LivingEntity mob;
		public int ticksLeft;
		public Player initiator;

		public SpellShockedMob(LivingEntity inMob, int ticks, Player inInitiator) {
			mob = inMob;
			ticksLeft = ticks;
			initiator = inInitiator;
		}
	}

	private static final int MANA_LANCE_1_DAMAGE = 8;
	private static final int MANA_LANCE_2_DAMAGE = 10;
	private static final int MANA_LANCE_1_COOLDOWN = 5 * 20;
	private static final int MANA_LANCE_2_COOLDOWN = 3 * 20;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);
	private static final int MANA_LANCE_STAGGER_DURATION = (int)(0.95 * 20);

	private static final float FROST_NOVA_RADIUS = 6.0f;
	private static final int FROST_NOVA_1_DAMAGE = 3;
	private static final int FROST_NOVA_2_DAMAGE = 6;
	private static final int FROST_NOVA_EFFECT_LVL = 2;
	private static final int FROST_NOVA_COOLDOWN = 18 * 20;
	private static final int FROST_NOVA_DURATION = 8 * 20;

	private static final float PRISMATIC_SHIELD_RADIUS = 4.0f;
	private static final int PRISMATIC_SHIELD_TRIGGER_HEALTH = 6;
	private static final int PRISMATIC_SHIELD_EFFECT_LVL_1 = 1;
	private static final int PRISMATIC_SHIELD_EFFECT_LVL_2 = 2;
	private static final int PRISMATIC_SHIELD_1_DURATION = 12 * 20;
	private static final int PRISMATIC_SHIELD_2_DURATION = 12 * 20;
	private static final int PRISMATIC_SHIELD_COOLDOWN = 80 * 20;
	private static final float PRISMATIC_SHIELD_KNOCKBACK_SPEED = 0.7f;
	private static final int PRISMATIC_SHIELD_1_DAMAGE = 3;
	private static final int PRISMATIC_SHIELD_2_DAMAGE = 6;

	private static final int MAGMA_SHIELD_COOLDOWN = 13 * 20;
	private static final int MAGMA_SHIELD_RADIUS = 6;
	private static final int MAGMA_SHIELD_FIRE_DURATION = 4 * 20;
	private static final int MAGMA_SHIELD_1_DAMAGE = 6;
	private static final int MAGMA_SHIELD_2_DAMAGE = 12;
	private static final float MAGMA_SHIELD_KNOCKBACK_SPEED = 0.5f;
	private static final double MAGMA_SHIELD_DOT_ANGLE = 0.33;

	private static final float ARCANE_STRIKE_RADIUS = 4.0f;
	private static final int ARCANE_STRIKE_1_DAMAGE = 5;
	private static final int ARCANE_STRIKE_2_DAMAGE = 8;
	private static final int ARCANE_STRIKE_BURN_DAMAGE_LVL_1 = 2;
	private static final int ARCANE_STRIKE_BURN_DAMAGE_LVL_2 = 4;
	private static final int ARCANE_STRIKE_COOLDOWN = 6 * 20;

	private static final int ELEMENTAL_ARROWS_ICE_DURATION = 8 * 20;
	private static final int ELEMENTAL_ARROWS_ICE_EFFECT_LVL = 1;
	private static final int ELEMENTAL_ARROWS_FIRE_DURATION = 5 * 20;
	private static final double ELEMENTAL_ARROWS_RADIUS = 4.0;

	private static final int SPELL_SHOCK_DURATION = 6 * 20;
	private static final int SPELL_SHOCK_TEST_PERIOD = 2;
	private static final int SPELL_SHOCK_DEATH_RADIUS = 3;
	private static final int SPELL_SHOCK_DEATH_DAMAGE = 3;
	private static final int SPELL_SHOCK_SPELL_RADIUS = 4;
	private static final int SPELL_SHOCK_SPELL_DAMAGE = 3;
	private static final int SPELL_SHOCK_REGEN_DURATION = 51;
	private static final int SPELL_SHOCK_REGEN_AMPLIFIER = 1;
	private static final int SPELL_SHOCK_SPEED_DURATION = 120;
	private static final int SPELL_SHOCK_SPEED_AMPLIFIER = 0;
	private static final int SPELL_SHOCK_VULN_DURATION = 4 * 20;
	private static final int SPELL_SHOCK_VULN_AMPLIFIER = 3; // 20%
	private static final int SPELL_SHOCK_STAGGER_DURATION = (int)(0.6 * 20);
	private static final Particle.DustOptions SPELL_SHOCK_COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	private static double PASSIVE_DAMAGE = 1.5;

	private static Map<UUID, SpellShockedMob>mSpellShockedMobs = new HashMap<UUID, SpellShockedMob>();

	private World mWorld;

	public MageClass(Plugin plugin, Random random, World world) {
		super(plugin, random);

		mWorld = world;

		// SpellShock task to process tagged mobs
		// pri
		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				Iterator<Map.Entry<UUID, SpellShockedMob>>it = mSpellShockedMobs.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<UUID, SpellShockedMob> entry = it.next();
					SpellShockedMob shocked = entry.getValue();

					Location loc = shocked.mob.getLocation().add(0, 1, 0);
					mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.2, 0.6, 0.2, 1);
					mWorld.spawnParticle(Particle.REDSTONE, loc, 4, 0.3, 0.6, 0.3, SPELL_SHOCK_COLOR);

					if (shocked.mob.isDead() || shocked.mob.getHealth() <= 0) {
						// Mob has died - trigger effects
						mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 50, 1, 1, 1, 0.001);
						mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 100, 1, 1, 1, 0.25);
						world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.0f);
						for (LivingEntity nearbyMob : EntityUtils.getNearbyMobs(shocked.mob.getLocation(), SPELL_SHOCK_DEATH_RADIUS)) {
							EntityUtils.damageEntity(plugin, nearbyMob, SPELL_SHOCK_DEATH_DAMAGE, shocked.initiator);
							nearbyMob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
							                                           SPELL_SHOCK_VULN_AMPLIFIER, false, true));
						}

						it.remove();
						continue;
					}

					shocked.ticksLeft -= SPELL_SHOCK_TEST_PERIOD;
					if (shocked.ticksLeft <= 0) {
						it.remove();
						continue;
					}
				}
			}

		}.runTaskTimer(plugin, 1, SPELL_SHOCK_TEST_PERIOD);
	}

	// Spell Shock
	private void SpellDamageMob(Plugin plugin, LivingEntity mob, float dmg, Player player) {
		SpellShockedMob shocked = mSpellShockedMobs.get(mob.getUniqueId());
		if (shocked != null) {
			// Hit a shocked mob with a real spell - extra damage

			int spellShock = ScoreboardUtils.getScoreboardValue(player, "SpellShock");
			if (spellShock > 1) {
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
				                                 new PotionEffect(PotionEffectType.REGENERATION,
				                                                  SPELL_SHOCK_REGEN_DURATION,
				                                                  SPELL_SHOCK_REGEN_AMPLIFIER, true, true));
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
				                                 new PotionEffect(PotionEffectType.SPEED,
				                                                  SPELL_SHOCK_SPEED_DURATION,
				                                                  SPELL_SHOCK_SPEED_AMPLIFIER, true, true));
			}

			// Consume the "charge"
			mSpellShockedMobs.remove(mob.getUniqueId());

			Location loc = shocked.mob.getLocation().add(0, 1, 0);
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 100, 1, 1, 1, 0.001);
			mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 75, 1, 1, 1, 0.25);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.5f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.0f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 1.5f);
			for (Entity nearbyMob : EntityUtils.getNearbyMobs(shocked.mob.getLocation(), SPELL_SHOCK_SPELL_RADIUS)) {
				// Only damage hostile mobs and specifically not the mob originally hit
				if (nearbyMob != mob) {
					mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SPELL_SHOCK_STAGGER_DURATION, 10, true, false));
					EntityUtils.damageEntity(plugin, (LivingEntity)nearbyMob, SPELL_SHOCK_SPELL_DAMAGE, player);
					((LivingEntity)nearbyMob).addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
					                                                           SPELL_SHOCK_VULN_AMPLIFIER, false, true));
				}
			}

			dmg += SPELL_SHOCK_SPELL_DAMAGE;
		}

		// Apply damage to the hit mob all in one shot
		if (dmg > 0) {
			EntityUtils.damageEntity(mPlugin, mob, dmg, player);
		}

		// Make sure to apply vulnerability after damage
		if (shocked != null) {
			mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
												 SPELL_SHOCK_VULN_AMPLIFIER, false, true));
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();

		//  Arcane Strike
		{
			if (InventoryUtils.isWandItem(mainHand)) {
				int arcaneStrike = ScoreboardUtils.getScoreboardValue(player, "ArcaneStrike");
				if (arcaneStrike > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ARCANE_STRIKE)) {
						if (!MetadataUtils.checkOnceThisTick(mPlugin, player, Constants.ENTITY_DAMAGE_NONCE_METAKEY)) {
							int extraDamage = arcaneStrike == 1 ? ARCANE_STRIKE_1_DAMAGE : ARCANE_STRIKE_2_DAMAGE;

							for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ARCANE_STRIKE_RADIUS)) {
								int dmg = extraDamage;

								// Arcane strike extra fire damage
								// Check if (the mob is burning AND was not set on fire this tick) OR the mob has slowness
								//

								if (arcaneStrike > 0 && ((mob.getFireTicks() > 0 &&
								                          MetadataUtils.checkOnceThisTick(mPlugin, mob, Constants.ENTITY_COMBUST_NONCE_METAKEY)) ||
								                         mob.hasPotionEffect(PotionEffectType.SLOW))) {
									dmg += (arcaneStrike == 1 ? ARCANE_STRIKE_BURN_DAMAGE_LVL_1 : ARCANE_STRIKE_BURN_DAMAGE_LVL_2);
								}

								EntityUtils.damageEntity(mPlugin, mob, dmg, player);
							}

							Location loc = damagee.getLocation();
							mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc.add(0, 1, 0), 50, 2.5, 1, 2.5, 0.001);
							mWorld.spawnParticle(Particle.SPELL_WITCH, loc.add(0, 1, 0), 200, 2.5, 1, 2.5, 0.001);
							mWorld.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 1.5f);

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.ARCANE_STRIKE, ARCANE_STRIKE_COOLDOWN);
						}
					}
				}
			}
		}

		// Spell Shock (add static to mobs here)
		if (InventoryUtils.isWandItem(mainHand) && cause.equals(DamageCause.ENTITY_ATTACK)) {
			int spellShock = ScoreboardUtils.getScoreboardValue(player, "SpellShock");
			if (spellShock > 0) {
				mSpellShockedMobs.put(damagee.getUniqueId(),
				                      new SpellShockedMob(damagee, SPELL_SHOCK_DURATION, player));
			}
		}

		if (InventoryUtils.isWandItem(mainHand)) {
			EntityUtils.damageEntity(mPlugin, damagee, PASSIVE_DAMAGE, player);
		}

		return true;
	}


	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		//  Magma Shield
		{
			//  If we're sneaking and we block with a shield we can attempt to trigger the ability.
			if (player.isSneaking()) {
				ItemStack offHand = player.getInventory().getItemInOffHand();
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if (((offHand.getType() == Material.SHIELD && InventoryUtils.isWandItem(mainHand)) || (mainHand.getType() == Material.SHIELD))
				    && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && !blockClicked.isInteractable()) {

					int magmaShield = ScoreboardUtils.getScoreboardValue(player, "Magma");
					if (magmaShield > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.MAGMA_SHIELD)) {
							Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
							for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), MAGMA_SHIELD_RADIUS)) {
								Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
								if (playerDir.dot(toMobVector) > MAGMA_SHIELD_DOT_ANGLE) {
									MovementUtils.KnockAway(player, mob, MAGMA_SHIELD_KNOCKBACK_SPEED);
									mob.setFireTicks(MAGMA_SHIELD_FIRE_DURATION);

									int extraDamage = magmaShield == 1 ? MAGMA_SHIELD_1_DAMAGE : MAGMA_SHIELD_2_DAMAGE;
									SpellDamageMob(mPlugin, mob, extraDamage, player);
								}
							}

							ParticleUtils.explodingConeEffect(mPlugin, player, MAGMA_SHIELD_RADIUS, Particle.FLAME, 0.75f, Particle.LAVA, 0.25f, MAGMA_SHIELD_DOT_ANGLE);

							mWorld.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.5f);
							mWorld.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.25f, 1.0f);

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.MAGMA_SHIELD, MAGMA_SHIELD_COOLDOWN);
						}
					}
				}
			} else {
				//Mana Lance
				if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !blockClicked.isInteractable())) {
					int manaLance = ScoreboardUtils.getScoreboardValue(player, "ManaLance");
					ItemStack mainHand = player.getInventory().getItemInMainHand();
					if (InventoryUtils.isWandItem(mainHand)) {
						if (manaLance > 0 && player.getGameMode() != GameMode.SPECTATOR) {
							if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.MANA_LANCE)) {

								int extraDamage = manaLance == 1 ? MANA_LANCE_1_DAMAGE : MANA_LANCE_2_DAMAGE;
								int cooldown = manaLance == 1 ? MANA_LANCE_1_COOLDOWN : MANA_LANCE_2_COOLDOWN;

								Location loc = player.getEyeLocation();
								Vector dir = loc.getDirection();
								loc.add(dir);
								mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125);

								for (int i = 0; i < 8; i++) {
									loc.add(dir);

									mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05, 0.05, 0.05, 0.025);
									mWorld.spawnParticle(Particle.REDSTONE, loc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR);

									if (loc.getBlock().getType().isSolid()) {
										loc.subtract(dir.multiply(0.5));
										mWorld.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125);
										mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
										break;
									}
									for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 0.5)) {
										SpellDamageMob(mPlugin, mob, extraDamage, player);
										mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, MANA_LANCE_STAGGER_DURATION, 10, true, false));
									}
								}

								mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.MANA_LANCE, cooldown);
								mWorld.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
							}
						}
					}
				}
			}
		}

		//  Frost Nova
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if (InventoryUtils.isWandItem(mainHand)) {
					int frostNova = ScoreboardUtils.getScoreboardValue(player, "FrostNova");
					if (frostNova > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.FROST_NOVA)) {
							player.setFireTicks(0);
							for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), FROST_NOVA_RADIUS)) {
								int extraDamage = frostNova == 1 ? FROST_NOVA_1_DAMAGE : FROST_NOVA_2_DAMAGE;
								SpellDamageMob(mPlugin, mob, extraDamage, player);

								mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_NOVA_DURATION, FROST_NOVA_EFFECT_LVL, true, false));
								if (frostNova > 1) {
									EntityUtils.applyFreeze(mPlugin, FROST_NOVA_DURATION, mob);
								}

								mob.setFireTicks(0);
							}

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.FROST_NOVA, FROST_NOVA_COOLDOWN);

							Location loc = player.getLocation();
							mWorld.spawnParticle(Particle.SNOW_SHOVEL, loc.add(0, 1, 0), 400, 4, 1, 4, 0.001);
							mWorld.spawnParticle(Particle.CRIT_MAGIC, loc.add(0, 1, 0), 200, 4, 1, 4, 0.001);
							mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f);
						}
					}
				}
			}
		}
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//  Elemental Arrows
		{
			int elementalArrows = ScoreboardUtils.getScoreboardValue(player, "Elemental");
			if (elementalArrows > 0) {
				if (arrow.hasMetadata("FireArrow")) {
					if (damagee instanceof Stray) {
						event.setDamage(event.getDamage() + 8);
					}

					// Trigger spellshock if mob is shocked
					SpellDamageMob(mPlugin, damagee, 0, player);
					damagee.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);

					if (elementalArrows == 2) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ELEMENTAL_ARROWS_RADIUS)) {
							if (mob != damagee) {
								// Trigger spellshock if mob is shocked
								SpellDamageMob(mPlugin, mob, 0, player);
								mob.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
							}
						}
					}
				} else if (arrow.hasMetadata("IceArrow")) {
					if (damagee instanceof Blaze) {
						event.setDamage(event.getDamage() + 8);
					}

					// Trigger spellshock if mob is shocked
					SpellDamageMob(mPlugin, damagee, 0, player);
					damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));

					if (elementalArrows == 2) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ELEMENTAL_ARROWS_RADIUS)) {
							if (mob != damagee) {
								mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));
								// Trigger spellshock if mob is shocked
								SpellDamageMob(mPlugin, mob, 0, player);
								if (mob instanceof Blaze) {
									EntityUtils.damageEntity(mPlugin, mob, 8, player);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		//  Elemental Arrows
		{
			int elementalArrows = ScoreboardUtils.getScoreboardValue(player, "Elemental");
			if (elementalArrows > 0) {
				//  If sneaking, Ice Arrow
				if (player.isSneaking()) {
					arrow.setFireTicks(0);
					arrow.setMetadata("IceArrow", new FixedMetadataValue(mPlugin, 0));
					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SNOW_SHOVEL);
				}
				//  else Fire Arrow
				else {
					arrow.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
					arrow.setMetadata("FireArrow", new FixedMetadataValue(mPlugin, 0));
					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
				}
			}
		}
	}

	@Override
	public boolean PlayerDamagedEvent(Player player, DamageCause cause, double damage) {
		if (!player.isDead()) {
			double correctHealth = player.getHealth() - damage;
			if (correctHealth > 0 && correctHealth <= PRISMATIC_SHIELD_TRIGGER_HEALTH) {
				int prismatic = ScoreboardUtils.getScoreboardValue(player, "Prismatic");
				if (prismatic > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PRISMATIC_SHIELD)) {
						int effectLevel = prismatic == 1 ? PRISMATIC_SHIELD_EFFECT_LVL_1 : PRISMATIC_SHIELD_EFFECT_LVL_2;
						int duration = prismatic == 1 ? PRISMATIC_SHIELD_1_DURATION : PRISMATIC_SHIELD_2_DURATION;
						float prisDamage = prismatic == 1 ? PRISMATIC_SHIELD_1_DAMAGE : PRISMATIC_SHIELD_2_DAMAGE;

						for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), PRISMATIC_SHIELD_RADIUS)) {
							SpellDamageMob(mPlugin, mob, prisDamage, player);
							MovementUtils.KnockAway(player, mob, PRISMATIC_SHIELD_KNOCKBACK_SPEED);
						}

						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.ABSORPTION, duration, effectLevel, true, true));
						mWorld.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5);
						mWorld.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1);
						mWorld.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
						MessagingUtils.sendActionBarMessage(mPlugin, player, "Prismatic Shield has been activated");

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PRISMATIC_SHIELD, PRISMATIC_SHIELD_COOLDOWN);
					}
				}
			}
		}
		return true;
	}

}
