package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class DoomsdayClock extends Spell {
	private static final double RADIUS = 11;
	private static final double WALL_HEIGHT = 10;
	private static final double BLACK_DAMAGE = 180;
	private static final double RED_DAMAGE = 100;
	private static final double BLACK_DEGREE = 2.25;
	private static final double RED_DEGREE = -3;
	private static final Color BLACK_COLOR = Color.fromRGB(0, 0, 0);
	private static final Color RED_COLOR = Color.fromRGB(255, 0, 0);
	private static final int PERIOD = 2;
	private static final int GROWTH_TIME = 5 * 20;
	private static final int MAX_TIME = 15 * 20;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;
	private final TealSpirit mTealSpirit;

	public DoomsdayClock(LivingEntity boss, Location center, int cooldownTicks, TealSpirit tealSpirit) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mTealSpirit = tealSpirit;
	}

	@Override
	public void run() {
		mTealSpirit.setInterspellCooldown(GROWTH_TIME + MAX_TIME + 3 * 20);

		World world = mBoss.getWorld();

		PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "let your doom be made manifest!"));

		double blackStartDeg = (int) FastUtils.randomDoubleInRange(0, 360);
		double redStartDeg = (int) FastUtils.randomDoubleInRange(0, 360);

		Plugin plugin = Plugin.getInstance();
		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = -GROWTH_TIME;
			double mBlackDeg = blackStartDeg;
			double mRedDeg = redStartDeg;

			@Override
			public void run() {
				double length;
				double wallHeight;
				List<Player> players = PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true);
				if (mT < 0) {
					double ratio = (GROWTH_TIME + mT) / (double) GROWTH_TIME;
					length = ratio * RADIUS;
					wallHeight = ratio * WALL_HEIGHT;

					if (mT % 4 == 0) {
						players.forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 2.0f, (float) (0.1 + ratio * 0.3)));
					}
				} else {
					length = RADIUS;
					wallHeight = WALL_HEIGHT;
				}

				List<BoundingBox> blackBoxes = createHand(mBlackDeg, length, BLACK_COLOR);
				List<BoundingBox> redBoxes = createHand(mRedDeg, length, RED_COLOR);

				for (int deg = 0; deg < 360; deg += 15) {
					Location base = mCenter.clone().add(FastUtils.cosDeg(deg) * RADIUS, 0.5, FastUtils.sinDeg(deg) * RADIUS);
					for (int y = 0; y < wallHeight; y++) {
						world.spawnParticle(Particle.CRIT, base.clone().add(0, y, 0), 1, 0.1, 0.1, 0.1, 0);
					}
				}

				if (mT == 0) {
					world.playSound(mCenter, Sound.BLOCK_ANVIL_PLACE, 1.5f, 1.4f);
				}

				if (mT > 0 && mT % 10 == 0) {
					for (Player player : players) {
						for (BoundingBox box : blackBoxes) {
							if (box.overlaps(player.getBoundingBox())) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, BLACK_DAMAGE, null, false, false, "Doomsday Clock");
							}
						}

						for (BoundingBox box : redBoxes) {
							if (box.overlaps(player.getBoundingBox()) && player.isOnGround()) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, RED_DAMAGE, null, false, false, "Doomsday Clock");
							}
						}

						if (LocationUtils.xzDistance(player.getLocation(), mCenter) > RADIUS) {
							BossUtils.bossDamagePercent(mBoss, player, mT > 5 * 20 ? 0.15 : 0.05, null, true, "Doomsday Clock");
						}

						player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 2f, 0.4f);
					}
				}

				if (mT >= MAX_TIME) {
					this.cancel();
					return;
				}

				if (mT > 0) {
					mBlackDeg += BLACK_DEGREE;
					mRedDeg += RED_DEGREE;
				}
				mT += PERIOD;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(plugin, 0, PERIOD);
	}

	@Override
	public boolean canRun() {
		return !mTealSpirit.isInterspellCooldown();
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	private List<BoundingBox> createHand(double deg, double length, Color color) {
		List<BoundingBox> boxes = new ArrayList<>();

		double radian = Math.toRadians(deg);
		for (double r = 0; r < length; r += 0.75) {
			Vector vec = new Vector(FastUtils.cos(radian) * r, 0, FastUtils.sin(radian) * r);
			vec = VectorUtils.rotateYAxis(vec, deg);
			Location l = mCenter.clone().add(vec);

			int tries = 0;
			while (l.getBlock().isSolid() && tries < 4) {
				l.add(0, 1, 0);
				tries++;
			}

			BoundingBox box = BoundingBox.of(l, 0.8, 50, 0.8);
			boxes.add(box);
			mCenter.getWorld().spawnParticle(Particle.REDSTONE, l, 1, new Particle.DustOptions(color, 2));
			if (color.equals(BLACK_COLOR)) {
				for (int i = 1; i <= 3; i++) {
					mCenter.getWorld().spawnParticle(Particle.REDSTONE, l.clone().add(0, i, 0), 1, new Particle.DustOptions(color, 1));
				}
			}
		}

		return boxes;
	}
}
