package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LesbianClawsCS extends GraspingClawsCS {

	public static final String NAME = "Lesbian Claws";
	private static final int SLASHES_PER_TICK = 4;
	private static final int TICKS = 3;

	public static final Color LESBIAN_RED = Color.fromRGB(0xD52D00);
	public static final Color LESBIAN_ORANGE = Color.fromRGB(0xEF7627);
	public static final Color LESBIAN_LIGHT_ORANGE = Color.fromRGB(0xFF9A56);
	public static final Color LESBIAN_WHITE = Color.WHITE;
	public static final Color LESBIAN_PINK = Color.fromRGB(0xD162A4);
	public static final Color LESBIAN_DUSTY_PINK = Color.fromRGB(0xB55690);
	public static final Color LESBIAN_HOT_PINK = Color.fromRGB(0xA30262);

	public static final List<Color> LESBIAN_COLORS = List.of(LESBIAN_RED, LESBIAN_ORANGE, LESBIAN_LIGHT_ORANGE, LESBIAN_WHITE, LESBIAN_PINK, LESBIAN_DUSTY_PINK, LESBIAN_HOT_PINK);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Love blooming like violets,",
			"a wave of colors emerge out of your hands."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.POINTED_DRIPSTONE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void onLand(Player player, World world, Location loc, double radius) {
		new BukkitRunnable() {
			int mTicks = 1;

			@Override
			public void run() {
				Location cLoc = loc.clone();
				cLoc.setDirection(VectorUtils.randomUnitVector());

				for (int i = mTicks * SLASHES_PER_TICK; i < (mTicks + 1) * SLASHES_PER_TICK; i++) {
					int finalI = i;
					ParticleUtils.drawHalfArc(cLoc, radius * (1 - (double) i / (TICKS * SLASHES_PER_TICK)), FastUtils.randomDoubleInRange(0, 360), 0, 360, 2, 0.25,
						(l, rings, angleProgress) ->
							new PartialParticle(Particle.REDSTONE, l)
								.data(new Particle.DustOptions(LESBIAN_COLORS.get(finalI % LESBIAN_COLORS.size()), 1.1f))
								.spawnAsPlayerActive(player));
				}
				mTicks++;
				if (mTicks > TICKS) {
					this.cancel();
					ParticleUtils.drawFlag(player, loc.clone().add(0, 3, 0), LESBIAN_COLORS, 1.6f);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 0.75f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 1.1f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.7f, 0.5f);
		world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.PLAYERS, 1.0f, 0.0f);

	}

	@Override
	public void onCageCreation(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 2.0f, 0.5f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.1f);
	}

	@Override
	public void onCagedMob(Player player, World world, Location loc, LivingEntity mob) {
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.75f, 0.9f);
	}

	@Override
	public void cageTick(Player player, Location loc, double radius, int ticks) {
		if (ticks % 10 == 0) {
			new PPSpiral(Particle.DUST_COLOR_TRANSITION, loc, radius)
				.delta(0.1)
				.data(switch (ticks % 20) {
					case 0 -> new Particle.DustTransition(LESBIAN_RED, LESBIAN_WHITE, 1.3f);
					case 10 -> new Particle.DustTransition(LESBIAN_WHITE, LESBIAN_HOT_PINK, 1.3f);
					default -> throw new IllegalStateException("Unexpected value: " + ticks % 20);
				})
				.countPerBlockPerCurve(2)
				.curveAngle(70)
				.curves(10)
				.ticks(10)
				.spawnAsPlayerActive(player);

			new PPSpiral(Particle.DUST_COLOR_TRANSITION, loc, radius)
				.delta(0.1)
				.data(switch (ticks % 20) {
					case 0 -> new Particle.DustTransition(LESBIAN_RED, LESBIAN_WHITE, 1.3f);
					case 10 -> new Particle.DustTransition(LESBIAN_WHITE, LESBIAN_HOT_PINK, 1.3f);
					default -> throw new IllegalStateException("Unexpected value: " + ticks % 20);
				})
				.countPerBlockPerCurve(2)
				.curveAngle(-70)
				.curves(10)
				.ticks(10)
				.spawnAsPlayerActive(player);
		}
		Location center = loc.clone();
		for (Color color : LESBIAN_COLORS) {
			new PPCircle(Particle.REDSTONE, center, radius)
				.countPerMeter(0.25)
				.delta(0, 0.2, 0)
				.data(new Particle.DustOptions(color, 1.2f))
				.spawnAsPlayerActive(player);
			center.add(0, 0.8, 0);
		}
	}

	@Override
	public void cleaveReadyTick(Player player) {
		Location rightHand = PlayerUtils.getRightSide(player.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(player.getEyeLocation(), -0.45).subtract(0, .8, 0);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, leftHand)
			.data(new Particle.DustTransition(LESBIAN_RED, LESBIAN_WHITE, 1.2f))
			.spawnAsPlayerPassive(player);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, rightHand)
			.data(new Particle.DustTransition(LESBIAN_WHITE, LESBIAN_HOT_PINK, 1.2f))
			.spawnAsPlayerPassive(player);
	}

	@Override
	public void onCleaveHit(Player player, LivingEntity mob, double radius) {
		Location loc = LocationUtils.getHalfHeightLocation(mob);
		loc.setPitch(0);
		for (Color color : LESBIAN_COLORS) {
			ParticleUtils.launchOrb(new Vector(0, 0.25, 0), loc.clone().add(VectorUtils.randomUnitVector().multiply(radius)),
				player, mob, 5 * 20, loc, new Particle.DustOptions(color, 1.84f), entity -> {
				});
		}
		World world = player.getWorld();
		world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.PLAYERS, 1.0f, 0.0f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f, 0.75f);
	}
}
