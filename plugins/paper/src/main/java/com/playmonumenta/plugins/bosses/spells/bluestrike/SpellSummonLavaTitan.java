package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.LavaCannonBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSummonLavaTitan extends Spell {

	private Plugin mPlugin;
	private Location mCenter;
	private int mPhase;

	private List<Vector> mSummonSpots;
	private boolean mCooldown;
	private ChargeUpManager mChargeUp;

	public SpellSummonLavaTitan(Plugin plugin, LivingEntity boss, Location center, int phase) {
		mPlugin = plugin;
		mCenter = center;
		mPhase = phase;
		mChargeUp = new ChargeUpManager(boss, LavaCannonBoss.chargeTime(mPhase), ChatColor.GREEN + "Charging " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Lava Cannon...",
			BarColor.RED, BarStyle.SEGMENTED_10, 100);

		mSummonSpots = Arrays.asList(
			new Vector(-3.5, 2, 28),
			new Vector(3.5, 2, 28),
			new Vector(-10, 2, 28),
			new Vector(10, 2, 28),
			new Vector(-3.5, 2, -28),
			new Vector(3.5, 2, -28),
			new Vector(-10, 2, -28),
			new Vector(10, 2, -28)
		);
	}

	@Override public void run() {
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, cooldownTicks() + 6 * 20);

		new BukkitRunnable() {
			int mT = 0;

			@Override public void run() {
				if (mT % (cooldownTicks() / 10) == 0) {
					mCenter.getWorld().playSound(mCenter, Sound.BLOCK_BEEHIVE_DRIP, 20, 0.5f);
				}

				if (mT >= cooldownTicks() / 2) {
					summon();
					this.cancel();
				}
				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	@Override public int cooldownTicks() {
		if (mPhase <= 3) {
			return 10 * 20;
		} else {
			return 5 * 20;
		}
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	private void summon() {
		new BukkitRunnable() {

			@Override public void run() {
				if (mChargeUp.nextTick(2)) {
					this.cancel();

					mChargeUp.setTitle(ChatColor.GREEN + "Unleashing " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Lava Cannon...");
					new BukkitRunnable() {
						int mT = 0;

						@Override
						public synchronized void cancel() {
							super.cancel();
							mChargeUp.reset();
							mChargeUp.setTitle(ChatColor.GREEN + "Charging " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "Lava Cannon...");
						}

						@Override public void run() {
							mChargeUp.setProgress(1 - ((double) mT / (LavaCannonBoss.BULLET_DURATION + 40)));
							if (mT > (LavaCannonBoss.BULLET_DURATION + 40)) {
								this.cancel();
							}
							mT++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);

		int rand = (int) Math.floor(FastUtils.randomDoubleInRange(0, 7.99));

		mCenter.getWorld().playSound(mCenter, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 5, 0.5f);
		Location loc = mCenter.clone().add(mSummonSpots.get(rand));
		LibraryOfSoulsIntegration.summon(loc, "LavaTitan");

		new BukkitRunnable() {
			@Override public void run() {
				mCenter.getWorld().playSound(mCenter, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 5, 0.5f);
				int rand2 = (int) Math.floor(FastUtils.randomDoubleInRange(0, 6.99));
				if (rand2 == rand) {
					rand2 = 7;
				}

				Location loc = mCenter.clone().add(mSummonSpots.get(rand2));
				LibraryOfSoulsIntegration.summon(loc, "LavaTitan");
			}
		}.runTaskLater(mPlugin, 40);
	}
}
