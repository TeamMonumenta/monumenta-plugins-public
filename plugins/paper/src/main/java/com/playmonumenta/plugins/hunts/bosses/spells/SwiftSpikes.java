package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SwiftSpikes extends Spell {

	// Windup duration of the attack
	private static final int WINDUP_DURATION = 2 * 20;

	// Amount of players to target
	private static final int MARKED_AREAS = 3;

	// Maximum distance from the boss for the attack to target
	private static final int SPIKE_MAX_DISTANCE = 14;

	// Radius of the telegraph and attack hitbox
	private static final double ATTACK_RADIUS = 3;

	// Attack damage
	private static final int ATTACK_DAMAGE = 85;

	private static final Color START_COLOR = Color.fromRGB(170, 139, 179);

	private static final Color END_COLOR = Color.fromRGB(127, 45, 138);

	private final Material mAirBlock = Material.AIR;
	private final Material mAmethystBlock = Material.AMETHYST_BLOCK;
	private final Material mGlassBlock = Material.PURPLE_STAINED_GLASS;
	private final Material mCrystal = Material.LARGE_AMETHYST_BUD;
	private final Material mCrystalTop = Material.AMETHYST_CLUSTER;

	// a most heinous crime indeed [y][z][x]
	private final Material[][][] mCrystalData = {
		{
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAmethystBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAmethystBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mGlassBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mCrystal, mAirBlock, mAirBlock},
			{mAirBlock, mGlassBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAirBlock, mAirBlock, mGlassBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mCrystal, mAmethystBlock, mGlassBlock, mAirBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mCrystal},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAirBlock, mAirBlock, mCrystal, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAmethystBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAirBlock, mCrystal, mAmethystBlock, mGlassBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mGlassBlock, mAmethystBlock, mAmethystBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mCrystal, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mGlassBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAmethystBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock}
		},
		{
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mCrystalTop, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock},
			{mAirBlock, mAirBlock, mAirBlock, mAirBlock, mAirBlock}
		}
	};


	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	public SwiftSpikes(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
	}

	@Override
	public void run() {
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), SPIKE_MAX_DISTANCE, true);
		if (nearbyPlayers.isEmpty()) {
			return; // Found no in-range players to target, end the spell (should never happen due to canRun())
		}

		List<Location> chosenLocs = getTargetLocations(nearbyPlayers);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// Telegraph
				if (mTicks == 0) {
					for (Location loc : chosenLocs) {
						mWorld.playSound(loc, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, SoundCategory.HOSTILE, 5f, 0.7f);
						mWorld.playSound(loc, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, SoundCategory.HOSTILE, 5f, 0.7f);
						mWorld.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 2.5f, 1.3f);
						mWorld.playSound(loc, Sound.ITEM_AXE_STRIP, SoundCategory.HOSTILE, 3f, 1.1f);
					}
				}
				if (mTicks < WINDUP_DURATION) {
					if (mTicks % 2 == 0) {
						for (Location loc : chosenLocs) {
							Color color = Color.fromRGB((int) (START_COLOR.getRed() + (END_COLOR.getRed() - START_COLOR.getRed()) * ((double) mTicks / WINDUP_DURATION)),
								(int) (START_COLOR.getGreen() + (END_COLOR.getGreen() - START_COLOR.getGreen()) * ((double) mTicks / WINDUP_DURATION)),
								(int) (START_COLOR.getBlue() + (END_COLOR.getBlue() - START_COLOR.getBlue()) * ((double) mTicks / WINDUP_DURATION)));
							new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.2, 0), ATTACK_RADIUS)
								.data(new Particle.DustOptions(color, 1))
								.countPerMeter(7)
								.delta(0, 0.2, 0)
								.ringMode(true)
								.spawnAsBoss();
							new PartialParticle(Particle.BLOCK_CRACK, loc.clone().add(0, 0.05, 0))
								.data(Material.AMETHYST_BLOCK.createBlockData())
								.delta(0.1, 0, 0.1)
								.count(4)
								.extra(0)
								.spawnAsBoss();
						}
					}
				}

				// Attack
				if (mTicks == WINDUP_DURATION) {
					for (Location loc : chosenLocs) {
						// the crime continues
						int xOffset = mCrystalData[0][0].length / 2;
						int zOffset = mCrystalData[0].length / 2;

						for (int t = 0; t < 3; t++) {
							int finalT = t;
							Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
								int start = (int) (((double) mCrystalData.length / 3) * (2 - finalT));

								for (int i = 0; i < mCrystalData[0][0].length; i++) {
									for (int j = 0; j < mCrystalData[0].length; j++) {
										for (int k = start; k < mCrystalData.length; k++) {
											Block b = loc.clone().add(i - xOffset, k - 1 - start, j - zOffset).getBlock();
											Material m = mCrystalData[k][j][i];
											if (m != Material.AIR) {
												TemporaryBlockChangeManager.INSTANCE.changeBlock(b, m, 20 + (3 * (2 - finalT)));
											}
										}
									}
								}
							}, ((long) t) * 2);
						}

						mWorld.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 2.0f, 1.1f);

						// Damage players
						Hitbox hitbox = new Hitbox.UprightCylinderHitbox(loc, 8, ATTACK_RADIUS);
						for (Player player : hitbox.getHitPlayers(true)) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, ATTACK_DAMAGE, null, false, true, "Swift Spikes");
							MovementUtils.knockAway(loc, player, 0f, 1.1f, false);

							player.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 2);
							player.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1.5f, 1);
						}
					}
				}

				mTicks++;

				if (mTicks > WINDUP_DURATION + 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	// Get target locations
	private List<Location> getTargetLocations(List<Player> players) {
		List<Location> result = new ArrayList<>();
		Player target = null;
		double maxDistanceSquared = 0;
		for (Player player : players) {
			double distanceSquared = mBoss.getLocation().distanceSquared(player.getLocation());
			if (distanceSquared > maxDistanceSquared) {
				maxDistanceSquared = distanceSquared;
				target = player;
			}
		}
		if (target != null) {
			Location l = getCenteredOnNearestGround(target.getLocation(), 5);
			if (l != null) {
				result.add(l);
			}
			players.remove(target);
		}

		Collections.shuffle(players);
		while (result.size() < MARKED_AREAS && !players.isEmpty()) {
			Player p = players.remove(0);
			Location loc = getCenteredOnNearestGround(p.getLocation(), 5);
			if (loc == null || result.stream().anyMatch(l -> l.distanceSquared(loc) < 4 * ATTACK_RADIUS * ATTACK_RADIUS)) {
				continue;
			}
			result.add(loc);
		}
		return result;
	}

	// Returns the open location above the nearest solid ground block vertically down from the starting location
	private @Nullable Location getCenteredOnNearestGround(Location startingLoc, int blocksDown) {
		Location loc = null;
		for (int i = 0; i < blocksDown; i++) {
			if (startingLoc.clone().add(0, -i, 0).getBlock().isEmpty()
				&& startingLoc.clone().add(0, -i - 1, 0).getBlock().getType().isOccluding()) {
				loc = startingLoc.clone().add(0, -i, 0);
				break;
			} else if (!startingLoc.clone().add(0, -i, 0).getBlock().isEmpty()) {
				loc = startingLoc.clone();
				break;
			}
		}
		if (loc != null) {
			loc.set(Math.floor(loc.getX()) + 0.5, Math.floor(loc.getY()), Math.floor(loc.getZ()) + 0.5);
			return loc;
		}
		return null;
	}

	@Override
	public int cooldownTicks() {
		return 2 * 20;
	}

	@Override
	public boolean canRun() {
		return !PlayerUtils.playersInRange(mBoss.getLocation(), SPIKE_MAX_DISTANCE, true).isEmpty();
	}
}
