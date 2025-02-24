package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellShadowCrystalVoidGrenades;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVoidCrystalTeleportPassive;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class VesperidysVoidCrystalShadow extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystalshadow";

	private final Vesperidys mVesperidys;
	private final Plugin mMonuPlugin;

	private static final int DELAY = 2 * 20;
	private static final int DUMMY_HEALTH = 1000;
	private static final int DAMAGE = 80;

	public static @Nullable VesperidysVoidCrystalShadow deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalShadow construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Vesperidys.
		Vesperidys vesperidys = null;
		List<LivingEntity> witherSkeletons = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.WITHER_SKELETON));
		for (LivingEntity mob : witherSkeletons) {
			if (mob.getScoreboardTags().contains(Vesperidys.identityTag)) {
				vesperidys = BossUtils.getBossOfClass(mob, Vesperidys.class);
				break;
			}
		}
		if (vesperidys == null) {
			MMLog.warning("VesperidysBlockPlacerBoss: Vesperidys wasn't found! (This is a bug)");
			return null;
		}
		return new VesperidysVoidCrystalShadow(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalShadow(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mMonuPlugin = plugin;
		mVesperidys = vesperidys;

		SpellShadowCrystalVoidGrenades spell = new SpellShadowCrystalVoidGrenades(mMonuPlugin, boss, 50, 20 * 20);

		SpellManager activeSpells = new SpellManager(List.of(spell));

		List<Spell> passiveSpells = List.of(
			new SpellVoidCrystalTeleportPassive(mVesperidys, boss)
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(activeSpells, passiveSpells, Vesperidys.detectionRange, null, DELAY);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		World world = mBoss.getWorld();
		List<Vesperidys.Platform> bossPlatform = List.of(Objects.requireNonNull(mVesperidys.mPlatformList.getPlatformNearestToEntity(mVesperidys.mBossTwo)));
		List<Vesperidys.Platform> summonerPlatforms = mVesperidys.mPlatformList.getRandomPlatforms(bossPlatform, 1);

		if (!summonerPlatforms.isEmpty()) {
			world.playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 3, 1.5f);
			int telegraphTicks = 40;

			for (Vesperidys.Platform platform : summonerPlatforms) {
				world.playSound(platform.getCenter(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3, 1.5f);

				BukkitRunnable platformRunnable = new BukkitRunnable() {
					int mDeathTicks = 0;

					@Override
					public void run() {
						if (mVesperidys.mDefeated || mVesperidys.mInvincible || platform.mBroken) {
							this.cancel();
						}

						if (mDeathTicks > telegraphTicks) {
							summonDummy(platform);
							this.cancel();
							return;
						}

						// Helix Spawn Animation
						if (mDeathTicks % 2 == 0) {
							double height = 5 - 5 * ((double) mDeathTicks / telegraphTicks);
							double radius = 0.3;
							Location centreLoc = platform.getCenter().add(0, height, 0);

							double angle = (double) mDeathTicks / telegraphTicks * 4 * Math.PI;

							for (int i = 0; i < 2; i++) {
								double angle1 = angle + i * Math.PI;

								double x = radius * Math.cos(angle1);
								double z = radius * Math.sin(angle1);
								Location pLoc = centreLoc.clone().add(x, 0, z);
								new PartialParticle(Particle.REDSTONE, pLoc, 1)
									.data(new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.0f))
									.count(1)
									.spawnAsBoss();
							}
						}

						mDeathTicks++;
					}
				};
				platformRunnable.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	private void summonDummy(Vesperidys.Platform platform) {
		Entity e = LibraryOfSoulsIntegration.summon(platform.getCenter().add(0, 1, 0), "TheVesperidys");
		if (e instanceof LivingEntity dummy) {
			EntityUtils.setMaxHealthAndHealth(dummy, DepthsParty.getAscensionScaledHealth(DUMMY_HEALTH, mVesperidys.mParty));
			dummy.getWorld().playSound(dummy.getLocation(), Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 3, 1f);

			BukkitRunnable dummyRunnable = new BukkitRunnable() {
				final int DURATION = 20 * 20;
				int mDummyTicks = 0;
				int mViolationComboTicks = 0;

				@Override
				public synchronized void cancel() {
					super.cancel();
					new PartialParticle(Particle.SPELL_WITCH, LocationUtils.getEntityCenter(dummy), 20, 0.5, 1, 0.5)
						.spawnAsEntityActive(dummy);
					dummy.remove();
				}

				@Override
				public void run() {
					if (mVesperidys.mDefeated || mVesperidys.mInvincible || mDummyTicks > DURATION || platform.mBroken || dummy.isDead() || !dummy.isValid()) {
						this.cancel();
						return;
					}

					for (LivingEntity le : EntityUtils.getNearbyMobs(dummy.getLocation(), 3, dummy)) {
						if (ScoreboardUtils.checkTag(le, "Boss") && !mVesperidys.mTeleportSpell.mTeleporting) {
							this.cancel();
							return;
						}
					}

					if (mDummyTicks > 2 * 20) {
						if (!platform.getPlayersOnPlatform().isEmpty()) {
							mViolationComboTicks++;
							if (mViolationComboTicks > 20) {
								for (Player player : platform.getPlayersOnPlatform()) {
									DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, null, true, true, "Decoy Dummy");
									PotionUtils.applyPotion(mMonuPlugin, player, new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 1));
									mMonuPlugin.mEffectManager.addEffect(player, "DummySlowness", new PercentSpeed(20 * 2, -1, "DummySlowness"));
									player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 2, -4));
								}

								for (Block block : platform.mBlocks) {
									Location loc = block.getLocation().add(0.5, 1.2, 0.5);
									new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
									if (FastUtils.randomIntInRange(0, 2) == 0) {
										// 1/3 chance for particle
										if (FastUtils.randomIntInRange(0, 2) == 0) {
											new PartialParticle(Particle.FLAME, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
										} else {
											new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
										}
									}

									if (FastUtils.randomIntInRange(0, 10) == 0) {
										// 1/10 chance for particle
										new PartialParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
									}
								}

								dummy.getWorld().playSound(dummy.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 3, 2f);
								dummy.getWorld().playSound(dummy.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 3, 2f);

								BukkitRunnable platformBreakRunnable = new BukkitRunnable() {
									int mPlatformTicks = 0;

									@Override
									public void run() {
										if (mVesperidys.mDefeated) {
											this.cancel();
											return;
										}

										if (mPlatformTicks > 20) {
											platform.destroy();

											Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
												if (mVesperidys.mPhase < 4 || (mVesperidys.mPhase >= 4 && Math.abs(platform.mX) <= 1 && Math.abs(platform.mY) <= 1)) {
													if (mVesperidys.mFullPlatforms) {
														platform.generateFull();
													} else {
														platform.generateInner();
													}
												}
											}, 20 * 20);

											this.cancel();
											return;
										}

										if (mPlatformTicks % 3 == 0) {
											Location centre = platform.getCenter();
											centre.getWorld().playSound(centre, Sound.BLOCK_NETHER_BRICKS_HIT, SoundCategory.HOSTILE, 3, 1f);

											int radius = mPlatformTicks / 3;

											for (int x = -radius; x < radius; x++) {
												for (int z = -radius; z < radius; z++) {
													Location bLoc = centre.clone().add(x, -1, z);
													Block block = bLoc.getBlock();

													if (platform.mBlocks.contains(block)) {
														for (int y = -4; y < 4; y++) {
															Block blockRelative = block.getRelative(0, y, 0);
															if (blockRelative.getType() != Material.COBWEB && blockRelative.isSolid()) {
																TemporaryBlockChangeManager.INSTANCE.changeBlock(blockRelative, Material.COBWEB, 120 * 20);
															}
														}
													}
												}
											}
										}
										mPlatformTicks++;
									}
								};
								platformBreakRunnable.runTaskTimer(mPlugin, 0, 1);

								this.cancel();
								return;
							}
						}
					}

					if (mDummyTicks % 50 == 0) {
						dummy.getWorld().playSound(dummy.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 3, 1f);
					}

					if (mDummyTicks % 10 == 0) {
						new PartialParticle(Particle.SPELL_WITCH, LocationUtils.getEntityCenter(dummy), 10, 0.5, 1, 0.5)
							.spawnAsEntityActive(dummy);
					}

					mDummyTicks++;
				}
			};
			dummyRunnable.runTaskTimer(mPlugin, 0, 1);
		}
	}
}
