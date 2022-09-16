package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellKaulsFury;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Witch;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class VerdantMinibossBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_verdantmini";
	public static final int detectionRange = 70;

	private static final String CRYSTAL_TAG = "Crystal";
	private static final int FURY_PERIOD = 5 * 20;
	private static final int HEIGHT = SpellKaulsFury.HEIGHT;
	private static final int RADIUS = SpellKaulsFury.RADIUS;
	private static final double DAMAGE_RADIUS = SpellKaulsFury.DAMAGE_RADIUS;
	private static final double DAMAGE = SpellKaulsFury.DAMAGE;
	private static final int IMPACT_TIME = 1 * 20;
	private static final int CHARGE_TIME = FURY_PERIOD - IMPACT_TIME;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private Player mFuryTarget;
	private Location mCrystalLoc;
	private boolean mShielded = true;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new VerdantMinibossBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public VerdantMinibossBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), detectionRange);
		for (ArmorStand stand : nearbyStands) {
			if (stand.getScoreboardTags() != null && stand.getScoreboardTags().contains(CRYSTAL_TAG)) {
				mCrystalLoc = stand.getLocation().getBlock().getLocation();
				break;
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				if (mBoss.getLocation().distance(mSpawnLoc) > detectionRange) {
					mBoss.teleport(mSpawnLoc);
				}
			}
		}.runTaskTimer(mPlugin, 0, 5 * 20);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isValid() && !mBoss.isDead() && mShielded) {
					World world = mBoss.getWorld();
					Location loc = mBoss.getLocation();
					for (double deg = 0; deg < 360; deg += 8) {
						world.spawnParticle(Particle.DOLPHIN, loc.clone().add(1.25 * FastUtils.cosDeg(deg), 0.75, 1.25 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, 0);
						world.spawnParticle(Particle.DOLPHIN, loc.clone().add(1.25 * FastUtils.cosDeg(deg), 1.25, 1.25 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, 0);
						world.spawnParticle(Particle.DOLPHIN, loc.clone().add(1.25 * FastUtils.cosDeg(deg), 1.75, 1.25 * FastUtils.sinDeg(deg)), 1, 0, 0, 0, 0);
					}

					world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 1, 0), 10, 1, 1, 1);
				} else {
					this.cancel();
					return;
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		mBoss.setRemoveWhenFarAway(false);
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.YELLOW, BarStyle.SEGMENTED_10, null);
		super.constructBoss(SpellManager.EMPTY, Arrays.asList(new SpellBlockBreak(boss), new SpellShieldStun(10 * 20)), detectionRange, bossBar);
	}

	@Override
	public void init() {
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setGlowing(true);
	}

	@Override
	public void nearbyBlockBreak(BlockBreakEvent event) {
		if (mCrystalLoc != null && mCrystalLoc.equals(event.getBlock().getLocation())) {
			mFuryTarget = event.getPlayer();
			activateFury();
		}
	}

	@Override
	public boolean hasNearbyBlockBreakTrigger() {
		return true;
	}

	private void activateFury() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mShielded || !mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				World world = mBoss.getWorld();
				mFuryTarget.playSound(mFuryTarget.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2, 1.5f);
				world.playSound(mFuryTarget.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);

				new BukkitRunnable() {
					int mT = 0;
					Location mLocation = mFuryTarget.getLocation().clone().add(0, HEIGHT, 0);

					@Override
					public void run() {
						if (mFuryTarget == null || !mFuryTarget.isOnline() || mFuryTarget.isDead() || mFuryTarget.getLocation().distance(mBoss.getLocation()) > detectionRange) {
							mFuryTarget = EntityUtils.getNearestPlayer(mBoss.getLocation(), detectionRange);
						}

						if (!mShielded || !mBoss.isValid() || mBoss.isDead()) {
							this.cancel();
							return;
						}

						if (mT < CHARGE_TIME) {
							mLocation = mFuryTarget.getLocation().add(0, HEIGHT, 0);

							double completionRatio = ((double) mT) / CHARGE_TIME;
							double chargingRadius = RADIUS * completionRatio;
							world.spawnParticle(Particle.SPELL_WITCH, mLocation, 5 + (int) (completionRatio * 20), chargingRadius / 2.5, chargingRadius / 2.5, chargingRadius / 2.5, 0);
							world.spawnParticle(Particle.FLAME, mLocation, 8 + (int) (completionRatio * 25), chargingRadius / 2, chargingRadius / 2, chargingRadius / 2, 0);
							world.spawnParticle(Particle.SMOKE_LARGE, mLocation, 5 + (int) (completionRatio * 20), chargingRadius / 2.5, chargingRadius / 2.5, chargingRadius / 2.5, 0);

							mFuryTarget.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, (float) (2 - completionRatio));
							mFuryTarget.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, (float) (2 - completionRatio));
							mFuryTarget.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, (float) (2 - completionRatio));
							mFuryTarget.playSound(mLocation, Sound.BLOCK_LAVA_POP, 3.5f, (float) (1.5 * (2 - completionRatio)));
						} else if (mT < CHARGE_TIME + IMPACT_TIME) {
							if (mT == CHARGE_TIME) {
								world.playSound(mLocation, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1);
							}

							// 5.0 because 5 ticks and impact time is in ticks, and to make it a double
							mLocation = mLocation.subtract(0, (5.0 * HEIGHT) / IMPACT_TIME, 0);
							world.spawnParticle(Particle.SPELL_WITCH, mLocation, 25, RADIUS / 2.5, RADIUS / 2.5, RADIUS / 2.5, 0);
							world.spawnParticle(Particle.FLAME, mLocation, 18, RADIUS / 2.0, RADIUS / 2.0, RADIUS / 2.0, 0);
							world.spawnParticle(Particle.CLOUD, mLocation, 18, RADIUS / 2.0, RADIUS / 2.0, RADIUS / 2.0, 0);
							world.spawnParticle(Particle.SMOKE_LARGE, mLocation, 25, RADIUS / 2.5, RADIUS / 2.5, RADIUS / 2.5, 0);
							world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, mLocation, 1, RADIUS / 2.5, RADIUS / 2.5, RADIUS / 2.5, 0);

							mFuryTarget.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, 1);
							mFuryTarget.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, 1);
							mFuryTarget.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, 1);
							world.playSound(mLocation, Sound.ENTITY_BLAZE_SHOOT, 1, 2.0f);
						} else {
							for (Player player : PlayerUtils.playersInRange(mLocation, DAMAGE_RADIUS, true)) {
								DamageUtils.damage(mBoss, player, DamageType.BLAST, DAMAGE, null, false, true, "Kaul's Fury");
							}

							//Give 0.5 blocks of leeway for hitting the boss, don't want to make it about being super precise
							if (mLocation.distance(mBoss.getLocation()) <= RADIUS + 0.5) {
								removeShield();
							}

							for (LivingEntity mob : EntityUtils.getNearbyMobs(mLocation, DAMAGE_RADIUS)) {
								// No damager so that the mobs don't target the boss
								DamageUtils.damage(null, mob, DamageType.BLAST, DAMAGE / 2, null, false, true, "Kaul's Fury");
							}

							world.playSound(mLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1);

							ParticleUtils.explodingRingEffect(mPlugin, mLocation, RADIUS, 1, 4,
									Arrays.asList(
											new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
												world.spawnParticle(Particle.FLAME, location, 1, 0.1, 0.1, 0.1, 0.1);
												world.spawnParticle(Particle.SMOKE_LARGE, location, 1, 0.1, 0.1, 0.1, 0.1);
											})
									));

							this.cancel();
						}

						mT += 5;
					}
				}.runTaskTimer(mPlugin, 0, 5);
			}
		}.runTaskTimer(mPlugin, 0, FURY_PERIOD);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mShielded) {
			event.setCancelled(true);
			if (event.getDamager() instanceof Arrow arrow && arrow.hasCustomEffect(PotionEffectType.SLOW)) {
				arrow.removeCustomEffect(PotionEffectType.SLOW);
			}
			if (event.getSource() instanceof Player player) {
				player.sendMessage(ChatColor.AQUA + "The shield absorbs your attack.");
				player.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
			}
		}
	}

	private void removeShield() {
		mShielded = false;
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.sendMessage(ChatColor.AQUA + "The shield shatters.");
		}
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 1);
		world.spawnParticle(Particle.CRIT, mBoss.getLocation().add(0, 1, 0), 15, 0.5, 0, 0.5);

		//Add new spell
		SpellManager spellManager = getSpellManager();
		if (spellManager != null) {
			changePhase(spellManager, Arrays.asList(new SpellBlockBreak(mBoss), new SpellShieldStun(10 * 20)), null);
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof Player)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		if (event.getEntity().equals(mFuryTarget)) {
			mFuryTarget = EntityUtils.getNearestPlayer(mBoss.getLocation(), detectionRange);
		}
	}

	private @Nullable SpellManager getSpellManager() {
		if (mBoss instanceof Vindicator) {
			return getTeccatlSpellManager();
		} else if (mBoss instanceof Witch) {
			return getTlorixSpellManager();
		}

		return null;
	}

	private SpellManager getTeccatlSpellManager() {
		World world = mBoss.getWorld();
		return new SpellManager(Arrays.asList(
				new SpellBaseCharge(mPlugin, mBoss, 32, 80, 25, false,
						0, 0, 0,
						true,
						// Warning sound/particles at boss location and slow boss
						(LivingEntity player) -> {
							world.spawnParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation(), 50, 2, 2, 2);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1, 1.5f);
							mBoss.setAI(false);
						},
						// Warning particles
						(Location loc) -> {
							world.spawnParticle(Particle.CRIT, loc, 2, 0.65, 0.65, 0.65, 0);
						},
						// Charge attack sound/particles at boss location
						(LivingEntity player) -> {
							world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1.5f);
						},
						// Attack hit a player
						(LivingEntity target) -> {
							world.spawnParticle(Particle.BLOCK_CRACK, target.getEyeLocation(), 5, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData());
							world.spawnParticle(Particle.BLOCK_CRACK, target.getEyeLocation(), 12, 0.4, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
							BossUtils.blockableDamage(mBoss, target, DamageType.MELEE, 20);
						},
						// Attack particles
						(Location loc) -> {
							world.spawnParticle(Particle.FLAME, loc, 4, 0.5, 0.5, 0.5, 0.075);
							world.spawnParticle(Particle.CRIT, loc, 8, 0.5, 0.5, 0.5, 0);
						},
						// Ending particles on boss
						() -> {
							world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 125, 0.3, 0.3, 0.3, 0.15);
							world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1.5f);
							mBoss.setAI(true);
						})
					));
	}

	private SpellManager getTlorixSpellManager() {
		return new SpellManager(Arrays.asList(
				new SpellBombToss(mPlugin, mBoss, detectionRange, 2, 50, 160,
						(World world, TNTPrimed tnt, Location loc) -> {
							world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
							world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
							world.spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.4);

							for (Player player : PlayerUtils.playersInRange(loc, 8, true)) {
								if (player.hasLineOfSight(tnt)) {
									double multiplier = (8 - player.getLocation().distance(loc)) / 8;
									BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, 32 * multiplier);
									player.setFireTicks((int)(20 * 8 * multiplier));
									EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), (int) (20 * 8 * multiplier), player, mBoss);
								}
							}
						})
			));
	}
}
