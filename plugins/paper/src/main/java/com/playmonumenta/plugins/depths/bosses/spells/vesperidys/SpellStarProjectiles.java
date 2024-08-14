package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellStarProjectiles extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;

	private static final int INIT_TICKS = 20;
	private static final int VOLLEYS = 3;
	private static final int VOLLEYS_A4 = 4;
	private static final int VOLLEYS_A15 = 5;
	private static final double DAMAGE = 60;

	private final int mTelegraphWaitTicks;
	private final int mCycleTicks;

	private final int mVolleyTotal;

	private boolean mOnCooldown = false;

	public SpellStarProjectiles(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 15) {
			mTelegraphWaitTicks = 10;
			mVolleyTotal = VOLLEYS_A15;
		} else if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 4) {
			mTelegraphWaitTicks = 15;
			mVolleyTotal = VOLLEYS_A4;
		} else {
			mTelegraphWaitTicks = 20;
			mVolleyTotal = VOLLEYS;
		}
		mCycleTicks = mTelegraphWaitTicks + 5;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + 20);

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WOLF_GROWL, 5, 1);
		GlowingManager.ActiveGlowingEffect glowingEffect = GlowingManager.startGlowing(mBoss, NamedTextColor.DARK_RED, -1, GlowingManager.BOSS_SPELL_PRIORITY);

		BukkitRunnable runnableA = new BukkitRunnable() {
			private int mVolley = 0;
			private int mT = -INIT_TICKS;

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				glowingEffect.clear();
			}

			@Override
			public void run() {
				if (mVolley >= mVolleyTotal) {
					this.cancel();
					return;
				}

				if (mT == mTelegraphWaitTicks) {
					shootStarProjectiles();
				} else if (mT == 0) {
					mVesperidys.mTeleportSpell.teleportRandom();
					mT += 1;
				}

				if (mT >= mCycleTicks) {
					mT = 0;
					mVolley += 1;
				} else if (!mVesperidys.mTeleportSpell.mTeleporting) {
					mT += 1;
				}
			}
		};
		runnableA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableA);

	}

	private void shootStarProjectiles() {
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 5, 1.5f);

		for (double theta = 0; theta < 360; theta += 22.5) {
			Location origin = mBoss.getLocation().add(0, 1.5, 0);
			double x = Math.cos(theta);
			double z = Math.sin(theta);

			double projSpeed = 0.3;

			BukkitRunnable projRunnable = new BukkitRunnable() {
				final Location mProjLoc = origin.clone();
				int mProjTicks = 0;

				@Override
				public void run() {
					if (mProjTicks > 200 || mProjLoc.distance(mVesperidys.mSpawnLoc) > 25) {
						this.cancel();
						return;
					}

					mProjTicks += 1;

					Vector dir = new Vector(x, 0, z).normalize();
					mProjLoc.add(dir.multiply(projSpeed));

					new PartialParticle(Particle.END_ROD, mProjLoc, 1, 0, 0, 0, 0)
						.extra(10000000)
						.spawnAsBoss();
					new PartialParticle(Particle.REDSTONE, mProjLoc, 1)
						.data(new Particle.DustOptions(Color.fromRGB(255, 0, 255), 1f))
						.spawnAsBoss();

					BoundingBox box = BoundingBox.of(mProjLoc, 0.25, 0.25, 0.25);

					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
						if (box.overlaps(player.getBoundingBox())) {
							BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.PROJECTILE, DAMAGE, "Pointed Stars", origin);
							new PartialParticle(Particle.CRIT_MAGIC, player.getLocation(), 25, 0.5, 1, 0.5, 0).spawnAsBoss();
							mBoss.getWorld().playSound(mProjLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
							this.cancel();
						}
					}

					if (mProjLoc.getBlock().isSolid()) {
						mBoss.getWorld().playSound(mProjLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 2);
						this.cancel();
					}
				}
			};

			projRunnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(projRunnable);
		}
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mVesperidys.mTeleportSpell.mTeleporting;
	}

	@Override
	public int cooldownTicks() {
		return mVesperidys.mSpellCooldowns;
	}
}
