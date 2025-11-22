package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPRectPrism;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A warning to future readers: This spell is prone to breaking because it handles every Seismic Ruin direction in one
 * method instead of the original implementation which used four methods and it has to handle casting twice at the same
 * time for Eldrask's final phase. Exercise caution lest you run into race conditions and weird off by one errors.
 *
 * @author Spy
 */
public final class UltimateSeismicRuin extends Spell {
	private static final String SPELL_NAME = "Seismic Ruin";
	private static final int LAYERS_TO_DESTROY = 14; /* 15 layers but count from 0 */
	private static final int TICKS_PER_LAYER = 4;
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 5;
	private static final int CAST_DURATION = (LAYERS_TO_DESTROY + 4) * TICKS_PER_LAYER; /* 3 layers are partially destroyed */

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final List<Location> mCardinals;
	private final Location mOrigin;

	private enum Cardinal {
		NORTH("North", -(FrostGiant.ARENA_LENGTH / 2), FrostGiant.ARENA_LENGTH / 2, 0, LAYERS_TO_DESTROY),
		SOUTH("South", -(FrostGiant.ARENA_LENGTH / 2), FrostGiant.ARENA_LENGTH / 2, -LAYERS_TO_DESTROY, 0),
		EAST("East", -LAYERS_TO_DESTROY, 0, -(FrostGiant.ARENA_LENGTH / 2), FrostGiant.ARENA_LENGTH / 2),
		WEST("West", 0, LAYERS_TO_DESTROY, -(FrostGiant.ARENA_LENGTH / 2), FrostGiant.ARENA_LENGTH / 2);

		public final String mName;
		public final int mMinX;
		public final int mMaxX;
		public final int mMinZ;
		public final int mMaxZ;

		Cardinal(final String name, final int minX, final int maxX, final int minZ, final int maxZ) {
			mName = name;
			mMinX = minX;
			mMaxX = maxX;
			mMinZ = minZ;
			mMaxZ = maxZ;
		}
	}

