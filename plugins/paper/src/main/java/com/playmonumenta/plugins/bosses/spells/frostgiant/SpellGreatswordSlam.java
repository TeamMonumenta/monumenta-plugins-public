package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class SpellGreatswordSlam extends Spell {
	private static final String SPELL_NAME = "Greatsword Slam";
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);
	private static final Particle.DustOptions GRAY_COLOR = new Particle.DustOptions(Color.fromRGB(156, 156, 156), 1.0f);

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final double mDeg;
	private final int mIceDuration;
	private final World mWorld;
	private final Location mStartLoc;
	private final ChargeUpManager mChargeManager;
	private final List<Block> mChangedBlocks = new ArrayList<>();

	public SpellGreatswordSlam(final Plugin plugin, final FrostGiant frostGiant, final int iceDuration, final double deg,
							   final Location startLoc) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mIceDuration = iceDuration;
		mDeg = deg;
		mWorld = mBoss.getWorld();
		mStartLoc = startLoc;
		mChargeManager = new ChargeUpManager(mBoss, CHARGE_DURATION, Component.text("Casting ", NamedTextColor.DARK_AQUA)
			.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, FrostGiant.detectionRange);
	}

	@Override
	public void run() {
		if (mFrostGiant.getArenaParticipants().isEmpty()) {
			return;
		}

		final Location bossLoc = mBoss.getLocation();

		mFrostGiant.freezeGolems();
		mWorld.playSound(bossLoc, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 5, 1);
		mWorld.playSound(bossLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 1.5f);
		new PPCircle(Particle.REDSTONE, mBoss.getLocation(), 3).count(1).delta(0.15).data(GRAY_COLOR).spawnAsEntityActive(mBoss);

		final Mob mMob = (Mob) mBoss;
		final Pathfinder pathfinder = mMob.getPathfinder();
		pathfinder.stopPathfinding();
		final Vector bossDir = bossLoc.getDirection();
		final BukkitRunnable warningRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeManager.nextTick()) {
					mChargeManager.reset();
					this.cancel();
				}

				if (mChargeManager.getTime() <= mChargeManager.getChargeTime()) {
					mBoss.teleport(mBoss.getLocation().setDirection(bossDir));
				}

				/* Spawn particles once every half second */
				if (mChargeManager.getTime() % Constants.HALF_TICKS_PER_SECOND != 0) {
					return;
				}

				for (int r = 0; r < 30; r += 2) {
					for (double degree = 90 - mDeg / 2; degree <= 90 + mDeg / 2; degree += 5) {
						Vector vec = new Vector(FastUtils.cosDeg(degree) * r, 0, FastUtils.sinDeg(degree) * r);
						vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw());

						final Location l = bossLoc.clone().add(vec);
						while (l.getBlock().getType() != Material.AIR && l.getBlockY() <= FrostGiant.ARENA_FLOOR_Y + 3) {
							l.add(0, 1, 0);
						}
						new PartialParticle(Particle.SPELL_WITCH, l, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.END_ROD, l, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
					}
				}
			}
		};
		warningRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(warningRunnable);

		mChangedBlocks.clear();

		/* TODO: This runnable should probably get a rewrite so it's not a mess of nested code but it is brittle and I don't want to deal with it */
		final BukkitRunnable jumpRunnable = new BukkitRunnable() {
			int mT = 0;
			final List<Player> mHitPlayers = new ArrayList<>();

			@Override
			public void run() {
				mT += 2;

				if (mT <= (int) (Constants.TICKS_PER_SECOND * 1.5) && mT >= Constants.TICKS_PER_SECOND) {
					//Initiates the jump upwards
					mBoss.setVelocity(new Vector(0, 1.5, 0));
				} else if (mT >= (int) (Constants.TICKS_PER_SECOND * 1.5)) {
					if (!mBoss.isOnGround()) {
						//Initiates the slam down
						mBoss.setVelocity(new Vector(0, -1.5, 0));
					} else {
						//Creates the giant 30 degree cone rift of damage
						mWorld.playSound(bossLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0);
						final BukkitRunnable runnable = new BukkitRunnable() {
							int mRadius = 0;

							@Override
							public void run() {
								mBoss.setVelocity(new Vector(0, 0, 0));
								pathfinder.stopPathfinding();

								if (mRadius >= 30) {
									this.cancel();
								}

								//In the current radius, makes a cone of frosted ice and various other particles
								//If player is in trajectory (in bounding box), damage them and knock back
								Vector vec;
								final List<BoundingBox> boxes = new ArrayList<>();
								for (double degree = 90 - mDeg / 2; degree <= 90 + mDeg / 2; degree += 5) {
									vec = new Vector(FastUtils.cosDeg(degree) * mRadius, 0, FastUtils.sinDeg(degree) * mRadius);
									vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw());

									//Also have to clone location because of use in HashMap, can not optimize
									final Location l = bossLoc.clone().add(vec).add(0, -1, 0);
									//Move down one block to not overshoot, sometimes boss can stand on a single block, affects location
									if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
										l.add(0, -1, 0);
									}
									//Once it leaves the arena, stop iterating
									if ((l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
										    || l.distance(mStartLoc) > FrostGiant.ARENA_RADIUS) {
										continue;
									}
									//If on bedrock or barriers, move up one to not replace that
									if (l.getBlock().getType() == Material.BEDROCK || l.getBlock().getType() == Material.BARRIER) {
										l.add(0, 1, 0);
									}

									//Put less frosted ice than the entire cone
									if (degree % 10 == 0) {
										final Block block = l.getBlock();
										if (block.getType() != SpellFrostRift.RIFT_BLOCK_TYPE
											    && TemporaryBlockChangeManager.INSTANCE.changeBlock(block, FrostGiant.ICE_TYPE,
											Constants.TICKS_PER_SECOND * mIceDuration - mRadius + FastUtils.randomIntInRange(0, 10))) {
											mChangedBlocks.add(block);
											final Ageable age = (Ageable) block.getBlockData();
											age.setAge(1 + FastUtils.RANDOM.nextInt(3));
											block.setBlockData(age);
										}
									}

									final BoundingBox box = BoundingBox.of(l, 1, 3.65, 1);
									boxes.add(box);
									final FallingBlock fallBlock = mWorld.spawn(l.add(0, 0.4, 0), FallingBlock.class,
										CreatureSpawnEvent.SpawnReason.CUSTOM, (final FallingBlock ice) -> {
											ice.setBlockData(Bukkit.createBlockData(Material.BLUE_ICE));
											ice.setVelocity(new Vector(0, 0.4, 0));
											ice.setDropItem(false);
											ice.setHurtEntities(false);
											EntityUtils.disableBlockPlacement(ice);
										});

									Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
										if (fallBlock.isValid()) {
											fallBlock.remove();
										}
									}, Constants.TICKS_PER_SECOND);

									new PartialParticle(Particle.CLOUD, l, 2, 0.15, 0.15, 0.15, 0.125).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.CRIT, l, 8, 0.15, 0.15, 0.15, 0.7).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.REDSTONE, l, 8, 0.15, 0.15, 0.15, BLUE_COLOR).spawnAsEntityActive(mBoss);
									if (degree > 85 && degree < 95 && mRadius % 5 == 0) {
										mWorld.playSound(l, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0.5f);
									}
								}
								for (final Player player : mFrostGiant.getArenaParticipants()) {
									for (final BoundingBox box : boxes) {
										if (player.getBoundingBox().overlaps(box) && !mHitPlayers.contains(player)) {
											DamageUtils.damage(mBoss, player, DamageType.MAGIC, 18, null, false, true, SPELL_NAME);
											AbilityUtils.silencePlayer(player, Constants.TICKS_PER_SECOND * 5);
											MovementUtils.knockAway(bossLoc, player, 0f, 1.5f, false);
											mHitPlayers.add(player);
											break;
										}
									}
								}
								mRadius++;
							}
						};
						runnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnable);

						mFrostGiant.unfreezeGolems();
						this.cancel();
					}
				} else {
					mBoss.setVelocity(new Vector(0, 0, 0));
					pathfinder.stopPathfinding();
				}
			}
		};
		jumpRunnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(jumpRunnable);
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, FrostGiant.ICE_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 7;
	}
}
