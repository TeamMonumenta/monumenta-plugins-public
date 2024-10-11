package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.vesperidys.VesperidysBlockPlacerBoss;
import com.playmonumenta.plugins.effects.PercentAbsorption;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


/**
 * Dark Hole:
 * Summons magus adds on field which begins the summon of a calamity. By default, 1 summoner every 2 players.
 *
 * A spherical blob of squid ink appears in the centre of the arena. It periodically pulls players towards it and deals minor corruption damage.
 * The dark hole is unstable. Periodically it will shoot curvy projectiles that targets all players with a telegraph.
 *
 * The dark hole slowly becomes smaller and eventually implodes. If the magus is not killed in time, the dark hole
 * will explode, dealing % damage to everyone based on the ascension level.
 *
 * Anyways I swear I am never looking at these 600 lines of insanity ever again.
 */
public class SpellVesperidysDarkHole extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;

	public static final int DARK_HOLE_TICKS_A0 = 25 * 20;
	public static final int DARK_HOLE_TICKS_A4 = 21 * 20;
	public static final int DARK_HOLE_TICKS_A8 = 18 * 20;
	public static final int DARK_HOLE_TICKS_A15 = 15 * 20;
	public static final int DARK_HOLE_PROJECTILES_PER_PLAYER = 5;
	public static final String darkSummonerLos = "VoidMagus";
	public static final String DARK_HOLE_CORE_TAG = "vesperidys_dark_hole_core";

	public int mDarkHoleTicks;

	public static final double VOID_MAGUS_HEALTH = 1000;

	public static double ARMOR_STAND_BLOCK_OFFSET = -1.6875;

	private static final int SHOCK_RADIUS = 3;
	private static final int SHOCK_VERTICAL_RANGE = 10;
	private static final int SHOCK_DELAY_TICKS = (int) (1.75 * Constants.TICKS_PER_SECOND);
	private static final double SHOCK_DAMAGE = 70;

	public SpellVesperidysDarkHole(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 15) {
			mDarkHoleTicks = DARK_HOLE_TICKS_A15;
		} else if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
			mDarkHoleTicks = DARK_HOLE_TICKS_A8;
		} else if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 4) {
			mDarkHoleTicks = DARK_HOLE_TICKS_A4;
		} else {
			mDarkHoleTicks = DARK_HOLE_TICKS_A0;
		}
	}

	@Override
	public void run() {
		summonDarkHole();
	}

	public void summonDarkHole() {
		mBoss.setInvulnerable(true);
		mVesperidys.mInvincible = true;

		Location skyLocation = mVesperidys.mSpawnLoc.clone().add(0, 4, 0);

		// Teleport check.
		mVesperidys.mTeleportSpell.teleport(skyLocation, false);

		mVesperidys.mInvinicibleWarned.clear();

		// Select Summoner Locations
		List<Vesperidys.Platform> bossPlatform = List.of(Objects.requireNonNull(mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss)));
		List<LivingEntity> darkHoleSummoners = new ArrayList<>();
		int numberOfSummoners = (int) Math.ceil(0.5 * PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true).size());

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 20) {
			numberOfSummoners += 1;
		}

		List<Vesperidys.Platform> summonerPlatforms = mVesperidys.mPlatformList.getRandomPlatforms(bossPlatform, numberOfSummoners);

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 0.5f, 1.3f);
			player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1.5f);
			MessagingUtils.sendTitle(player, Component.text("!", NamedTextColor.RED, TextDecoration.BOLD), Component.text("Defeat the Magus!", NamedTextColor.YELLOW), 0, 3*20, 20);
		}


		BukkitRunnable darkHoleRunnable = new BukkitRunnable() {
			int mTicksDarkHoleRunnable = 0;
			final Location mStartLoc = mBoss.getLocation().add(0, 1.5, 0);

			@Override
			public void run() {
				if (mTicksDarkHoleRunnable >= 40) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3f, 1f);

					for (Vesperidys.Platform platform : summonerPlatforms) {
						Location summonLocation = platform.getCenter().add(0, 1, 0);
						LivingEntity e = (LivingEntity) LibraryOfSoulsIntegration.summon(summonLocation, darkSummonerLos);

						if (e != null) {
							EntityUtils.setMaxHealthAndHealth(e, DepthsParty.getAscensionScaledHealth(VOID_MAGUS_HEALTH, mVesperidys.mParty));
							e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 5 * 20, 0));
							mPlugin.mBossManager.manuallyRegisterBoss(e, new VesperidysBlockPlacerBoss(mPlugin, e, mVesperidys));
							e.setGlowing(true);
							darkHoleSummoners.add(e);

							// Summoner Ring Particles
							BukkitRunnable summonerSpawnAnimation = new BukkitRunnable() {
								int mSpawnTicks = 0;

								@Override
								public void run() {
									if (mSpawnTicks > 20) {
										this.cancel();
										return;
									}

									if (mSpawnTicks % 8 == 0) {
										new PPCircle(Particle.REDSTONE, e.getLocation().add(0, (double) mSpawnTicks / 8 + 0.1, 0), 1)
											.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f))
											.ringMode(true)
											.count(20)
											.spawnAsBoss();
									}

									mSpawnTicks++;
								}
							};
							summonerSpawnAnimation.runTaskTimer(mPlugin, 0, 1);
							mActiveRunnables.add(summonerSpawnAnimation);
						}
					}

					this.cancel();
					mBoss.setInvulnerable(false);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 3f, 1f);

					Location mDarkHoleCenter = mVesperidys.mSpawnLoc.clone().add(0, 8, 0);
					ArmorStand core = mBoss.getWorld().spawn(mDarkHoleCenter.clone().add(0, ARMOR_STAND_BLOCK_OFFSET, 0), ArmorStand.class);
					core.setVisible(false);
					core.setGravity(false);
					core.setMarker(true);
					core.setCollidable(false);
					core.getEquipment().setHelmet(new ItemStack(Material.SEA_LANTERN));
					core.addScoreboardTag(DARK_HOLE_CORE_TAG);

					ChargeUpManager darkHoleChargeUp = new ChargeUpManager(mBoss, mDarkHoleTicks, Component.text("Casting", NamedTextColor.GREEN).append(Component.text(" Dark Hole...", NamedTextColor.GRAY, TextDecoration.BOLD)), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 100);

					BukkitRunnable darkHoleChargeRunnable = new BukkitRunnable() {
						int mTicksDarkHoleCharge = 0;

						@Override
						public synchronized void cancel() throws IllegalStateException {
							super.cancel();
							darkHoleChargeUp.reset();
							mVesperidys.mInvincible = false;
							mVesperidys.mDamageCap = 0.03;
							mBoss.setGravity(true);

							// Remove Dark Hole Cores.
							mVesperidys.mSpawnLoc.getNearbyEntitiesByType(ArmorStand.class, 100)
								.stream()
								.filter(e -> ScoreboardUtils.checkTag(e, DARK_HOLE_CORE_TAG))
								.forEach(Entity::remove);
						}

						@Override
						public void run() {
							// Check if summoners still alive, if not remove.
							darkHoleSummoners.removeIf(summoner -> !summoner.isValid() || summoner.isDead());

							// End early if conditions are met.
							if (darkHoleSummoners.isEmpty() || mVesperidys.mDefeated || mBoss.isDead()) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 3f, 0.5f);
								mVesperidys.resetPhase(60);

								this.cancel();
								return;
							}

							// if Boss is not near the sky location, teleport back. (In case of epic teleportation failure)
							if (mBoss.getLocation().distance(skyLocation) > 3) {
								mVesperidys.mTeleportSpell.teleport(skyLocation, false);
							}

							mTicksDarkHoleCharge++;

							// Explosion
							if (darkHoleChargeUp.nextTick(1)) {
								this.cancel();
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 3.0f, 0.5f);

								// Black hole implosion
								BukkitRunnable blackHoleAnimation = new BukkitRunnable() {
									int mBlackHoleTicks = 0;

									@Override
									public void run() {
										// Check if summoners still alive, if not remove.
										darkHoleSummoners.removeIf(summoner -> !summoner.isValid() || summoner.isDead());

										// End early if conditions are met.
										if (darkHoleSummoners.isEmpty() || mVesperidys.mDefeated || mBoss.isDead()) {
											mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 3f, 0.5f);
											mVesperidys.resetPhase(60);

											this.cancel();
											return;
										}

										// It's too late, may lord have mercy on your souls.
										if (mBlackHoleTicks > 60) {
											this.cancel();
											mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2f, 0.5f);
											mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 3f, 1f);
											mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 2f, 1f);

											// Sphere Animation Explosion
											new PPExplosion(Particle.FLAME, mDarkHoleCenter)
												.extra(1)
												.count(75)
												.spawnAsBoss();

											for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
												double percentDamage = 0.5;
												mVesperidys.dealPercentageAndCorruptionDamage(player, percentDamage, "Dark Hole");
												MovementUtils.knockAway(mDarkHoleCenter, player, 3f, 0.2f);

												mPlugin.mEffectManager.addEffect(player, "Vesperidys Antiheal", new PercentHeal(10 * 20, -1.00));
												mPlugin.mEffectManager.addEffect(player, "Vesperidys Antiabsroption", new PercentAbsorption(10 * 20, -1.00));
												player.sendActionBar(Component.text("You cannot heal for 10s", NamedTextColor.RED));
												PotionUtils.applyPotion(mPlugin, player, new PotionEffect(PotionEffectType.BAD_OMEN, 10 * 20, 1));
											}

											for (LivingEntity summoner : darkHoleSummoners) {
												summoner.remove();
											}

											mVesperidys.resetPhase(60);

											return;
										}

										double blackHoleRadius = Math.max(1, 5 - 4 * ((double) mBlackHoleTicks / 60));

										// Pull Player in towards black hole (it would be hillarious)
										if (mBlackHoleTicks % 5 == 0) {
											mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 3f, 0.5f);

											for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
												MovementUtils.pullTowardsNormalized(mDarkHoleCenter, player, 0.5f, false);
											}

											new PartialParticle(Particle.ENCHANTMENT_TABLE, mDarkHoleCenter, (int) (blackHoleRadius * 10), blackHoleRadius, blackHoleRadius, blackHoleRadius).spawnAsBoss();

											for (Player player : PlayerUtils.playersInRange(mDarkHoleCenter, blackHoleRadius, true)) {
												double percentDamage = 0.1;
												mVesperidys.dealPercentageAndCorruptionDamage(player, percentDamage, "Dark Hole");
											}
										}

										for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
											// Draw tether line from Player to Dark Hole.
											Location playerLoc = player.getLocation();
											Vector dir = LocationUtils.getVectorTo(mDarkHoleCenter, playerLoc).normalize().multiply(0.5);
											Location pLoc = playerLoc.clone();
											for (int i = 0; i < 40; i++) {
												pLoc.add(dir);
												new PartialParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0)
													.extra(9999999)
													.spawnForPlayer(ParticleCategory.BOSS, player);

												if (pLoc.distance(mDarkHoleCenter) < 0.5) {
													break;
												}
											}
										}

										// Sphere Animation Pull
										new PartialParticle(Particle.SONIC_BOOM, mDarkHoleCenter)
											.spawnAsBoss();

										for (double i = 0; i <= Math.PI; i += Math.PI/8) {
											for (double j = 0; j < 2*Math.PI; j += 2 * Math.PI / 3) {
												double r = blackHoleRadius;
												double theta = ((mBlackHoleTicks + mDarkHoleTicks) / 40.0) * 2 * Math.PI + j;
												double alpha = i;

												double x = r * FastUtils.sin(alpha) * FastUtils.cos(theta);
												double z = r * FastUtils.sin(alpha) * FastUtils.sin(theta);
												double y = r * FastUtils.cos(alpha);

												Location pLoc = mDarkHoleCenter.clone().add(x, y, z);

												int shade = FastUtils.randomIntInRange(150, 255);
												new PartialParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(shade, shade, shade), 1.0f))
													.extra(0)
													.spawnAsBoss();
											}
										}

										mBlackHoleTicks++;
									}
								};

								blackHoleAnimation.runTaskTimer(mPlugin, 0, 1);
								mActiveRunnables.add(blackHoleAnimation);
								return;
							}

							double blackHoleRadius = 1 + 4 * ((double) mTicksDarkHoleCharge / mDarkHoleTicks);

							// Player Yeet + Corruption Damage + Send an arcing projectile towards players.
							if (mTicksDarkHoleCharge % 50 == 0) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3f, 1.5f);
								new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 125, 0, 0, 0, 0.5, null, true).spawnAsEntityActive(mBoss);

								for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
									// MovementUtils.pullTowardsNormalized(mDarkHoleCenter, player, 0.2f * blackHoleRadius);

									for (int i = 0; i < DARK_HOLE_PROJECTILES_PER_PLAYER; i++) {
										/*
										 * Quadratic Math.
										 * x * (x - distance) from player to get it to intersect at player's position.
										 *
										 * Pick a random rotation point.
										 * Travel projSpeed blocks per mProjTick.
										 */

										Location playerLoc = player.getLocation().add(0, 1, 0);
										double goalDist = Math.max(5, playerLoc.distance(mDarkHoleCenter)) + 5;
										double projSpeed = 0.5;

										BukkitRunnable projRunnable = new BukkitRunnable() {
											int mProjTicks = 0;
											final Location mProjectileCenterLoc = mDarkHoleCenter.clone();
											double mX = 0;
											double mRotation = FastUtils.randomDoubleInRange(0, Math.PI * 2);
											final int mCurvature = FastUtils.randomIntInRange(10, 15); // Curvature. Lower is more curvy

											private double quadratic(double x) {
												// Max of quadratic is at goalDist/2, multiply by goalDist / 10 to get desired curvature
												double halfway = goalDist / 2;
												double maxValue = halfway * (halfway - goalDist);
												return ((x * (x - goalDist)) / maxValue) * (goalDist / mCurvature);
											}

											private void update() {
												double vDistance = quadratic(mX);
												Vector dir = LocationUtils.getDirectionTo(playerLoc, mDarkHoleCenter);

												ParticleUtils.drawCurve(mProjectileCenterLoc, 1, 1, dir.clone().normalize(),
													t -> 0,
													t -> FastUtils.sin(Math.PI * 2 * t / 2 + mRotation) * vDistance,
													t -> FastUtils.cos(Math.PI * 2 * t / 2 + mRotation) * vDistance,
													(l, t) -> {
														new PartialParticle(Particle.SOUL_FIRE_FLAME, l, 1, 0, 0, 0, 0)
															.extra(0)
															.spawnAsBoss();
														BoundingBox box = BoundingBox.of(l, 0.25, 0.25, 0.25);

														for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
															if (box.overlaps(player.getBoundingBox())) {
																BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, 45, "Unstable Void", mDarkHoleCenter);
																new PPExplosion(Particle.SMOKE_NORMAL, player.getLocation())
																	.count(10)
																	.spawnAsBoss();
																mBoss.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
																this.cancel();
															}
														}

														if (mProjTicks % 5 == 0) {
															mBoss.getWorld().playSound(l, Sound.BLOCK_FIRE_EXTINGUISH, 1, 2);
														}

														if (l.getBlock().isSolid()) {
															mBoss.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
															this.cancel();
														}
													});

												mProjTicks++;
												mRotation -= 0.1;
												mX += projSpeed;
												mProjectileCenterLoc.add(dir.normalize().multiply(projSpeed));
											}

											@Override
											public void run() {
												if (mProjTicks >= 60) {
													this.cancel();
													return;
												}

												// Update twice for better hit detection and speed.
												update();
												update();
											}
										};
										projRunnable.runTaskTimer(mPlugin, 0, 1);
										mActiveRunnables.add(projRunnable);
									}
								}
							}

							// Boss Shield Particles, Enchantment Table Particles + Damage players in blackhole range + Random Projectile
							if (mTicksDarkHoleCharge % 20 == 0) {
								Location loc = mBoss.getLocation();

								new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.75, 0), 2)
									.count(15)
									.data(new Particle.DustOptions(Color.fromRGB(51, 204, 255), 1.0f))
									.ringMode(true)
									.extra(10000000)
									.spawnAsBoss();
								new PPCircle(Particle.REDSTONE, loc.clone().add(0, 1.25, 0), 2)
									.count(15)
									.data(new Particle.DustOptions(Color.fromRGB(51, 204, 255), 1.0f))
									.ringMode(true)
									.extra(10000000)
									.spawnAsBoss();
								new PPCircle(Particle.REDSTONE, loc.clone().add(0, 1.75, 0), 2)
									.count(15)
									.data(new Particle.DustOptions(Color.fromRGB(51, 204, 255), 1.0f))
									.ringMode(true)
									.extra(10000000)
									.spawnAsBoss();

								new PartialParticle(Particle.ENCHANTMENT_TABLE, mDarkHoleCenter, (int) (blackHoleRadius * 10), blackHoleRadius, blackHoleRadius, blackHoleRadius).spawnAsBoss();
								for (Player player : PlayerUtils.playersInRange(mDarkHoleCenter, blackHoleRadius, true)) {
									double percentDamage = 0.1;
									mVesperidys.dealPercentageAndCorruptionDamage(player, percentDamage, "Dark Hole");
								}

								// Send projectile in random direction. Attempt twice per tick.
								for (int i = 0; i < 6; i++) {
									if (FastUtils.randomDoubleInRange(0, 5) < blackHoleRadius) {
										double r = blackHoleRadius;
										double alpha = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
										double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);

										double x = r * FastUtils.sin(theta) * FastUtils.cos(alpha);
										double y = - Math.abs(r * FastUtils.sin(theta) * FastUtils.sin(alpha));
										double z = r * FastUtils.cos(theta);

										double projSpeed = 0.5;

										mBoss.getWorld().playSound(mDarkHoleCenter, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 2f, 0.5f);

										BukkitRunnable projRunnable = new BukkitRunnable() {
											Location mProjLoc = mDarkHoleCenter.clone();
											int mProjTicks = 0;

											@Override
											public void run() {
												if (mProjTicks > 70) {
													this.cancel();
													return;
												}

												mProjTicks += 1;

												Vector dir = new Vector(x, y, z).normalize();
												mProjLoc.add(dir.multiply(projSpeed));

												new PartialParticle(Particle.END_ROD, mProjLoc, 1, 0, 0, 0, 0)
													.extra(10000000)
													.spawnAsBoss();
												new PartialParticle(Particle.REDSTONE, mProjLoc, 1)
													.data(new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1f))
													.spawnAsBoss();

												BoundingBox box = BoundingBox.of(mProjLoc, 0.25, 0.25, 0.25);

												for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
													if (box.overlaps(player.getBoundingBox())) {
														BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, 45, "Unstable Void", mDarkHoleCenter);
														new PartialParticle(Particle.CRIT_MAGIC, player.getLocation(), 25, 0.5, 1, 0.5, 0).spawnAsBoss();
														mBoss.getWorld().playSound(mProjLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
														this.cancel();
													}
												}

												if (mProjLoc.getBlock().isSolid()) {
													mBoss.getWorld().playSound(mProjLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
													this.cancel();
												}
											}
										};
										projRunnable.runTaskTimer(mPlugin, 0, 1);
										mActiveRunnables.add(projRunnable);
									}
								}
							}


							// If Ascended, rain meteors down!
							if ((mTicksDarkHoleCharge % 80 == 0 && mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 4)
								|| (mTicksDarkHoleCharge % 40 == 0 && mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8)
								|| (mTicksDarkHoleCharge % 20 == 0 && mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 15)) {
								Location center = mVesperidys.mSpawnLoc.clone();

								for (Player player : PlayerUtils.playersInRange(center, Vesperidys.detectionRange, true)) {
									startStrike(player.getLocation());
								}
							}

							// Sphere Animation Expanding
							double yOffset = 0.25 * Math.sin((double) mTicksDarkHoleCharge / 20.0);
							core.teleport(mDarkHoleCenter.clone().add(0, yOffset + ARMOR_STAND_BLOCK_OFFSET, 0));
							core.setRotation((float) (mTicksDarkHoleCharge * 5.0), 0);
							for (double i = 0; i <= Math.PI; i += Math.PI/8) {
								for (double j = 0; j < 2*Math.PI; j += 2 * Math.PI/3) {
									double r = blackHoleRadius;
									double theta = (mTicksDarkHoleCharge / 40.0) * 2 * Math.PI + j;
									double alpha = i;

									double x = r * FastUtils.sin(alpha) * FastUtils.cos(theta);
									double z = r * FastUtils.sin(alpha) * FastUtils.sin(theta);
									double y = r * FastUtils.cos(alpha);

									Location pLoc = mDarkHoleCenter.clone().add(x, y, z);

									int shade = FastUtils.randomIntInRange(150, 255);
									new PartialParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(shade, shade, shade), 1.0f))
										.extra(0)
										.spawnAsBoss();
								}
							}

							// Summoner Animations
							for (LivingEntity summoner: darkHoleSummoners) {
								double rotation = mTicksDarkHoleCharge / 20.0 * Math.PI;
								double x = 0.5 * FastUtils.sin(rotation);
								double z = 0.5 * FastUtils.cos(rotation);
								double y = 1 + 0.5 * FastUtils.sin(rotation / 3);

								Location mPLoc = summoner.getLocation().add(x, y, z);

								new PartialParticle(Particle.SPELL_WITCH, mPLoc, 1, 0, 0, 0)
									.spawnAsBoss();
							}

							if (mTicksDarkHoleCharge % 10 == 0) {
								for (LivingEntity summoner: darkHoleSummoners) {
									// Summoner Projectile Animation
									BukkitRunnable animation = new BukkitRunnable() {
										Location mParticleLocCenter = summoner.getLocation().add(0, 1.5, 0);
										double mRotation = mTicksDarkHoleCharge / 20.0 * Math.PI;
										double mRadius = 0.2 + 0.2 * FastUtils.sin(mRotation);
										boolean mReversed = false;

										@Override
										public void run() {
											if (mParticleLocCenter.distance(mDarkHoleCenter) <= blackHoleRadius || !mVesperidys.mInvincible) {
												this.cancel();
												return;
											}

											if (mReversed) {
												mRadius -= 0.1;
											} else {
												mRadius += 0.1;
											}

											if (mRadius >= 0.5) {
												mReversed = true;
											} else if (mRadius <= 0) {
												mReversed = false;
											}

											mRotation += 0.2;
											int shade = Math.min(255, Math.max(0, (int) (mRadius * 2 * 255)));

											double distance = 0.5;

											Vector dir = LocationUtils.getDirectionTo(mDarkHoleCenter, mParticleLocCenter);
											mParticleLocCenter.add(dir.normalize().multiply(distance));
											new PartialParticle(Particle.CRIT_MAGIC, mParticleLocCenter, 1, 0, 0, 0)
												.spawnAsBoss();

											ParticleUtils.drawCurve(mParticleLocCenter, 1, 3, dir.clone().normalize(),
												t -> 0,
												t -> FastUtils.sin(Math.PI * 2 * t / 2 + mRotation) * mRadius,
												t -> FastUtils.cos(Math.PI * 2 * t / 2 + mRotation) * mRadius,
												(l, t) -> {
													new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(shade, shade, shade), 1.0f)).spawnAsBoss();
												});
										}
									};
									animation.runTaskTimer(mPlugin, 0, 2);
									mActiveRunnables.add(animation);
								}
							}

							// Playsound
							if (mTicksDarkHoleCharge % 8 == 0) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (((float) mTicksDarkHoleCharge) / mDarkHoleTicks));
							} else if (mTicksDarkHoleCharge % 8 == 2) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (((float) mTicksDarkHoleCharge) / mDarkHoleTicks));
							} else if (mTicksDarkHoleCharge % 8 == 4) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (((float) mTicksDarkHoleCharge) / mDarkHoleTicks));
							} else if (mTicksDarkHoleCharge % 8 == 6) {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (((float) mTicksDarkHoleCharge) / mDarkHoleTicks));
							}
						}
					};

					darkHoleChargeRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(darkHoleChargeRunnable);

				} else {
					mTicksDarkHoleRunnable++;

					// Particles that slowly reach summoners.
					for (Vesperidys.Platform platform : summonerPlatforms) {
						Location endLoc = platform.getCenter().add(0, 2.5, 0);
						Vector dir = LocationUtils.getDirectionTo(endLoc, mStartLoc).normalize();
						double distance = mStartLoc.distance(endLoc);
						double particleDistance = Math.min(distance, ((double) mTicksDarkHoleRunnable / 35) * distance);

						Location particleLoc = mStartLoc.clone().add(dir.multiply(particleDistance));

						new PartialParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1)
							.extra(10000000)
							.spawnAsBoss();
						new PartialParticle(Particle.REDSTONE, particleLoc, 1)
							.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 0.75f))
							.spawnAsBoss();

						new PPCircle(Particle.SOUL_FIRE_FLAME, particleLoc.clone().add(0, -1.7, 0), Math.max(0.5, Math.min(2, -(1.0/100.0) * mTicksDarkHoleRunnable * (mTicksDarkHoleRunnable - 40))))
							.extra(10000000)
							.count(10)
							.spawnAsBoss();
					}

					// Shield Up and Down Particles
					new PPCircle(Particle.CRIT_MAGIC, mBoss.getLocation().add(0, 2.5 - Math.abs(2.5 * (mTicksDarkHoleRunnable - 20) / 20.0), 0), 2)
						.ringMode(true)
						.count(15)
						.spawnAsBoss();
					new PPCircle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 2.5 - Math.abs(2.5 * (mTicksDarkHoleRunnable - 20) / 20.0), 0), 2)
						.ringMode(true)
						.count(8)
						.spawnAsBoss();
				}
			}
		};
		darkHoleRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(darkHoleRunnable);

	}

	public void startStrike(Location strikeLocation) {
		World world = mBoss.getWorld();
		strikeLocation.setY(mVesperidys.mSpawnLoc.getY());

		BukkitRunnable runnable = new BukkitRunnable() {

			int mT = 0;

			@Override
			public void run() {
				if (mT > SHOCK_DELAY_TICKS) {
					Collection<Player> shockPlayers = PlayerUtils.playersInCylinder(
						strikeLocation,
						SHOCK_RADIUS,
						2 * SHOCK_VERTICAL_RANGE
					);
					shockPlayers.forEach((Player player) -> strikeShock(strikeLocation, player));

					world.playSound(strikeLocation, Sound.ITEM_TRIDENT_THUNDER, 1f, 0.75f);
					world.playSound(strikeLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.75f);

					BukkitRunnable runnableEnd = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							if (mT > 6 || (mT > 0 && mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8)) {
								this.cancel();
								return;
							}

							for (int i = 0; i <= 8; i++) {
								double rad = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
								Location l = strikeLocation.clone().add(0.2 * Math.cos(rad), 0.2 + FastUtils.randomDoubleInRange(0, 0.2), 0.2 * Math.sin(rad));
								Location center = strikeLocation.clone().add(0, 0.2, 0);
								Vector vector = l.toVector().subtract(center.toVector()).normalize();
								new PartialParticle(Particle.END_ROD, l).delta(vector.getX(), vector.getY(), vector.getZ())
									.count(1)
									.extra(FastUtils.randomDoubleInRange(0, 0.5))
									.directionalMode(true)
									.spawnAsBoss();
							}
							mT += 3;
						}
					};
					runnableEnd.runTaskTimer(mPlugin, 0, 3);
					mActiveRunnables.add(runnableEnd);

					int particleAmount = 20;
					if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
						particleAmount = 10;
					}
					ParticleUtils.drawRing(strikeLocation.clone().add(0, 0.1, 0), particleAmount, new Vector(0, 1, 0), 0.2,
						(l, t) -> {
							Location center = strikeLocation.clone().add(0, 0.1, 0);
							Vector vector = l.toVector().subtract(center.toVector()).normalize();
							new PartialParticle(Particle.END_ROD, l).delta(vector.getX(), vector.getY(), vector.getZ())
								.count(1)
								.extra(0.25)
								.directionalMode(true)
								.spawnAsBoss();
						}
					);

					int finalParticleAmount = particleAmount;
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						ParticleUtils.drawRing(strikeLocation.clone().add(0, 0.1, 0), finalParticleAmount / 2, new Vector(0, 1, 0), SHOCK_RADIUS,
							(l, t) -> {
								Location center = strikeLocation.clone().add(0, 0.1, 0);
								Vector vector = l.toVector().subtract(center.toVector()).normalize();
								new PartialParticle(Particle.END_ROD, l).delta(vector.getX(), vector.getY(), vector.getZ())
									.count(1)
									.extra(0.25)
									.directionalMode(true)
									.spawnAsBoss();
							}
						);
					}, 10);

					this.cancel();
					return;
				}

				double progress = (double) mT / SHOCK_DELAY_TICKS;

				if (mT % 10 == 0) {
					new PPPillar(Particle.REDSTONE, strikeLocation, SHOCK_VERTICAL_RANGE)
						.count(2 * SHOCK_VERTICAL_RANGE)
						.data(new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1))
						.spawnAsBoss();
				}

				if (mT % 5 == 0) {
					world.playSound(strikeLocation, Sound.ITEM_FIRECHARGE_USE, 1.0f, (float) (1f + 1f * (1 - progress)));
				}

				if (mT % 4 == 0) {

					ParticleUtils.drawRing(strikeLocation.clone().add(0, 0.1, 0), 25, new Vector(0, 1, 0), SHOCK_RADIUS * (1 - progress),
						(l, t) -> {
							Color color = Color.fromRGB(240 + FastUtils.randomIntInRange(-5, 5), (int) (240 * (1 - progress) + FastUtils.randomIntInRange(0, 5)), FastUtils.randomIntInRange(0, 5));

							new PartialParticle(Particle.REDSTONE, l).delta(0, 0, 0)
								.count(1)
								.extra(0.15)
								.data(new Particle.DustOptions(color, 1f))
								.spawnAsBoss();
						}
					);
				}

				if (mT % 4 == 0) {
					new PPCircle(Particle.REDSTONE, strikeLocation.clone().add(0, 0.1, 0), SHOCK_RADIUS)
						.count(25)
						.extra(0)
						.data(new Particle.DustOptions(Color.fromRGB(240 + FastUtils.randomIntInRange(-5, 5), 0, 240 + FastUtils.randomIntInRange(-5, 5)), 1))
						.spawnAsBoss();
				}

				mT++;
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}


	public void strikeShock(
		Location strikeLocation,
		Player player
	) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, SHOCK_DAMAGE, null, false, false, "Feint Particle Beam");
		MovementUtils.knockAway(strikeLocation, player, 1f, 0.5f);

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
			mPlugin.mEffectManager.addEffect(player, "VesperidysMagicVuln", new PercentDamageReceived(15 * 20, 0.3, EnumSet.of(DamageEvent.DamageType.MAGIC)));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
