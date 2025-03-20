package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class SpellFeatherBomb extends Spell {
	public static final int BASE_COOLDOWN = 6 * 20;
	public static final int COOLDOWN_INCLUDING_NEST = 12 * 20;
	public static final int LAND_DURATION = (int) (4.5 * 20);
	public static final int BOMB_DURATION = 2 * 20;
	public static final Color[] STEEL_COLORS = {Color.fromRGB(56, 56, 61), Color.fromRGB(120, 130, 130), Color.fromRGB(82, 85, 96)};

	private final Plugin mPlugin;
	private static final int RADIUS = 5;
	private static final int DAMAGE = 95;

	public final LivingEntity mBoss;
	private final SteelWingHawk mHawk;
	private final PassivePhantomControl mPhantomControl;
	private boolean mOnCooldown = false;
	private boolean mWillNest = false; // Tracking for cooldown purposes

	public SpellFeatherBomb(Plugin plugin, LivingEntity boss, SteelWingHawk hawk, PassivePhantomControl phantomControl) {
		mPlugin = plugin;
		mBoss = boss;
		mHawk = hawk;
		mPhantomControl = phantomControl;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		mHawk.mFeathers = Math.max(mHawk.mFeathers - 5, 0);
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, cooldownTicks() + 20);
		bomb();
		mWillNest = mHawk.mFeathers <= 10;
		if (mWillNest) {
			nest();
		}
	}

	private void bomb() {
		List<Location> bombLocations = new ArrayList<>();
		List<Location> projLocations = new ArrayList<>();
		List<Double> projVelocity = new ArrayList<>();
		for (Player p : PlayerUtils.playersInRange(mHawk.mSpawnLoc, SteelWingHawk.OUTER_RADIUS, false)) {
			Location bombLocation = LocationUtils.fallToGround(p.getLocation(), 0);
			bombLocations.add(bombLocation.add(0, 1, 0));
			Location projLoc = mBoss.getLocation().clone();
			projLocations.add(projLoc);
			projVelocity.add(projLoc.distance(bombLocation) / (float) BOMB_DURATION);
		}
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}
				//circles and particles.
				if (mTicks <= BOMB_DURATION) {
					for (int i = 0; i < bombLocations.size(); i++) {
						if (mTicks <= BOMB_DURATION - 7) {
							new PPCircle(Particle.CLOUD, bombLocations.get(i), RADIUS).count(30).ringMode(true).spawnAsBoss();
						}
						Vector dir = LocationUtils.getDirectionTo(bombLocations.get(i), projLocations.get(i)).multiply(projVelocity.get(i));
						projLocations.set(i, projLocations.get(i).add(dir));
						new PartialParticle(Particle.REDSTONE, projLocations.get(i), 5).delta(0.25).extra(0.05)
							.data(getRandomSteelColor()).spawnAsBoss();
						new PPLine(Particle.REDSTONE, projLocations.get(i), bombLocations.get(i)).count(10).data(getSteelColor(0)).spawnAsBoss();
					}
				}

				//cancel and damage tick
				if (mTicks >= BOMB_DURATION) {
					for (Location location : bombLocations) {
						dealDamage(location);
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void nest() {
		int playerCount = mHawk.getPlayers().size();
		mHawk.mFeathers += 10 + FastUtils.randomIntInRange((int) (4 * Math.sqrt(playerCount)), (int) (4 * Math.pow(playerCount, 0.6)));

		mPhantomControl.setNextPoint(mHawk.mSpawnLoc, mBoss.getLocation().distance(mHawk.mSpawnLoc) / 20, 10, LAND_DURATION + BOMB_DURATION, true);

		new BukkitRunnable() {
			int mTicks = 0;
			final Vector mVecToNest = LocationUtils.getDirectionTo(mHawk.mSpawnLoc, mBoss.getLocation());
			final float mYawNest = (float) Math.toDegrees(Math.atan2(-mVecToNest.getX(), mVecToNest.getZ()));
			final ChargeUpManager mCharge = new ChargeUpManager(mBoss, LAND_DURATION + BOMB_DURATION - 30, Component.text("Collecting Feathers", NamedTextColor.BLUE), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, SteelWingHawk.OUTER_RADIUS * 2);

			@Override
			public void run() {
				if (!mBoss.isValid()) {
					this.cancel();
					return;
				}

				//diving
				if (mTicks >= 10 && mTicks < 30) {
					//start landing
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 1.5f, 1f);
					if (mTicks == 29) {
						mBoss.setRotation(mYawNest, 0);
						mPhantomControl.freeze(BOMB_DURATION + LAND_DURATION - 30);
						EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
					}
				} else if (mTicks >= 30 && mCharge.nextTick()) {
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public static Particle.DustOptions getRandomSteelColor() {
		return getSteelColor(FastUtils.randomIntInRange(0, STEEL_COLORS.length - 1));
	}

	public static Particle.DustOptions getSteelColor(int i) {
		return new Particle.DustOptions(STEEL_COLORS[i], 1.5f);
	}

	@Override
	public int cooldownTicks() {
		return mWillNest ? COOLDOWN_INCLUDING_NEST : BASE_COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void dealDamage(Location loc) {
		for (Player p : PlayerUtils.playersInRange(loc, RADIUS, true)) {
			DamageUtils.damage(null, p, DamageEvent.DamageType.BLAST, DAMAGE);
		}
		for (int i = 0; i < 5; i++) {
			new PPExplosion(Particle.REDSTONE, loc).delta(RADIUS).data(getRandomSteelColor()).count(50).spawnAsBoss();
		}
		mBoss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);
	}
}
