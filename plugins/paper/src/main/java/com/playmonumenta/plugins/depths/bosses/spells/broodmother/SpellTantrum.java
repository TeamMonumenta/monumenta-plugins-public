package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellTantrum extends Spell {

	public static final String SPELL_NAME = "Tantrum";
	public static final int COOLDOWN = 800;
	public static final int FISSURE_COUNT = 5;
	public static final int FISSURE_DELAY = 10;
	public static final int CAST_TIME = FISSURE_COUNT * (FISSURE_DELAY + 1);
	public static final int FISSURE_TRAVEL_TICKS = 60;
	public static final double TRAVEL_SPEED = 1;
	public static final double TRAVEL_QUAKE_RADIUS = 1.75;
	public static final double HIT_QUAKE_RADIUS = 5;
	public static final double KNOCKUP_VELOCITY = 1.6;
	public static final double DAMAGE = 70;
	public static final List<Material> GROUND_QUAKE_BLOCKS = List.of(Material.TERRACOTTA, Material.PACKED_MUD, Material.BROWN_MUSHROOM_BLOCK, Material.DRIPSTONE_BLOCK);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final ChargeUpManager mChargeUp;
	private final ArrayList<BukkitTask> mTantrumTasks = new ArrayList<>();
	private final int mFinalCooldown;

	private boolean mOnCooldown = false;

	public SpellTantrum(LivingEntity boss, @Nullable DepthsParty party) {
		mBoss = boss;
		mPlugin = Plugin.getInstance();

		mFinalCooldown = DepthsParty.getAscensionEigthCooldown(COOLDOWN, party);

		mChargeUp = new ChargeUpManager(mBoss, CAST_TIME,
			Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)),
			BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, 100);
	}

	@Override
	public void run() {
		mOnCooldown = true;

		mChargeUp.reset();
		mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 10.0f, 1.5f);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					tantrumInternal();
					cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		// Handle Cooldown
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, mFinalCooldown);
	}

	@Override
	public int cooldownTicks() {
		return CAST_TIME;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void tantrumInternal() {
		mChargeUp.setTitle(Component.text("Unleashing ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTimesRun = 0;

			@Override
			public void run() {
				List<Player> possibleTargets = PlayerUtils.playersInRange(mBoss.getLocation(), 100, false);

				if (possibleTargets.size() != 0) {
					Collections.shuffle(possibleTargets);
					// Alternate the starting location between the two frontal legs
					boolean leftSide = mTimesRun % 2 == 0;
					Location startLoc = leftSide ? mBoss.getLocation().clone().add(-8, -1, -8) : mBoss.getLocation().clone().add(-8, -1, 8);
					Location endLoc = possibleTargets.get(0).getLocation();
					startLoc.setY(Broodmother.GROUND_Y_LEVEL);
					endLoc.setY(Broodmother.GROUND_Y_LEVEL);
					// Draw a line from the start location to the end location as telegraph
					Vector dir = LocationUtils.getDirectionTo(endLoc, startLoc);
					new PPLine(Particle.FLAME, startLoc, startLoc.clone().add(dir.multiply(FISSURE_TRAVEL_TICKS * TRAVEL_SPEED))).countPerMeter(3).spawnAsBoss();
					// Launch the spell after a slight delay
					int fissureNumber = mTimesRun + 1;
					mTantrumTasks.add(
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> launchFissure(startLoc, endLoc, leftSide, fissureNumber, mChargeUp), FISSURE_DELAY)
					);
				}

				mTimesRun++;
				if (mTimesRun >= FISSURE_COUNT) {
					cancel();
				}
			}
		};
		mTantrumTasks.add(
			runnable.runTaskTimer(mPlugin, 0, FISSURE_DELAY)
		);
	}

	private void launchFissure(Location startLoc, Location endLoc, boolean leftSide, int fissureNumber, ChargeUpManager chargeUp) {
		startLoc.getWorld().playSound(startLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 5.0f, 0.5f);
		chargeUp.setProgress(1.0 - ((double) fissureNumber / (double) FISSURE_COUNT));

		// Animate the legs slamming the ground
		if (leftSide) {
			StructuresAPI.loadAndPasteStructure("BikeSpiderTantrumLeft", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
			mTantrumTasks.add(
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> StructuresAPI.loadAndPasteStructure("BikeSpiderLeftLegReset", mBoss.getLocation().clone().add(-8, -1, -12), false, false), FISSURE_DELAY)
			);
		} else {
			StructuresAPI.loadAndPasteStructure("BikeSpiderTantrumRight", mBoss.getLocation().clone().add(-8, -1, -12), false, false);
			mTantrumTasks.add(
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> StructuresAPI.loadAndPasteStructure("BikeSpiderRightLegReset", mBoss.getLocation().clone().add(-8, -1, -12), false, false), FISSURE_DELAY)
			);
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			final Location mCurrentLoc = startLoc;
			final Vector mDirectionStep = LocationUtils.getDirectionTo(endLoc, startLoc).multiply(TRAVEL_SPEED);

			int mTicks = 0;

			@Override
			public void run() {
				travelAesthetics();

				// Check if the fissure has expired
				if (mTicks >= FISSURE_TRAVEL_TICKS) {
					cancel();
					return;
				}

				// Check if it hits a player (even in stealth) during the travel
				tryDamagePlayers(mCurrentLoc);
				// Travel towards end location
				mCurrentLoc.add(mDirectionStep);
				mTicks++;
			}

			private void travelAesthetics() {
				mCurrentLoc.getWorld().playSound(mCurrentLoc, Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 2.0f, 1.0f);
				mCurrentLoc.getWorld().playSound(mCurrentLoc, Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 2.0f, 1.0f);
				DisplayEntityUtils.groundBlockQuake(mCurrentLoc, TRAVEL_QUAKE_RADIUS, GROUND_QUAKE_BLOCKS, new Display.Brightness(8, 8), 0.002);
			}

			private void hitAesthetics(Location loc) {
				Entity d1 = loc.getWorld().spawnEntity(loc.clone().add(0, -0.1, 0), EntityType.BLOCK_DISPLAY);
				if (d1 instanceof BlockDisplay display1) {
					display1.setBlock(Material.TERRACOTTA.createBlockData());
					new DisplayEntityUtils.DisplayAnimation(display1)
						.addKeyframe(DisplayEntityUtils.getScale(1.1f, 5, 1.1f), 3)
						.addDelay(20)
						.addKeyframe(DisplayEntityUtils.getScale(0, 0, 0), 8)
						.removeDisplaysAfterwards()
						.play();
				}

				Entity d2 = loc.getWorld().spawnEntity(loc.clone().add(-0.375, -0.5375, 0.25), EntityType.BLOCK_DISPLAY);
				if (d2 instanceof BlockDisplay display2) {
					display2.setBlock(Material.TERRACOTTA.createBlockData());
					new DisplayEntityUtils.DisplayAnimation(display2)
						.addKeyframe(new Transformation(new Vector3f(), new Quaternionf(0.25000572f, 0.066989325f, -0.25000107f, 0.9330109f), new Vector3f(0.8f, 2.5f, 0.8f), new Quaternionf()), 3)
						.addDelay(20)
						.addKeyframe(DisplayEntityUtils.getScale(0, 0, 0), 8)
						.removeDisplaysAfterwards()
						.play();
				}

				Entity d3 = loc.getWorld().spawnEntity(loc.clone().add(0.5, -0.6, -0.375), EntityType.BLOCK_DISPLAY);
				if (d3 instanceof BlockDisplay display3) {
					display3.setBlock(Material.TERRACOTTA.createBlockData());
					new DisplayEntityUtils.DisplayAnimation(display3)
						.addKeyframe(new Transformation(new Vector3f(), new Quaternionf(-0.25000572f, -0.066989325f, -0.25000107f, 0.9330109f), new Vector3f(0.8f, 2.5f, 0.8f), new Quaternionf()), 3)
						.addDelay(20)
						.addKeyframe(DisplayEntityUtils.getScale(0, 0, 0), 8)
						.removeDisplaysAfterwards()
						.play();
				}

				Entity d4 = loc.getWorld().spawnEntity(loc.clone().add(-0.375, -1.0375, -0.125), EntityType.BLOCK_DISPLAY);
				if (d4 instanceof BlockDisplay display4) {
					display4.setBlock(Material.TERRACOTTA.createBlockData());
					new DisplayEntityUtils.DisplayAnimation(display4)
						.addKeyframe(new Transformation(new Vector3f(), new Quaternionf(-0.25000572f, 0.066989325f, 0.25000107f, 0.9330109f), new Vector3f(0.8f, 2.5f, 0.8f), new Quaternionf()), 3)
						.addDelay(20)
						.addKeyframe(DisplayEntityUtils.getScale(0, 0, 0), 8)
						.removeDisplaysAfterwards()
						.play();
				}

				Entity d5 = loc.getWorld().spawnEntity(loc.clone().add(-0.375, -0.5375, 0.25), EntityType.BLOCK_DISPLAY);
				if (d5 instanceof BlockDisplay display5) {
					display5.setBlock(Material.TERRACOTTA.createBlockData());
					new DisplayEntityUtils.DisplayAnimation(display5)
						.addKeyframe(new Transformation(new Vector3f(), new Quaternionf(0.25000572f, -0.066989325f, 0.25000107f, 0.9330109f), new Vector3f(0.8f, 2.5f, 0.8f), new Quaternionf()), 3)
						.addDelay(20)
						.addKeyframe(DisplayEntityUtils.getScale(0, 0, 0), 8)
						.removeDisplaysAfterwards()
						.play();
				}
			}

			private void tryDamagePlayers(Location loc) {
				List<Player> hitPlayers = PlayerUtils.playersInRange(loc, TRAVEL_QUAKE_RADIUS, true);
				if (hitPlayers.size() > 0) {
					DisplayEntityUtils.groundBlockQuake(loc, HIT_QUAKE_RADIUS, GROUND_QUAKE_BLOCKS, new Display.Brightness(8, 8), 0.03);
					hitPlayers.forEach(p -> {
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, DAMAGE, null, true, true, SPELL_NAME);
						p.setVelocity(p.getVelocity().add(new Vector(0, KNOCKUP_VELOCITY, 0)));
						hitAesthetics(p.getLocation());
					});
					cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	public void stopTantrumTasks() {
		mTantrumTasks.forEach(BukkitTask::cancel);
	}
}
