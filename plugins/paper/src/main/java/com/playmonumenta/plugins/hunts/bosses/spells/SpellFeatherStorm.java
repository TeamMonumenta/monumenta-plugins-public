package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.hunts.bosses.spells.SpellFeatherBomb.getRandomSteelColor;

public class SpellFeatherStorm extends Spell {
	public static int COOLDOWN = 12 * 20;
	public static int DURATION = 8 * 20;
	public static int FORM_DURATION = 3 * 20;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SteelWingHawk mHawk;
	private static final double MIN_RADIUS = 3.5;
	private static final double MAX_RADIUS = 5.5;
	private static final double MIN_MOVE_SPEED = 0.25;
	private static final double MAX_MOVE_SPEED = 1.25;
	private static final float DAMAGE = 33;
	private static final Particle.DustOptions INDICATOR = new Particle.DustOptions(Color.fromBGR(10, 10, 10), 2.0f);

	public boolean mOnCooldown = false;


	public SpellFeatherStorm(Plugin plugin, LivingEntity boss, SteelWingHawk hawk) {
		mPlugin = plugin;
		mBoss = boss;
		mHawk = hawk;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, COOLDOWN + 20);
		mHawk.mFeathers -= 6;
		List<Player> players = PlayerUtils.playersInRange(mHawk.mSpawnLoc, SteelWingHawk.OUTER_RADIUS, true, false);
		Collections.shuffle(players);
		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, FORM_DURATION, Component.text("Charging Feather Storm", NamedTextColor.BLUE), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_6, SteelWingHawk.OUTER_RADIUS * 2);
		Location mStormLoc = LocationUtils.fallToGround(players.get(0).getLocation(), 0);
		new BukkitRunnable() {
			final List<Location> mLocations = new ArrayList<>();

			int mPos = 0;
			int mTicks = 0;
			int mDamageStack = 0;

			@Override
			public void run() {
				double targetDistance = players.get(mPos).getLocation().distance(mStormLoc);
				//choosing target
				int counter = 0;
				while (players.get(mPos).isDead() || targetDistance > 200) {
					int newPos = (mPos + 1) % players.size();
					if (newPos != mPos) {
						mDamageStack = 0;
					}
					mPos = newPos;

					targetDistance = players.get(mPos).getLocation().distance(mStormLoc);
					counter++;
					if (counter > players.size()) {
						// No valid targets
						this.cancel();
						break;
					}
				}

				if (mTicks % 5 == 4) {
					mLocations.add(players.get(mPos).getLocation());
				}

				if (mTicks <= FORM_DURATION) {
					//spawn animation
					new PPCircle(Particle.REDSTONE, mStormLoc, MIN_RADIUS).data(INDICATOR).countPerMeter(0.9).delta(0.25).ringMode(true).spawnAsBoss();
					chargeUp.nextTick();
				} else if (mTicks % 5 == 0) {
					double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * ((double) mTicks - FORM_DURATION) / DURATION;
					double maxSpeed = Math.max(MAX_MOVE_SPEED + Math.pow(1.04, targetDistance) - 1 - mDamageStack * 0.25, MIN_MOVE_SPEED);

					double dist = 0;
					while (dist < maxSpeed * 0.85 && !mLocations.isEmpty()) {
						Location loc = mLocations.get(0);
						Vector vec = LocationUtils.getVectorTo(LocationUtils.fallToGround(loc, loc.getY() - 10), mStormLoc);
						if (vec.lengthSquared() > maxSpeed * maxSpeed) {
							vec = vec.normalize().multiply(maxSpeed);
						} else {
							mLocations.remove(0);
						}
						mStormLoc.add(vec);
						dist += vec.length();
					}

					new PPCircle(Particle.REDSTONE, mStormLoc, radius).data(INDICATOR).countPerMeter(1.6).ringMode(true).spawnAsBoss();
					if (mTicks % 10 == 0) {
						List<Player> playersInRange = PlayerUtils.playersInRange(mStormLoc, radius, true, false);
						if (playersInRange.contains(players.get(mPos))) {
							mDamageStack++;
						} else if (targetDistance >= radius + 2 && mDamageStack > 0) {
							mDamageStack--;
						}
						for (Player p : playersInRange) {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.PROJECTILE, DAMAGE, null, false, true, "Feather Storm");
						}
					}
				}

				if (mTicks >= FORM_DURATION) {
					if (mTicks == FORM_DURATION) {
						chargeUp.setChargeTime(DURATION);
						chargeUp.setTime(DURATION);
						chargeUp.setTitle(Component.text("Channeling Feather Storm", NamedTextColor.BLUE));
						chargeUp.update();
					} else {
						chargeUp.previousTick();
					}
				}
				if (mTicks > FORM_DURATION && mTicks % 2 == 0) {
					for (int i = 0; i < 5; i++) {
						cosmeticBullet(mStormLoc);
					}
				}
				if (mTicks > (DURATION + FORM_DURATION) || mBoss.isDead()) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && mHawk.mFeathers >= 6;
	}

	private void cosmeticBullet(Location stormLoc) {
		float x = FastUtils.randomIntInRange(-4, 4);
		float z = FastUtils.randomIntInRange(-4, 4);
		Location bullet = stormLoc.clone().add(x, 10, z);
		Vector vec = LocationUtils.getDirectionTo(new Location(mBoss.getWorld(), stormLoc.getX() + x, stormLoc.getY(), stormLoc.getZ() + z), bullet);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.REDSTONE, bullet, 3).delta(0.125)
					.data(getRandomSteelColor()).spawnAsBoss();
				bullet.add(vec);
				if (bullet.getBlock().isSolid() || mTicks > 30) {
					//want to give the feel of a storm approaching
					bullet.getWorld().playSound(bullet, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 1f, 1.5f);
					this.cancel();
				}
				mTicks++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}
