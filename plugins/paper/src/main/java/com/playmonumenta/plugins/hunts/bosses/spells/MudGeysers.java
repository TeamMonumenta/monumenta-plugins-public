package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class MudGeysers extends Spell {
	private static final int WINDUP_DURATION = 2 * 20;
	private static final int GEYSER_ANIMATION = (int) (0.5 * 20);
	private static final double GEYSER_DURATION = 5 * 20;
	private static final double RADIUS = 3.25;
	private static final double DAMAGE = 60;
	private static final Material[] GEYSER_BLOCKS = new Material[]{
		Material.MUD, Material.SOUL_SAND
	};

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperiment;

	public MudGeysers(Plugin plugin, LivingEntity boss, ExperimentSeventyOne experiment) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperiment = experiment;
	}

	@Override
	public void run() {
		List<Location> chosenLocs = new ArrayList<>();
		List<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), 40, true).stream().filter(player -> player.getGameMode() != GameMode.SPECTATOR).collect(Collectors.toList());
		Collections.shuffle(nearbyPlayers);
		for (int i = 0; i < Math.min(3, nearbyPlayers.size()); i++) {
			Location location = LocationUtils.varyInCircle(nearbyPlayers.get(i).getLocation(), 4);
			chosenLocs.add(LocationUtils.fallToGround(location, 5).add(0, 0.75, 0));
		}

		List<List<Location>> windupLocs = new ArrayList<>();
		for (Location loc : chosenLocs) {
			List<Location> locs = getLocationsInCircle(loc, (int) RADIUS)
				.stream().filter((loc2) -> {
					// check if block beneath is not an air block
					loc2 = loc2.clone().add(0, -1, 0);
					return loc2.isChunkLoaded() && !loc2.getBlock().isEmpty();
				})
				.collect(Collectors.toList());
			if (locs.isEmpty()) {
				continue;
			}
			windupLocs.add(locs);
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks < WINDUP_DURATION) {
					List<BlockDisplay> displays = new ArrayList<>();
					for (List<Location> locs : windupLocs) {
						displays.add(spawnGeyserBlock(locs.get(FastUtils.randomIntInRange(0, locs.size() - 1))));
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						displays.forEach((display) -> {
							display.setTransformationMatrix(new Matrix4f().translate(0, 0.25f, 0));
							display.setInterpolationDelay(0);
							display.setInterpolationDuration(5);
						});
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							for (BlockDisplay display : displays) {
								if (display.isValid()) {
									display.remove();
								}
							}
						}, 5);
					}, 1);
					if (mTicks % 5 == 0) {
						for (Location loc : chosenLocs) {
							new PPCircle(Particle.BLOCK_CRACK, loc, RADIUS - 0.25).data(Material.MUD.createBlockData()).ringMode(false).countPerMeter(2).spawnAsBoss();
							new PPCircle(Particle.REDSTONE, loc, RADIUS).data(new Particle.DustOptions(Color.fromRGB(200, 80, 0), 1f)).countPerMeter(2).spawnAsBoss();
							new PPSpiral(Particle.WATER_DROP, loc, RADIUS).count(1).spawnAsBoss();
							mWorld.playSound(loc, Sound.BLOCK_MUD_STEP, 1f, 0.5f);
							mWorld.playSound(loc, Sound.BLOCK_SOUL_SAND_STEP, 1f, 0.5f);
						}
					}
				} else if (mTicks < WINDUP_DURATION + GEYSER_DURATION) {
					if (mTicks == WINDUP_DURATION) {
						for (Location location : chosenLocs) {
							mWorld.playSound(location, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.8f, 2f);
							mWorld.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 2f, 0.75f);
							mWorld.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
						}
					}
					if ((mTicks - WINDUP_DURATION) % GEYSER_ANIMATION == 0) {
						for (Location location : chosenLocs) {
							launchGeyser(location.clone());
						}
					}
				} else {
					this.cancel();
				}
				mTicks++;
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void launchGeyser(Location location) {
		mWorld.playSound(location, Sound.AMBIENT_UNDERWATER_EXIT, 2f, 0.6f);

		// Damage players
		Hitbox hitbox = new Hitbox.UprightCylinderHitbox(location.clone().subtract(0, 2, 0), 10, RADIUS);
		for (Player p : hitbox.getHitPlayers(false)) {
			DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, "Mud Geysers");
			MovementUtils.knockAway(location, p, 0.3f, 1.5f, false);
			p.playSound(location, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1f, 1f);
		}

		double dustDelta = Math.max(RADIUS / 2 - 0.25, 0);
		final double dy = 1;
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PPCircle(Particle.REDSTONE, location, RADIUS)
					.data(new Particle.DustOptions(Color.fromRGB(68, 64, 64), 1.5f))
					.ringMode(false)
					.count(15)
					.delta(0, dustDelta, 0)
					.spawnAsBoss();
				new PPCircle(Particle.REDSTONE, location, RADIUS)
					.data(new Particle.DustOptions(Color.fromRGB(68, 64, 64), 1.5f))
					.ringMode(true)
					.count(15)
					.delta(0, dustDelta, 0)
					.spawnAsBoss();
				new PPCircle(Particle.BLOCK_CRACK, location, RADIUS)
					.data(Material.SOUL_SAND.createBlockData())
					.ringMode(false)
					.count(15)
					.delta(0, dustDelta, 0)
					.spawnAsBoss();
				new PPCircle(Particle.BLOCK_CRACK, location, RADIUS)
					.data(Material.SOUL_SAND.createBlockData())
					.ringMode(true)
					.count(15)
					.delta(0, dustDelta, 0)
					.spawnAsBoss();
				location.add(0, dy, 0);

				mTicks++;
				if (mTicks > GEYSER_ANIMATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private BlockDisplay spawnGeyserBlock(Location loc) {
		// move one block down and center location
		loc = loc.clone().add(-0.5, -1.5, -0.5).setDirection(new Vector(1, 0, 0));
		return loc.getWorld().spawn(loc, BlockDisplay.class, entity -> {
			entity.setBlock(GEYSER_BLOCKS[FastUtils.randomIntInRange(0, 1)].createBlockData());
			entity.setPersistent(false);
			entity.setBrightness(new Display.Brightness(0, 15));
			EntityUtils.setRemoveEntityOnUnload(entity);
		});
	}

	private List<Location> getLocationsInCircle(Location center, int radius) {
		List<Location> locs = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if ((x * x) + (z * z) <= (radius * radius)) {
					locs.add(center.clone().add(new Vector(x, 0, z)));
				}
			}
		}
		return locs;
	}

	@Override
	public int cooldownTicks() {
		return 15 + WINDUP_DURATION + GEYSER_ANIMATION;
	}

	@Override
	public boolean canRun() {
		return mExperiment.canRunSpell(this) && !mExperiment.getMudBlocks().isEmpty();
	}
}
