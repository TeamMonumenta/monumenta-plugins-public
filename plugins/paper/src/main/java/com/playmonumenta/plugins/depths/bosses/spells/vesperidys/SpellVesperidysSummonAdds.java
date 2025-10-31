package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.bosses.vesperidys.VesperidysBlockPlacerBoss;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellVesperidysSummonAdds extends Spell {
	private final LoSPool mNormalPool = LoSPool.fromString("~VesperidysNormalAdds");
	private final LoSPool mElitePool = LoSPool.fromString("~VesperidysCrystals");

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;
	private final int mTimeBetween;
	private final int mTimeBetweenElite;

	private static final int NORMAL_BASE = 2;
	private static final int NORMAL_PER_PLAYER = 1;

	private final int mNumElite;
	private final List<Player> mWarnedPlayers = new ArrayList<>();

	private static final double CRYSTAL_HEALTH = 600;

	private int mTimer;
	private int mEliteTimer;

	public SpellVesperidysSummonAdds(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, int timeBetween, int timeBetweenElite, int numElite) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;

		mTimeBetween = timeBetween;
		mTimeBetweenElite = timeBetweenElite;
		mNumElite = numElite;

		mTimer = timeBetween;
		mEliteTimer = timeBetweenElite / 2;
	}

	@Override
	public void run() {
		mTimer--;
		World world = mBoss.getWorld();

		int numPlayers = PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true, false).size();

		if (mTimer < 0 && countMobs() < 25) {
			mTimer = mTimeBetween + FastUtils.randomIntInRange(0, 5 * 4);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.0f, 1.0f);
			PartialParticle particles = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 50, 1, 1, 1);
			particles.spawnAsEnemy();

			int numNormalMobs = NORMAL_BASE + numPlayers * NORMAL_PER_PLAYER;
			List<Integer> splits = new ArrayList<>();

			if (numNormalMobs > 3) {
				int split1 = numNormalMobs / 2;

				splits.add(split1);
				splits.add(numNormalMobs - split1);
			} else {
				splits.add(numNormalMobs);
			}

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				for (int split : splits) {
					summonMobs(split);
				}
			}, 20);
		}

		if (mNumElite > 0) {
			mEliteTimer--;
			if (mEliteTimer < 0 && (!mVesperidys.mInvincible || mBoss.isInvulnerable())) {
				mEliteTimer = mTimeBetweenElite + FastUtils.randomIntInRange(0, 10 * 4);

				world.playSound(mBoss.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.HOSTILE, 1.0f, 0.5f);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					world.playSound(mBoss.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 1.0f, 0.5f);
				}, 20);

				PartialParticle particles = new PartialParticle(Particle.VILLAGER_HAPPY, mBoss.getLocation(), 50, 1, 1, 1);
				particles.spawnAsEnemy();

				summonCrystals();
			}
		}
	}

	public void summonMobs(int amount) {
		List<Vesperidys.Platform> platforms = mVesperidys.mPlatformList.getShuffledPlatforms(null);
		Vesperidys.Platform selectedPlatform = null;

		// Prioritizes platforms which doesn't have adds on it (including boss itself).
		for (Vesperidys.Platform platform : platforms) {
			selectedPlatform = platform;
			if (platform.getMobsOnPlatform().isEmpty() && platform.getPlayersOnPlatform().isEmpty()) {
				break;
			}
		}

		Location newLoc;
		if (selectedPlatform == null) {
			// Failsafe. Should not happen, ever.
			newLoc = mVesperidys.mSpawnLoc;
		} else {
			newLoc = selectedPlatform.getCenter().add(0, 1, 0);
		}

		for (int i = 0; i < amount; i++) {
			double x = FastUtils.randomDoubleInRange(-2, 2);
			double z = FastUtils.randomDoubleInRange(-2, 2);

			Location mobLoc = newLoc.clone().add(x, 10, z);
			LivingEntity e = (LivingEntity) mNormalPool.spawn(mobLoc);

			if (e != null) {
				e.addScoreboardTag("DD2BossFight3");
				e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 5 * 20, 0));
				mPlugin.mBossManager.manuallyRegisterBoss(e, new VesperidysBlockPlacerBoss(mPlugin, e, mVesperidys));
			}
		}
	}

	public void summonCrystals() {
		List<Vesperidys.Platform> bossPlatform = List.of(Objects.requireNonNull(mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss)));

		List<Vesperidys.Platform> summonerPlatforms = mVesperidys.mPlatformList.getRandomPlatforms(bossPlatform, mNumElite);

		for (Player player : PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true)) {
			if (!mWarnedPlayers.contains(player)) {
				mWarnedPlayers.add(player);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					player.sendMessage(Component.text("The Vesperidys is channeling its power through the crystals! Each existing crystals applies ", NamedTextColor.YELLOW)
						.append(Component.text("damage ", NamedTextColor.RED))
						.append(Component.text("and ", NamedTextColor.YELLOW))
						.append(Component.text("resistance ", NamedTextColor.GRAY))
						.append(Component.text("buffs to the Vesperidys!", NamedTextColor.YELLOW))
					);
				}, 3 * 20);
			}
		}


		BukkitRunnable crystalRunnable = new BukkitRunnable() {
			int mCrystalTicks = 0;
			final Location mStartLoc = mBoss.getLocation().add(0, 1.5, 0);

			@Override
			public void run() {
				if (mVesperidys.mDefeated) {
					this.cancel();
					return;
				}

				if (mCrystalTicks >= 40) {
					for (Vesperidys.Platform platform : summonerPlatforms) {
						LivingEntity e = (LivingEntity) mElitePool.spawn(platform.getCenter().clone().add(0, 1, 0));

						if (e != null) {
							EntityUtils.setMaxHealthAndHealth(e, DepthsParty.getAscensionScaledHealth(CRYSTAL_HEALTH, mVesperidys.mParty));
							e.addScoreboardTag("DD2BossFight3");
							e.addScoreboardTag("VoidCrystal");
						}
					}

					this.cancel();
					return;
				}

				mCrystalTicks++;
				// Particles that slowly reach summoners.
				for (Vesperidys.Platform platform : summonerPlatforms) {
					Location endLoc = platform.getCenter().add(0, 2.5, 0);
					Vector dir = LocationUtils.getDirectionTo(endLoc, mStartLoc).normalize();
					double distance = mStartLoc.distance(endLoc);
					double particleDistance = Math.min(distance, ((double) mCrystalTicks / 35) * distance);

					Location particleLoc = mStartLoc.clone().add(dir.multiply(particleDistance));

					new PartialParticle(Particle.END_ROD, particleLoc, 1)
						.extra(10000000)
						.spawnAsBoss();
					new PartialParticle(Particle.REDSTONE, particleLoc, 1)
						.data(new Particle.DustOptions(Color.fromRGB(128, 128, 128), 0.75f))
						.spawnAsBoss();

					new PPCircle(Particle.END_ROD, particleLoc.clone().add(0, -1.7, 0), Math.max(0.5, Math.min(2, -(1.0 / 100.0) * mCrystalTicks * (mCrystalTicks - 40))))
						.extra(10000000)
						.count(10)
						.spawnAsBoss();
				}
			}
		};

		crystalRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(crystalRunnable);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}


	public int countMobs() {
		// Look for mobs with the DropShardTag
		List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100);
		int count = 0;
		for (LivingEntity e : livingEntities) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains(VesperidysBlockPlacerBoss.identityTag)) {
				count++;
			}
		}
		return count;
	}
}