	public UltimateSeismicRuin(final Plugin plugin, final LivingEntity boss, final Location originLoc, final Location northCardinal,
	                           final Location southCardinal, final Location eastCardinal, final Location westCardinal) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mOrigin = originLoc;
		mCardinals = new ArrayList<>(List.of(northCardinal, southCardinal, eastCardinal, westCardinal));
	}

	@Override
	public void run() {
		final int randomDirection = FastUtils.RANDOM.nextInt(mCardinals.size());
		destroyFromDirectionTowardsCenter(mCardinals.get(randomDirection));
		mCardinals.remove(randomDirection);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 5, 0.5f);
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 8;
	}

	private void destroyFromDirectionTowardsCenter(final Location direction) {
		/* ChargeUpManager bars need to be local to this method so Eldrask can have two casts of this spell run for the final phase */
		final ChargeUpManager chargeBar = new ChargeUpManager(mBoss, CHARGE_DURATION, Component.text("Charging ", NamedTextColor.DARK_AQUA)
			.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, FrostGiant.detectionRange);
		final ChargeUpManager castBar = new ChargeUpManager(mBoss, CAST_DURATION, Component.text("Casting ", NamedTextColor.DARK_AQUA)
			.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, FrostGiant.detectionRange);
		final Cardinal cardDir;
		final double deltaX = Math.abs(direction.getX()) - Math.abs(mOrigin.getX());
		final double deltaZ = Math.abs(direction.getZ()) - Math.abs(mOrigin.getZ());

		/* Draw a box based on the Location direction parameter. Since the wave of distruction travels from the edge
		 * towards the center, the corners of the box are determined by the length of the arena and how many layers of
		 * blocks the spell removes. */
		if (deltaZ < 0 && Math.abs(deltaX) < 0.1) {
			cardDir = Cardinal.NORTH;
		} else if (deltaZ > 0 && Math.abs(deltaX) < 0.1) {
			cardDir = Cardinal.SOUTH;
		} else if (deltaX < 0 && Math.abs(deltaZ) < 0.1) {
			cardDir = Cardinal.EAST;
		} else if (deltaX > 0 && Math.abs(deltaZ) < 0.1) {
			cardDir = Cardinal.WEST;
		} else {
			MMLog.warning(() -> "[FrostGiant] Spell UltimateSeismicRuin cast by " + mBoss + " couldn't find which cardinal a location is on! " +
				"mOrigin: " + mOrigin + " direction: " + direction);
			return;
		}

		/* Replace some of the blocks to be destroyed as a tell for players to move */
		final Location blockReplaceLoc = direction.clone().add(0, -1, 0);
		for (double x = direction.getX() + cardDir.mMinX; x <= direction.getX() + cardDir.mMaxX; x++) {
			for (double z = direction.getZ() + cardDir.mMinZ; z <= direction.getZ() + cardDir.mMaxZ; z++) {
				blockReplaceLoc.setX(x);
				blockReplaceLoc.setZ(z);
				if (blockReplaceLoc.getBlock().getType() != Material.AIR) {
					final int rand = FastUtils.RANDOM.nextInt(6);
					if (rand == 0) {
						blockReplaceLoc.getBlock().setType(Material.HONEY_BLOCK);
					} else if (rand < 3) {
						blockReplaceLoc.getBlock().setType(Material.MAGMA_BLOCK);
					}
				}
			}
		}

		/* Align explosion particles on the X or Z axis */
		final PPLine explosionLine = (cardDir == Cardinal.NORTH || cardDir == Cardinal.SOUTH)
			? new PPLine(Particle.EXPLOSION_HUGE, direction.clone().add(cardDir.mMinX, 0, 0), direction.clone().add(cardDir.mMaxX, 0, 0))
			: new PPLine(Particle.EXPLOSION_HUGE, direction.clone().add(0, 0, cardDir.mMinZ), direction.clone().add(0, 0, cardDir.mMaxZ));
		explosionLine.countPerMeter(0.25).delta(0.15).extra(0.05).distanceFalloff(FrostGiant.ARENA_LENGTH);

		/* Spawn a line of explosion particles at the platform edge and a sheet of damage indicator
		 * particles in the area to be destroyed along with warning sounds */
		new BukkitRunnable() {
			final Location mParticleLoc = direction.clone().add(0, 0.1, 0);
			float mPitch = 0.5f;

			@Override
			public void run() {
				final int currentTime = chargeBar.getTime();
				if (currentTime % Constants.QUARTER_TICKS_PER_SECOND == 0) {
					new PPRectPrism(Particle.DAMAGE_INDICATOR, mParticleLoc.clone().add(cardDir.mMinX, 0, cardDir.mMinZ), mParticleLoc.clone().add(cardDir.mMaxX, 0.1, cardDir.mMaxZ))
						.countPerMeterSquared(0.2).delta(0.15).extra(0.25).distanceFalloff(FrostGiant.ARENA_LENGTH).spawnAsEntityActive(mBoss);

					if (currentTime >= Constants.TICKS_PER_SECOND * 4) {
						mWorld.playSound(direction, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1);
					}
				}

				if (currentTime % Constants.HALF_TICKS_PER_SECOND == 0) {
					mWorld.playSound(direction, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2, mPitch);
					mPitch += 0.05f;
				}

				if (currentTime % Constants.TICKS_PER_SECOND == 0) {
					explosionLine.spawnAsEntityActive(mBoss);
				}

				if (chargeBar.nextTick()) {
					chargeBar.reset();
					castBar.setTime(CAST_DURATION);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		/* Incrementally destroy layers of blocks */
		new BukkitRunnable() {
			final Location mLoc = direction.clone();
			boolean mRandomize = false;
			int mLayerIterator = 0;

			@Override
			public void run() {
				if (castBar.getTime() % TICKS_PER_LAYER == 0) {
					switch (cardDir) {
						case NORTH -> {
							destroyXLayer(mLoc, mLoc.getZ() + mLayerIterator, mRandomize);
							mLayerIterator++;
						}
						case SOUTH -> {
							destroyXLayer(mLoc, mLoc.getZ() + mLayerIterator, mRandomize);
							mLayerIterator--;
						}
						case EAST -> {
							destroyZLayer(mLoc, mLoc.getX() + mLayerIterator, mRandomize);
							mLayerIterator--;
						}
						case WEST -> {
							destroyZLayer(mLoc, mLoc.getX() + mLayerIterator, mRandomize);
							mLayerIterator++;
						}
						default -> {
							MMLog.warning(() -> "[UltimateSeismicRuin] Spell somehow hit the default switch case while running on " + cardDir.mName);
							this.cancel();
						}
					}
				}

				if (castBar.getTime() % (TICKS_PER_LAYER * 2) == 0) {
					if (cardDir == Cardinal.NORTH || cardDir == Cardinal.SOUTH) {
						explosionLine.location(mLoc.clone().add(cardDir.mMinX, 0.1, mLayerIterator), mLoc.clone().add(cardDir.mMaxX, 0.1, mLayerIterator));
						mWorld.playSound(mLoc.clone().add(0, 0, mLayerIterator), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f);
					} else {
						explosionLine.location(mLoc.clone().add(mLayerIterator, 0.1, cardDir.mMinZ), mLoc.clone().add(mLayerIterator, 0.1, cardDir.mMaxZ));
						mWorld.playSound(mLoc.clone().add(mLayerIterator, 0, 0), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f);
					}

					explosionLine.spawnAsEntityActive(mBoss);
				}

				if (Math.abs(mLayerIterator) == LAYERS_TO_DESTROY + 1) {
					mRandomize = true;
				}

				castBar.previousTick();
				if (Math.abs(mLayerIterator) >= LAYERS_TO_DESTROY + 4) {
					castBar.reset();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, CHARGE_DURATION, 1);
	}

	private void destroyZLayer(final Location centerLoc, final double xVal, final boolean randomize) {
		final Location loc = centerLoc.clone();
		final double zMin = centerLoc.getZ() - (FrostGiant.ARENA_LENGTH / 2.0);
		final double zMax = centerLoc.getZ() + (FrostGiant.ARENA_LENGTH / 2.0);

		for (double y = centerLoc.getY() - FrostGiant.ARENA_DEPTH; y <= centerLoc.getY(); y++) {
			for (double z = zMin; z <= zMax; z++) {
				loc.set(xVal, y, z);

				if (!randomize) {
					loc.getBlock().setType(Material.AIR);
				} else if (FastUtils.RANDOM.nextInt(3) == 0) {
					loc.getBlock().setType(Material.AIR);
				}
			}
		}
	}

	private void destroyXLayer(final Location centerLoc, final double zVal, final boolean randomize) {
		final Location loc = centerLoc.clone();
		final double xMin = centerLoc.getX() - (FrostGiant.ARENA_LENGTH / 2.0);
		final double xMax = centerLoc.getX() + (FrostGiant.ARENA_LENGTH / 2.0);

		for (double x = xMin; x <= xMax; x++) {
			for (double y = centerLoc.getY() - FrostGiant.ARENA_DEPTH; y <= centerLoc.getY(); y++) {
				loc.set(x, y, zVal);

				if (!randomize) {
					loc.getBlock().setType(Material.AIR);
				} else if (FastUtils.RANDOM.nextInt(3) == 0) {
					loc.getBlock().setType(Material.AIR);
				}
			}
		}
	}
}
