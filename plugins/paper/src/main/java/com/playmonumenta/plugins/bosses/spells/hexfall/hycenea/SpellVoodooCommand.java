package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellVoodooCommand extends Spell {

	private static final String ABILITY_NAME = "Voodoo Command";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mDamage;
	private final int mCastTime;
	private final int mCooldown;
	private final double mMinRad;
	private final double mMaxRad;
	private boolean mTriggered;

	private int mT = 0;

	public SpellVoodooCommand(Plugin plugin, LivingEntity boss, int damage, int castTime, int cooldown, double minRad, double maxRad) {
		mPlugin = plugin;
		mBoss = boss;
		mDamage = damage;
		mCastTime = castTime;
		mCooldown = cooldown;
		mMinRad = minRad;
		mMaxRad = maxRad;
		mTriggered = false;
	}

	@Override
	public void run() {

		if (mT % 2 == 0 && mT > (mCastTime / 2)) {
			new PPCircle(Particle.REDSTONE, mBoss.getLocation(), mMaxRad)
				.count(5)
				.delta(0.1, 0.05, 0.1)
				.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.65f))
				.spawnAsBoss();
			if (mMinRad > 0) {
				new PPCircle(Particle.REDSTONE, mBoss.getLocation(), mMinRad)
					.count(5)
					.delta(0.1, 0.05, 0.1)
					.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.65f))
					.spawnAsBoss();
			}
			for (double rad = mMinRad; rad < mMaxRad; rad += 0.5) {
				new PPCircle(Particle.SOUL, mBoss.getLocation(), rad).ringMode(true).count(5).spawnAsBoss();
			}
		}

		if (mT >= mCastTime && !mTriggered) {
			mTriggered = true;

			List<Player> hitPlayers = HexfallUtils.playersInBossInXZRange(mBoss.getLocation(), mMaxRad, true);

			if (mMinRad > 0) {
				for (Player player : HexfallUtils.playersInBossInXZRange(mBoss.getLocation(), mMinRad, true)) {
					hitPlayers.remove(player);
				}
			}

			for (Player player : hitPlayers) {
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, mDamage, null, false, true, ABILITY_NAME);
				MovementUtils.knockAway(mBoss.getLocation(), player, 0f, 1f, false);
			}

			BukkitRunnable resolveRunnable = new BukkitRunnable() {
				int mT = 0;
				final int ANIM_TIME = 10;
				final int MAX_HEIGHT = 3;

				@Override
				public void run() {
					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
						return;
					}

					if (mT >= ANIM_TIME) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 1f, 1f);
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1f, 1f);
						mBoss.remove();
						this.cancel();
					}

					if (mT % 2 == 0) {
						if (mMinRad > 0) {
							new PPCircle(Particle.REDSTONE, mBoss.getLocation().add(0, MAX_HEIGHT - (mT / 2f), 0), mMinRad)
								.count(5)
								.delta(0.1, 0.05, 0.1)
								.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.65f))
								.spawnAsBoss();
							new PPCircle(Particle.SQUID_INK, mBoss.getLocation().add(0, MAX_HEIGHT - (mT / 2f), 0), mMinRad)
								.count(5)
								.delta(0.1, 0.05, 0.1)
								.spawnAsBoss();
						}
						new PPCircle(Particle.REDSTONE, mBoss.getLocation().add(0, MAX_HEIGHT - (mT / 2f), 0), mMaxRad)
							.count(30)
							.delta(0.1, 0.05, 0.1)
							.data(new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.65f))
							.spawnAsBoss();
						new PPCircle(Particle.SQUID_INK, mBoss.getLocation().add(0, MAX_HEIGHT - (mT / 2f), 0), mMaxRad)
							.count(30)
							.delta(0.1, 0.05, 0.1)
							.spawnAsBoss();
						PPCircle indicator2 = new PPCircle(Particle.REDSTONE, mBoss.getLocation(), 0)
							.ringMode(true)
							.count(2)
							.delta(0.25, 0.1, 0.25)
							.data(new Particle.DustOptions(Color.fromRGB(85, 85, 85), 1.65f));
						for (double r = mMinRad; r < mMaxRad; r++) {
							indicator2.radius(r).location(mBoss.getLocation()).spawnAsBoss();
						}
					}

					mT += 1;
				}
			};
			resolveRunnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(resolveRunnable);
		}

		mT++;
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
