package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
	private static final String SPELL_NAME = "Volcanic Demise";
	private static final int DAMAGE = 42;
	private static final int METEOR_GROUPS_PER_SPELL = 25;
	private static final int TICKS_PER_METEOR_GROUP = 10;
	private static final double DEATH_RADIUS = 2;
	private static final double HIT_RADIUS = 5;
	private static final int SPELL_LENGTH_TICKS = TICKS_PER_METEOR_GROUP * METEOR_GROUPS_PER_SPELL;
	private static final double SPELL_LENGTH_TICKS_RECIPROCAL = 1.0 / SPELL_LENGTH_TICKS;
	private static final int DETECTION_RANGE = 50;
	private static final double MAX_PLAYER_Y = 61.0;
	private static final float PITCH_TICK_SCALE_FACTOR = 1.0f / 25;

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

		mChargeUp = new ChargeUpManager(mBoss, 20 * 2, Component.text("Charging ", NamedTextColor.GREEN)
			.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED, TextDecoration.BOLD)),
			BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, 60);
	}

	class ChargeUpThenCastRunnable extends BukkitRunnable {
		@Override
		public void run() {
			World world = mBoss.getWorld();

			float currTick = mChargeUp.getTime();
			float pitchIncrease = currTick * PITCH_TICK_SCALE_FACTOR;

			new PartialParticle(Particle.LAVA, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005)
				.spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125)
				.spawnAsEntityActive(mBoss);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.5f + pitchIncrease);

			// Prepare to cast
			if (mChargeUp.nextTick(2)) {
				this.cancel();
				mActiveRunnables.remove(this);

				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 0.5f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1, 0.7f);

				mChargeUp.setTitle(Component.text("Unleashing ", NamedTextColor.GREEN)
					.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED, TextDecoration.BOLD)));

				BukkitRunnable castRunnable = new CastRunnable();
				castRunnable.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(castRunnable);
			}
		}
	}

	class CastRunnable extends BukkitRunnable {
		// If cancelled, return to charging
		@Override
		public synchronized void cancel() {
			super.cancel();

			mChargeUp.reset();
			mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.GREEN)
				.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED, TextDecoration.BOLD)));
		}

		int mTickCount = 0;
		int mMeteorGroupsSummoned = 0;

		@Override
		public void run() {
			mTickCount++;
			mChargeUp.setProgress(1 - mTickCount * SPELL_LENGTH_TICKS_RECIPROCAL);

			if (mTickCount % TICKS_PER_METEOR_GROUP == 0) {
				mMeteorGroupsSummoned++;

				List<Player> players = getValidTargetPlayers();

				// Punish players on outer edges/in water; 10tick meteors
				players.stream().map(player -> player.getLocation())
					.filter(loc -> loc.getBlock().isLiquid() || loc.distanceSquared(mCenter) > 42 * 42)
					.forEach((loc) -> {
						loc.setY(mCenter.getY());
						rainMeteor(loc, 10);
					});


				for (int j = 0; j < 4; j++) {
					rainMeteor(horizontalRandomShift(mCenter.clone(), mRange), 40);
				}

				// Target one random player. Have a meteor rain nearby them.
				if (!players.isEmpty()) {
					Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
					Location loc = rPlayer.getLocation();
					loc.setY(mCenter.getY());
					rainMeteor(horizontalRandomShift(loc, 8), 40);
				}

				if (mMeteorGroupsSummoned >= METEOR_GROUPS_PER_SPELL) {
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}

		}
	}

	class RainMeteorRunnable extends BukkitRunnable {
		double mCurrentRelY;
		static final int TICKS_PER_EMANATING_CIRCLE = 8;
		final Location mLoc;
		final World mWorld;

		public RainMeteorRunnable(Location locInput, double spawnRelY) {
			mCurrentRelY = spawnRelY;
			mLoc = locInput.clone();
			mWorld = locInput.getWorld();
		}

		@Override
		public void run() {
			mCurrentRelY -= 1;

			// Particles: Emanating Circles
			if (mCurrentRelY % TICKS_PER_EMANATING_CIRCLE == 0) {
				new PPCircle(Particle.FLAME, mLoc.clone().add(0, 0.2, 0), DEATH_RADIUS)
					// RotateDelta originates from positive X
					.delta(1, 0, 0).rotateDelta(true)
					// 1 particle per 2 degrees; 90 particles per pi radians.
					// 1 radian per radius meters circumference
					// 90/(pi * radius) particles per meter
					.countPerMeter(90.0 / (Math.PI * DEATH_RADIUS)).extra(0.15).directionalMode(true).distanceFalloff(15)
					.spawnAsBoss();

				new PPCircle(Particle.REDSTONE, mLoc.clone().add(0, 0.2, 0), 2)
					// 1 particle per 6 degrees; 30 particles per pi radians.
					// 1 radian per radius meters circumference
					// 30/(pi * radius) particles per meter
					.countPerMeter(30.0 / (Math.PI * 2)).extra(0)
					.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1)).distanceFalloff(15)
					.spawnAsBoss();
			}
			new PartialParticle(Particle.LAVA, mLoc, 3, 2.5, 0, 2.5, 0.05).distanceFalloff(20)
				.spawnAsBoss();

			// Particles: Meteor Trail
			Location meteorTrailLoc = mLoc.clone().add(0, mCurrentRelY, 0);
			new PartialParticle(Particle.FLAME, meteorTrailLoc, 10, 0.2f, 0.2f, 0.2f, 0.1)
				.distanceFalloff(20).spawnAsBoss();
			new PartialParticle(Particle.SMOKE_LARGE, meteorTrailLoc, 5, 0, 0, 0, 0.05)
				.distanceFalloff(20).spawnAsBoss();
			mWorld.playSound(meteorTrailLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);

			// Impact
			if (mCurrentRelY <= 0) {
				this.cancel();
				mActiveRunnables.remove(this);

				// Particles: Impact
				new PartialParticle(Particle.FLAME, mLoc, 50, 0, 0, 0, 0.175)
					.distanceFalloff(20).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_LARGE, mLoc, 10, 0, 0, 0, 0.25)
					.distanceFalloff(20).spawnAsBoss();
				mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.9f);

				// Meteor should deal 1000 blast damage if you're in the 2 block death cylinder
				// If you're clipped by the edge it deals less
				Location mLowerLoc = mLoc.clone().add(0, -8, 0);
				// The hitbox should start 8 blocks below the ground, so it can hit players in the ground.
				Hitbox deathBox = new Hitbox.UprightCylinderHitbox(mLowerLoc, 15, DEATH_RADIUS);
				Hitbox hitBox = new Hitbox.UprightCylinderHitbox(mLowerLoc, 23, HIT_RADIUS);
				List<Player> hitPlayers = new ArrayList<>(hitBox.getHitPlayers(true));
				List<Player> deathPlayers = new ArrayList<>(deathBox.getHitPlayers(true));

				// Death Zone
				for (Player player : deathPlayers) {
					DamageUtils.damage(mBoss, player, DamageType.BLAST, 1000, null, true, true, SPELL_NAME);
					MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
					hitPlayers.remove(player);
				}

				// Edge Zone
				for (Player player : hitPlayers) {
					boolean didDamage = BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, DAMAGE, SPELL_NAME, mLoc);
					if (didDamage) {
						MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
					}
				}

				for (Block block : LocationUtils.getNearbyBlocks(mLoc.getBlock(), 4)) {
					if (FastUtils.RANDOM.nextInt(8) < 1) {
						Material blockType = block.getType();
						if (blockType == Material.SMOOTH_RED_SANDSTONE) {
							block.setType(Material.NETHERRACK);
						} else if (blockType == Material.NETHERRACK) {
							block.setType(Material.MAGMA_BLOCK);
						} else if (blockType == Material.SMOOTH_SANDSTONE) {
							block.setType(Material.SMOOTH_RED_SANDSTONE);
						}
					}
				}
			}
		}

		@Override
		public synchronized void cancel() {
			super.cancel();
			if (mCurrentRelY > 0) {
				MMLog.warning(() -> "[Kaul] A Volcanic Demise cast was cancelled early!");
			}
		}
	}

	private List<Player> getValidTargetPlayers() {
		List<Player> result = PlayerUtils.playersInRange(mCenter, DETECTION_RANGE, true);
		result.removeIf(player -> player.getLocation().getY() >= MAX_PLAYER_Y);
		return result;
	}

	private Location horizontalRandomShift(Location loc, double maxOffset) {
		return loc.add(
			FastUtils.randomDoubleInRange(-maxOffset, maxOffset),
			0,
			FastUtils.randomDoubleInRange(-maxOffset, maxOffset)
		);
	}

	@Override
	public void run() {
		for (Player player : getValidTargetPlayers()) {
			player.sendMessage(Component.text("SCATTER, INSECTS.", NamedTextColor.GREEN));
		}

		// For the advancement "Such Devastation"
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("function monumenta:kaul/volcanic_demise_count");

		BukkitRunnable chargeRunnable = new ChargeUpThenCastRunnable();
		chargeRunnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(chargeRunnable);
	}

	private void rainMeteor(Location locInput, double spawnRelY) {
		BukkitRunnable runnable = new RainMeteorRunnable(locInput, spawnRelY);
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
