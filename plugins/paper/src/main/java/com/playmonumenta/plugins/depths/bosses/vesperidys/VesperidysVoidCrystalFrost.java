package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.spells.vesperidys.SpellVoidCrystalTeleportPassive;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class VesperidysVoidCrystalFrost extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysvoidcrystalfrost";

	private final Vesperidys mVesperidys;
	private final Plugin mMonuPlugin;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 10 * 20;
	private static final int DELAY = 30;
	private static final double SPEED = 0.6;
	private static final double TURN_RADIUS = 0.02;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int) (DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 0.25;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 40;

	private final List<Block> mAvalancheBlocks = new ArrayList<>();

	SpellBaseSeekingProjectile mMissile;

	public static @Nullable VesperidysVoidCrystalFrost deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysVoidCrystalFrost construct(Plugin plugin, LivingEntity boss) {
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
			MMLog.warning("VesperidysVoidCrystalFrost: Vesperidys wasn't found! (This is a bug)");
			return null;
		}
		return new VesperidysVoidCrystalFrost(plugin, boss, vesperidys);
	}

	public VesperidysVoidCrystalFrost(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		super(plugin, identityTag, boss);
		mMonuPlugin = plugin;
		mVesperidys = vesperidys;

		mMissile = new SpellBaseSeekingProjectile(plugin, boss, Vesperidys.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
			SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				if (ticks == 0) {
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 2.0f, 0.7f);
					world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 2.0f, 2.0f);
				}

				if (ticks % 4 == 0) {
					new PartialParticle(Particle.SNOWFLAKE, loc, 10, 0.25, 0.25, 0.25, 0.15).spawnAsEntityActive(mBoss);
				}
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0.5f);
				world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 3, 0.5f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.SNOWFLAKE, loc, 4, 0.1, 0.1, 0.1, 0.05).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.BLOCK_DUST, loc, 1, 0.1, 0.1, 0.1, 0.05, Material.ICE.createBlockData()).spawnAsEntityActive(mBoss);
			},
			// Hit Action
			(World world, @Nullable LivingEntity le, Location loc, @Nullable Location prevLoc) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 1, 2);
				loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 2f);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.5, 0.5, 0.5, 0.5).spawnAsEntityActive(mBoss);
				if (le instanceof Player player) {
					BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MAGIC, DAMAGE, "Ice Missiles", prevLoc);
					mMonuPlugin.mEffectManager.addEffect(player, "VoidCrystalFrostSlowness", new PercentSpeed(20 * 2, -0.5, "VoidCrystalFrostSlowness"));
					Block pLoc = player.getLocation().add(0, -1, 0).getBlock();
					iceExposedBlock(pLoc);
					iceExposedBlock(pLoc.getRelative(-1, 0, -1));
					iceExposedBlock(pLoc.getRelative(-1, 0, 0));
					iceExposedBlock(pLoc.getRelative(-1, 0, 1));
					iceExposedBlock(pLoc.getRelative(0, 0, -1));
					iceExposedBlock(pLoc.getRelative(0, 0, 1));
					iceExposedBlock(pLoc.getRelative(1, 0, -1));
					iceExposedBlock(pLoc.getRelative(1, 0, 0));
					iceExposedBlock(pLoc.getRelative(1, 0, 1));
					iceExposedBlock(pLoc.getRelative(-2, 0, 0));
					iceExposedBlock(pLoc.getRelative(2, 0, 0));
					iceExposedBlock(pLoc.getRelative(0, 0, -2));
					iceExposedBlock(pLoc.getRelative(0, 0, 2));
				} else {
					Block pLoc = loc.clone().add(0, -1, 0).getBlock();
					iceExposedBlock(pLoc);
					iceExposedBlock(pLoc.getRelative(-1, 0, -1));
					iceExposedBlock(pLoc.getRelative(-1, 0, 0));
					iceExposedBlock(pLoc.getRelative(-1, 0, 1));
					iceExposedBlock(pLoc.getRelative(0, 0, -1));
					iceExposedBlock(pLoc.getRelative(0, 0, 1));
					iceExposedBlock(pLoc.getRelative(1, 0, -1));
					iceExposedBlock(pLoc.getRelative(1, 0, 0));
					iceExposedBlock(pLoc.getRelative(1, 0, 1));
					iceExposedBlock(pLoc.getRelative(-2, 0, 0));
					iceExposedBlock(pLoc.getRelative(2, 0, 0));
					iceExposedBlock(pLoc.getRelative(0, 0, -2));
					iceExposedBlock(pLoc.getRelative(0, 0, 2));
				}
			});

		Spell spell = new Spell() {

			@Override
			public void run() {
				open();
				BukkitRunnable missileLaunch = new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						if (mTicks > DELAY) {
							List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true);
							Collections.shuffle(players);

							if (players.size() > 0) {
								Player selectedPlayer = players.get(0);

								Runnable runnable = () -> mMissile.launch(selectedPlayer, selectedPlayer.getEyeLocation());
								Bukkit.getScheduler().runTaskLater(mPlugin, runnable, 0);
								Bukkit.getScheduler().runTaskLater(mPlugin, runnable, 20);
								Bukkit.getScheduler().runTaskLater(mPlugin, runnable, 40);
							}

							Bukkit.getScheduler().runTaskLater(mPlugin, VesperidysVoidCrystalFrost.this::close, 70);
							this.cancel();
							return;
						}
						mMissile.runInitiateAesthetic(mBoss.getWorld(), mBoss.getEyeLocation(), mTicks);

						mTicks++;
					}
				};
				missileLaunch.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(missileLaunch);
			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};
		SpellManager activeSpells = new SpellManager(List.of(spell));

		List<Spell> passiveSpells = List.of(
			new SpellVoidCrystalTeleportPassive(mVesperidys, boss)
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(activeSpells, passiveSpells, Vesperidys.detectionRange, null, DELAY);
	}

	private void iceExposedBlock(Block b) {
		//Check above block first and see if it is exposed to air
		if (b.getRelative(BlockFace.UP).isSolid() && !(b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).isSolid() || b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.WATER)) {
			spawnIceTerrain(b.getRelative(BlockFace.UP));
		} else if (b.isSolid() || b.getType() == Material.WATER) {
			spawnIceTerrain(b);
		} else if (b.getRelative(BlockFace.DOWN).isSolid() || b.getRelative(BlockFace.DOWN).getType() == Material.WATER) {
			spawnIceTerrain(b.getRelative(BlockFace.DOWN));
		}
	}

	private void spawnIceTerrain(Block b) {
		Material iceMaterial = Material.BLUE_ICE;

		if (TemporaryBlockChangeManager.INSTANCE.changeBlock(b, iceMaterial, 120 * 20)) {
			mAvalancheBlocks.add(b);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Vesperidys.Platform targetPlatform = mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss);

		if (targetPlatform != null) {
			Location centre = targetPlatform.getCenter().clone().add(0, 1, 0);
			int telegraphTicks = 60;

			// Avalanche
			BukkitRunnable deathRunnable = new BukkitRunnable() {
				int mDeathTicks = 0;

				@Override
				public void run() {
					if (mVesperidys.mDefeated) {
						this.cancel();
					}

					if (mDeathTicks > telegraphTicks) {
						// Check if players standing on Blue Ice.
						List<Player> hitPlayers = new ArrayList<>();

						for (Player player : PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true, false)) {
							Block standingBlock = player.getLocation().add(0, -1, 0).getBlock();

							if (mAvalancheBlocks.contains(standingBlock) && standingBlock.getType() == Material.BLUE_ICE) {
								mMonuPlugin.mEffectManager.addEffect(player, "VoidCrystalFrostSlowness", new PercentSpeed(20 * 2, -0.5, "VoidCrystalFrostSlowness"));
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE * 3, null, true, true, "Avalanche");
								MovementUtils.knockAway(standingBlock.getLocation(), player, 0.5f, 0.75f, false);
								hitPlayers.add(player);
							}
						}

						for (Player player : targetPlatform.getPlayersOnPlatform()) {
							if (!hitPlayers.contains(player)) {
								mMonuPlugin.mEffectManager.addEffect(player, "VoidCrystalFrostSlowness", new PercentSpeed(20 * 2, -0.5, "VoidCrystalFrostSlowness"));
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE * 3, null, true, true, "Avalanche");
								MovementUtils.knockAway(targetPlatform.getCenter(), player, 0.5f, 0.75f, false);
								hitPlayers.add(player);
							}
						}

						centre.getWorld().playSound(centre, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0.5f);
						centre.getWorld().playSound(centre, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.5f);
						targetPlatform.destroy();

						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							if (mVesperidys.mPhase < 4 || (mVesperidys.mPhase >= 4 && Math.abs(targetPlatform.mX) <= 1 && Math.abs(targetPlatform.mY) <= 1)) {
								if (mVesperidys.mFullPlatforms) {
									targetPlatform.generateFull();
								} else {
									targetPlatform.generateInner();
								}
							}
						}, 20*20);

						for (Block block : mAvalancheBlocks) {
							if (TemporaryBlockChangeManager.INSTANCE.revertChangedBlock(block, Material.BLUE_ICE)) {
								Location bLoc = block.getLocation().add(0.5, 1, 0.5);
								new PPExplosion(Particle.SNOWFLAKE, bLoc)
									.count(3)
									.extra(1)
									.spawnAsBoss();
							}
						}

						this.cancel();
						return;
					}

					if (mDeathTicks % 5 == 0) {
						centre.getWorld().playSound(centre, Sound.BLOCK_SNOW_BREAK, SoundCategory.HOSTILE, 3, 1f);

						int radius = mDeathTicks / 5;

						boolean newBlock = false;
						for (int x = -radius; x < radius; x++) {
								for (int z = -radius; z < radius; z++) {
									Location bLoc = centre.clone().add(x, -1, z);
									Block block = bLoc.getBlock();

									if (targetPlatform.mBlocks.contains(block)) {
										for (int y = -radius; y < radius; y++) {
											Block blockRelative = block.getRelative(0, y, 0);
											if (blockRelative.getType() != Material.BLUE_ICE && blockRelative.isSolid()) {
												spawnIceTerrain(blockRelative);
												newBlock = true;
											}
										}
									}
								}
							}

						if (newBlock) {
							centre.getWorld().playSound(centre, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 1f);
						}

						// particles.
						for (Block block : mAvalancheBlocks) {
							if (block.getType() == Material.BLUE_ICE) {
								Location bLoc = block.getLocation().clone().add(0.5, 1, 0.5);
								new PartialParticle(Particle.SNOWFLAKE, bLoc, 2, 0.2, 0.2, 0.2)
									.spawnAsBoss();
							}
						}
					}

					mDeathTicks++;
				}
			};
			deathRunnable.runTaskTimer(mPlugin, 0, 1);
		}
	}


	public void open() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(1.0f);
		}
	}

	public void close() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(0.0f);
		}
	}
}
