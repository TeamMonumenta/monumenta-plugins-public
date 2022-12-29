package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Volcanic Demise:
 * Death.
=======
 * Volcanic Demise (CD: 20): Kaul starts summoning meteors that fall from the sky in random areas.
 * Each Meteor deals 42 damage in a 4 block radius on collision with the ground.
 * This ability lasts X seconds and continues spawning meteors until the ability duration runs out.
 * Kaul is immune to damage during the channel of this ability.
 */
public class SpellVolcanicDemise extends Spell {

	private static final int DAMAGE = 42;
	private static final int METEOR_COUNT = 25;
	private static final int METEOR_RATE = 10;
	private static final double DEATH_RADIUS = 2;
	private static final double HIT_RADIUS = 5;

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final double mRange;
	private final Location mCenter;
	private final ChargeUpManager mChargeUp;

	public SpellVolcanicDemise(Plugin plugin, LivingEntity boss, double range, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mCenter = center;

		mChargeUp = new ChargeUpManager(mBoss, 20 * 2, ChatColor.GREEN + "Charging " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Volcanic Demise...",
			BarColor.RED, BarStyle.SEGMENTED_10, 60);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mCenter, 50, true);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		for (Player player : players) {
			player.sendMessage(ChatColor.GREEN + "SCATTER, INSECTS.");
		}
		CommandUtils.runCommandViaConsole("function monumenta:kaul/volcanic_demise_count"); // For the advancement "Such Devastation"

		BukkitRunnable runnable = new BukkitRunnable() {

			@Override
			public void run() {
				float fTick = mChargeUp.getTime();
				float ft = fTick / 25;
				new PartialParticle(Particle.LAVA, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.5f + ft);
				if (mChargeUp.nextTick(2)) {
					this.cancel();
					mActiveRunnables.remove(this);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1, 0.7f);

					mChargeUp.setTitle(ChatColor.GREEN + "Unleashing " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Volcanic Demise...");
					BukkitRunnable runnable = new BukkitRunnable() {

						@Override
						public synchronized void cancel() {
							super.cancel();
							mChargeUp.reset();
							mChargeUp.setTitle(ChatColor.GREEN + "Charging " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Volcanic Demise...");
						}

						int mI = 0;

						int mMeteors = 0;

						@Override
						public void run() {
							mI++;

							mChargeUp.setProgress(1 - ((double) mI / (double) (METEOR_COUNT * METEOR_RATE)));

							if (mI % METEOR_RATE == 0) {
								mMeteors++;
								List<Player> players = PlayerUtils.playersInRange(mCenter, 50, true);
								players.removeIf(p -> p.getLocation().getY() >= 61);
								Collections.shuffle(players);
								for (Player player : players) {
									Location loc = player.getLocation();
									if (loc.getBlock().isLiquid() || !loc.toVector().isInSphere(mCenter.toVector(), 42)) {
										loc.setY(mCenter.getY());
										rainMeteor(loc, players, 10);
									}
								}
								for (int j = 0; j < 4; j++) {
									rainMeteor(mCenter.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), 0, FastUtils.randomDoubleInRange(-mRange, mRange)), players, 40);
								}

								// Target one random player. Have a meteor rain nearby them.
								if (players.size() >= 1) {
									Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
									Location loc = rPlayer.getLocation();
									loc.setY(mCenter.getY());
									rainMeteor(loc.add(FastUtils.randomDoubleInRange(-8, 8), 0, FastUtils.randomDoubleInRange(-8, 8)), players, 40);
								}

								if (mMeteors >= METEOR_COUNT) {
									this.cancel();
									mActiveRunnables.remove(this);
								}
							}

						}

					};
					runnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable);
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void rainMeteor(Location locInput, List<Player> players, double spawnY) {
		if (locInput.distance(mCenter) > 50 || locInput.getY() >= 55) {
			// Somehow tried to spawn a meteor too far away from the center point
			return;
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			double mY = spawnY;
			final Location mLoc = locInput.clone();
			final World mWorld = locInput.getWorld();

			@Override
			public void run() {
				players.removeIf(p -> p.getLocation().distance(mCenter) > 50 || p.getLocation().getY() >= 61);

				mY -= 1;
				if (mY > 0 && (int) mY % 3 == 0) {
					new PPCircle(Particle.LAVA, mLoc, DEATH_RADIUS / 2)
						.ringMode(false)
						.count(10)
						.distanceFalloff(20)
						.spawnAsBoss();
				}
				Location particle = mLoc.clone().add(0, mY, 0);
				new PartialParticle(Particle.FLAME, particle, 2, 0.2f, 0.2f, 0.2f, 0.05)
					.distanceFalloff(20).spawnAsBoss();
				if (FastUtils.RANDOM.nextBoolean()) {
					new PartialParticle(Particle.SMOKE_LARGE, particle, 1, 0, 0, 0, 0)
						.distanceFalloff(20).spawnAsBoss();
				}
				mWorld.playSound(particle, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);
				if (mY <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					new PartialParticle(Particle.FLAME, mLoc, 50, 0, 0, 0, 0.175)
						.distanceFalloff(20).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 10, 0, 0, 0, 0.25)
						.distanceFalloff(20).spawnAsBoss();
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.9f);
					Hitbox deathBox = new Hitbox.UprightCylinderHitbox(mLoc, 7, DEATH_RADIUS);
					Hitbox hitBox = new Hitbox.UprightCylinderHitbox(mLoc, 15, HIT_RADIUS);
					List<Player> hitPlayers = new ArrayList<>(hitBox.getHitPlayers(true));
					for (Player player : deathBox.getHitPlayers(true)) {
						DamageUtils.damage(mBoss, player, DamageType.BLAST, 1000, null, false, true, "Volcanic Demise");
						MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
						hitPlayers.remove(player);
					}
					for (Player player : hitPlayers) {
						BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, DAMAGE, "Volcanic Demise", mLoc);
						if (!BossUtils.bossDamageBlocked(player, mLoc)) {
							MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
						}
					}
					for (Block block : LocationUtils.getNearbyBlocks(mLoc.getBlock(), 4)) {
						if (FastUtils.RANDOM.nextDouble() < 0.125) {
							if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
								block.setType(Material.NETHERRACK);
							} else if (block.getType() == Material.NETHERRACK) {
								block.setType(Material.MAGMA_BLOCK);
							} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
								block.setType(Material.SMOOTH_RED_SANDSTONE);
							}
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int castTicks() {
		return 20 * 17;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 35;
	}

}
