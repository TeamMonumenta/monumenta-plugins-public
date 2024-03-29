package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.Samwell;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellCrystalBarrage extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Samwell mSamwell;

	private final Location mCenter;
	private final int mPhase;

	private static final int NUM_BULLETS = 60;
	private final double ARMOR_STAND_HEAD_OFFSET = 1.6875;
	private static final double DIRECT_HIT_DAMAGE = 60;
	private static final String SPELL_NAME = "Crystal Barrage";
	private static final int BULLET_DURATION = 5 * 20;
	private static final Material BULLET_MATERIAL = Material.AMETHYST_BLOCK;
	private static final double HITBOX = 0.3125;
	private final String BULLET_TAG = "samwell_bullet";

	private final PartialParticle mPHit;
	private final List<Player> mHitPlayers;
	private boolean mCooldown;
	private final ChargeUpManager mChargeUp;

	// CrystalBarrage: Bullet Hell pattern which places crystals surrounding the arena, and send
	// it flying to the middle and THROUGH the middle, forcing players to jump twice!
	// Deals 80 damage on first hit, subsequent hits (for same cast) is halved.
	public SpellCrystalBarrage(Plugin plugin, LivingEntity boss, Samwell samwell, int phase) {
		mPlugin = plugin;
		mBoss = boss;
		mSamwell = samwell;
		mCenter = mSamwell.mSpawnLoc;
		mPhase = phase;
		mHitPlayers = new ArrayList<>();
		mChargeUp = new ChargeUpManager(mBoss, castTime(), chargeUpTitle("Charging"),
			BossBar.Color.PINK, BossBar.Overlay.NOTCHED_10, 100);

		mPHit = new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 20, 0.25, 0.25, 0.25, 0.25);
	}

	@Override
	public void run() {
		mCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, cooldownTicks() + 20);

		mCenter.getWorld().playSound(mCenter, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5, 1);
		mCenter.getWorld().playSound(mCenter, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 5, 1.4f);
		mHitPlayers.clear();

		BukkitRunnable charge = new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUp.nextTick(2)) {
					this.cancel();

					mChargeUp.setTitle(chargeUpTitle("Unleashing"));
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public synchronized void cancel() {
							super.cancel();
							mChargeUp.reset();
							mChargeUp.setTitle(chargeUpTitle("Charging"));
						}

						@Override
						public void run() {
							mChargeUp.setProgress(1 - ((double) mT / BULLET_DURATION));
							if (mT > BULLET_DURATION) {
								this.cancel();
							}
							mT++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}
			}
		};
		charge.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(charge);

		BukkitRunnable spawnBullets = new BukkitRunnable() {
			int mT = 0;
			int mBullets = 0;

			@Override
			public void run() {
				// Aim to spawn everything in 3 seconds, which suggests spawning 1 every tick.
				// If Phase 4, then spawn two bullets per tick.
				if (mPhase <= 3) {
					double r = 25;
					double angle = (360.0 / NUM_BULLETS) * mBullets;
					double radians = Math.toRadians(angle);
					Location loc = mCenter.clone().add(r * Math.cos(radians), 1.1875, r * Math.sin(radians));
					Vector dir = LocationUtils.getVectorTo(mCenter, loc).setY(0).normalize();
					int timeStart = castTime() - mT;
					launchAcceleratingBullet(loc, dir, timeStart);
					mBullets++;

					if (mT % 2 == 0) {
						loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.HOSTILE, 5, 1);
						loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.HOSTILE, 5, 0.5f + 1.5f * mT / castTime());
					}

				} else {
					double r = 25;
					double angle = (360.0 / NUM_BULLETS) * mBullets;
					double radians = Math.toRadians(angle);
					Location loc = mCenter.clone().add(r * Math.cos(radians), 1.1875, r * Math.sin(radians));
					Vector dir = LocationUtils.getVectorTo(mCenter, loc).setY(0).normalize();
					int timeStart = castTime() - mT;
					launchAcceleratingBullet(loc, dir, timeStart);
					mBullets++;

					angle = (360.0 / NUM_BULLETS) * mBullets;
					radians = Math.toRadians(angle);
					loc = mCenter.clone().add(r * Math.cos(radians), 1.1875, r * Math.sin(radians));
					dir = LocationUtils.getVectorTo(mCenter, loc).setY(0).normalize();
					timeStart = castTime() - mT;
					launchAcceleratingBullet(loc, dir, timeStart);
					mBullets++;

					loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.HOSTILE, 5, 1);
					loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.HOSTILE, 5, 0.5f + 1.5f * mT / castTime());
				}

				if (mBullets >= 60) {
					this.cancel();
				}

				mT++;
			}
		};
		spawnBullets.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(spawnBullets);
	}

	private void launchAcceleratingBullet(Location detLoc, Vector dir, int accelStart) {
		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, false);

		ArmorStand bullet = mBoss.getWorld().spawn(detLoc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0), ArmorStand.class);
		bullet.setVisible(false);
		bullet.setGravity(false);
		bullet.setMarker(true);
		bullet.setCollidable(false);
		bullet.getEquipment().setHelmet(new ItemStack(BULLET_MATERIAL));
		bullet.addScoreboardTag(BULLET_TAG);

		BukkitRunnable bulletRun = new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, HITBOX, HITBOX, HITBOX);
			int mTicks = 0;
			double mInnerVelocity = 0;

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				for (int j = 0; j < 2; j++) {
					mBox.shift(dir.clone().multiply(mInnerVelocity * 0.5));
					Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(mBox)) {
							directHit(player);
							MovementUtils.knockAway(loc, player, 1f, 0.5f);
							bullet.remove();
							this.cancel();
							return;
						}
					}
				}
				Location loc = mBox.getCenter().toLocation(mBoss.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0));
				if (mTicks >= BULLET_DURATION + accelStart || mBoss.isDead() || mPhase != mSamwell.mPhase) {
					bullet.remove();
					this.cancel();
				}
				if (mTicks >= accelStart && mInnerVelocity < 0.5) {
					mInnerVelocity += 0.05;
				}
			}
		};
		bulletRun.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(bulletRun);
	}


	@Override
	public int cooldownTicks() {
		if (mPhase <= 3) {
			return 10 * 20;
		} else {
			return 5 * 20;
		}
	}

	public int castTime() {
		if (mPhase <= 3) {
			return 70;
		} else {
			return 35;
		}
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	private void directHit(Player player) {
		mPHit.location(player.getLocation().add(0, 1, 0)).spawnAsBoss();
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 2);

		if (!mHitPlayers.contains(player)) {
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DIRECT_HIT_DAMAGE, null, false, false, SPELL_NAME);
			BossUtils.bossDamagePercent(mBoss, player, 0.1, SPELL_NAME);
			mHitPlayers.add(player);
		} else {
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, 40, null, false, false, SPELL_NAME);
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		mChargeUp.reset();

		// Remove already spawned crystal barrage projectiles
		mCenter.getNearbyEntitiesByType(ArmorStand.class, 100)
			.stream()
			.filter(e -> ScoreboardUtils.checkTag(e, BULLET_TAG))
			.forEach(Entity::remove);
	}

	private static Component chargeUpTitle(String firstWord) {
		return Component.text(firstWord + " ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
	}
}
