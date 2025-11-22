package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellStranglingRupture extends Spell {

	public static final String ABILITY_NAME = "Strangling Rupture";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mDetectionRange;
	private final int mPlatformRadius;
	private final int mRuptureDelay;
	private final Location mSpawnLoc;
	private final int mCooldown;
	private final ChargeUpManager mChargeUp;

	public SpellStranglingRupture(Plugin plugin, LivingEntity boss, int range, int castTime, int detectionRange, int platformRadius, int ruptureDelay, Location spawnLoc, int cooldown) {
		mPlugin = plugin;
		mBoss = boss;
		mDetectionRange = detectionRange;
		mPlatformRadius = platformRadius;
		mRuptureDelay = ruptureDelay;
		mSpawnLoc = spawnLoc;
		mCooldown = cooldown;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		BukkitRunnable runnable = new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUp.getTime() % 2 == 0) {
					Set<Entity> islandTargets = new HashSet<>();

					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						Entity platform = p.getNearbyEntities(mDetectionRange, mDetectionRange, mDetectionRange).stream()
							.filter(entity -> entity.getScoreboardTags().contains("boss_totemplatform"))
							.min((entity1, entity2) -> (int) (entity1.getLocation().distance(p.getLocation()) - entity2.getLocation().distance(p.getLocation())))
							.orElse(null);
						if (platform != null) {
							islandTargets.add(platform);
						} else {
							if (mChargeUp.getTime() % 10 == 0) {
								mBoss.getWorld().playSound(p.getLocation(), Sound.BLOCK_WOOD_BREAK, SoundCategory.HOSTILE, 0.75f, 1f);
							}
							new PPCircle(Particle.BLOCK_CRACK, p.getLocation(), mPlatformRadius - 1).data(Material.DARK_OAK_WOOD.createBlockData()).count(35).spawnAsBoss();
						}
					}

					for (Entity e : islandTargets) {
						if (mChargeUp.getTime() % 10 == 0) {
							mBoss.getWorld().playSound(e.getLocation(), Sound.BLOCK_WOOD_BREAK, SoundCategory.HOSTILE, 0.75f, 1f);
						}
						new PPCircle(Particle.BLOCK_CRACK, e.getLocation(), mPlatformRadius - 1).data(Material.DARK_OAK_WOOD.createBlockData()).count(35).spawnAsBoss();
					}
				}

				if (mChargeUp.nextTick()) {
					Set<Entity> islandTargets = new HashSet<>();
					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						Collection<Entity> nearbyEntities = p.getNearbyEntities(mDetectionRange, mDetectionRange, mDetectionRange);

						Entity platform = nearbyEntities.stream()
							.filter(entity -> entity.getScoreboardTags().contains("boss_totemplatform"))
							.min((entity1, entity2) -> (int) (entity1.getLocation().distance(p.getLocation()) - entity2.getLocation().distance(p.getLocation())))
							.orElse(null);

						Entity armorStand = nearbyEntities.stream()
							.filter(entity -> entity.getScoreboardTags().contains("Hycenea_Island"))
							.min((entity1, entity2) -> (int) (entity1.getLocation().distance(p.getLocation()) - entity2.getLocation().distance(p.getLocation())))
							.orElse(null);

						if (platform != null && armorStand != null) {
							islandTargets.add(platform);
							armorStand.addScoreboardTag("Hycenea_StranglingRupture_Target");
						} else {
							new PPCircle(Particle.BLOCK_CRACK, p.getLocation(), mPlatformRadius - 1).data(Material.DARK_OAK_WOOD.createBlockData()).count(5).spawnAsBoss();
							new PPCircle(Particle.BLOCK_CRACK, p.getLocation(), mPlatformRadius - 1).data(Material.DARK_OAK_LEAVES.createBlockData()).count(5).spawnAsBoss();
							for (double rad = 0; rad < mPlatformRadius; rad += 0.5) {
								new PPCircle(Particle.SMOKE_NORMAL, p.getLocation(), rad).count(5).spawnAsBoss();
							}

							PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME);
						}
					}
					for (Entity e : islandTargets) {
						spawnRupture(e);
					}

					mChargeUp.setColor(BossBar.Color.RED);
					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
					BukkitRunnable bossBarRunnable = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mChargeUp.setProgress((double) mT / mRuptureDelay);

							if (mT++ >= mRuptureDelay) {
								mChargeUp.remove();
								this.cancel();
							}
						}
					};
					bossBarRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(bossBarRunnable);

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void spawnRupture(Entity e) {

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT % 2 == 0) {
					new PPCircle(Particle.BLOCK_CRACK, e.getLocation(), mPlatformRadius - 1).data(Material.DARK_OAK_WOOD.createBlockData()).count(10).spawnAsBoss();
					new PPCircle(Particle.BLOCK_CRACK, e.getLocation(), mPlatformRadius - 1).data(Material.DARK_OAK_LEAVES.createBlockData()).count(10).spawnAsBoss();
					for (double rad = 0; rad < mPlatformRadius; rad += 0.5) {
						new PPCircle(Particle.SMOKE_NORMAL, e.getLocation(), rad).count(5).spawnAsBoss();
					}
					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						p.playSound(e.getLocation(), Sound.BLOCK_AZALEA_BREAK, SoundCategory.HOSTILE, 1.5f, 0.5f);
					}
				}
				if (mT > mRuptureDelay) {
					LivingEntity entity = (LivingEntity) e;
					entity.setHealth(0);
					GrowableAPI.grow("stranglingRupture1", e.getLocation().add(0, -1, 0), 1, 50, true);

					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						p.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1f);
						p.playSound(e.getLocation(), Sound.BLOCK_VINE_BREAK, SoundCategory.HOSTILE, 1.5f, 0.5f);
					}
					this.cancel();
				}
				mT++;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}
