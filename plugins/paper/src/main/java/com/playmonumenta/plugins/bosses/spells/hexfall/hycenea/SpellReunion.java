package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellReunion extends Spell {
	private static final String ABILITY_NAME = "Reunion (☠)";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCastTime;
	private int mRadius;
	private final int mInterval;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellReunion(Plugin plugin, LivingEntity boss, int range, int startingRadius, int channelTime, int castTime, int interval, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mCastTime = castTime;
		mRadius = startingRadius;
		mInterval = interval;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, channelTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		BukkitRunnable chargeRunnable = new BukkitRunnable() {
			@Override
			public void run() {

				if (mChargeUp.getTime() % 40 == 0) {
					for (int i = 0; i < 10; i++) {
						new PPCircle(Particle.ENCHANTMENT_TABLE, mBoss.getLocation().clone().add(0, i, 0), mRadius)
							.ringMode(true)
							.countPerMeter(5)
							.spawnAsBoss();
					}
				}

				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.RED)));
					mChargeUp.setColor(BossBar.Color.RED);

					BukkitRunnable runnable = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							float progress = (float) mT / mCastTime;
							mChargeUp.setProgress(1 - progress);

							for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
								if (LocationUtils.xzDistance(p.getLocation(), mSpawnLoc) >= mRadius) {
									PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME, true, true, true);
								}
							}

							if (mT % mInterval == 0) {
								mRadius--;

								new SpellDestroyCenterPlatform(mSpawnLoc, mRadius, mRadius + 1, mInterval).run();

								for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
									player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 0.5f + progress, 1f);
								}

								for (int i = 0; i < 10; i++) {
									new PPCircle(Particle.ENCHANTMENT_TABLE, mSpawnLoc.clone().add(0, i, 0), mRadius)
										.ringMode(true)
										.countPerMeter(5)
										.spawnAsBoss();
								}
							}

							if (mT++ >= mCastTime) {
								this.cancel();
								mChargeUp.remove();

								for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
									PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME, true, true, true);
								}
							}
						}
					};
					runnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable);
				}
			}
		};
		chargeRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(chargeRunnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
