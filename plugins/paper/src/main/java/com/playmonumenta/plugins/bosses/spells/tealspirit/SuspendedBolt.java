package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SuspendedBolt extends Spell {

	private static final String ABILITY_NAME = "Suspended Bolt";
	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final int mRange;
	private final int mCastCount;
	private final int mDamage;
	private final int mCastTime;
	private final int mExecutionTime;
	private final int mCooldown;
	private final int mRadius;
	private final ChargeUpManager mChargeUp;
	private final Location mSpawnLoc;

	public SuspendedBolt(LivingEntity boss, Plugin plugin, int range, int castCount, int damage, int castTime, int executionTime, int cooldown, Location spawnLoc, int radius) {
		this.mBoss = boss;
		this.mPlugin = plugin;
		this.mRange = range;
		this.mCastCount = castCount;
		this.mDamage = damage;
		this.mCastTime = castTime;
		this.mExecutionTime = executionTime;
		this.mCooldown = cooldown;
		this.mSpawnLoc = spawnLoc;
		this.mRadius = radius;
		mChargeUp = new ChargeUpManager(mBoss, mCastTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)),
			BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange);
	}

	@Override
	public void run() {
		mChargeUp.setTime(0);
		mChargeUp.setTitle(Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)));

		// Cast Runnable
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				// Things to run during cast
				int castInterval = mCastTime / mCastCount;
				if (mChargeUp.getTime() % castInterval == 0) {
					List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, mRange, true);
					players.forEach(p -> {
						executeBlast(p.getLocation());
					});
				}

				if (mChargeUp.nextTick()) {
					// Things to run at end of charge

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void executeBlast(Location targetLoc) {
		World world = targetLoc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.2f);
		Location shotLoc = mBoss.getEyeLocation();
		new PPLine(Particle.WAX_OFF, shotLoc, targetLoc)
			.delta(0.4, 0.4, 0.4)
			.countPerMeter(5)
			.spawnAsBoss();
		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT % 2 == 0) {
					world.playSound(shotLoc, Sound.ENTITY_CAT_HISS, SoundCategory.HOSTILE, 0.1f, 1.5f);
				}
				new PartialParticle(Particle.WAX_OFF, shotLoc, 5, 0.2, 0.2, 0.2, 0.05).spawnAsEntityActive(mBoss);
				new PPCircle(Particle.SMOKE_LARGE, targetLoc, mRadius)
					.count(7)
					.spawnAsBoss();
				new PPCircle(Particle.REDSTONE, targetLoc, mRadius - 0.02)
					.count(7)
					.data(new Particle.DustOptions(Color.fromRGB(252, 3, 3), 1.65f))
					.spawnAsBoss();
				if (mT >= mExecutionTime) {
					world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 2.0f, 0.5f);
					new PPLine(Particle.WAX_OFF, shotLoc, targetLoc)
						.delta(0.4, 0.4, 0.4)
						.countPerMeter(5)
						.spawnAsBoss();
					new PPExplosion(Particle.FLAME, targetLoc)
						.speed(1)
						.count(120)
						.extraRange(0.15, 1)
						.spawnAsBoss();
					PlayerUtils.playersInRange(targetLoc, mRadius, true).forEach(p -> {
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, mDamage, null, false, true, ABILITY_NAME);
					});
					this.cancel();
				}
				mT += 1;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCastTime + mExecutionTime + mCooldown;
	}
}
