package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SpellAirGolemStrike extends Spell {
	private static final String SPELL_NAME = "Air Golem Strike";
	private static final String GOLEM_NAME = "PermafrostConstruct";
	private static final int HIT_RADIUS = 2;
	private static final int HIT_HEIGHT = 10;
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 2;

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final List<Location> mTargetLocs = new ArrayList<>();
	private final ChargeUpManager mChargeManager;

	private boolean mCanRun = true;

	public SpellAirGolemStrike(final Plugin plugin, final FrostGiant frostGiant) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mChargeManager = FrostGiant.defaultChargeUp(mFrostGiant.mBoss, CHARGE_DURATION, "Charging " + SPELL_NAME + "...");
	}

	@Override
	public void run() {
		mCanRun = false;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCanRun = true, Constants.TICKS_PER_SECOND * 24);

		List<Player> players = (List<Player>) mFrostGiant.getArenaParticipants();
		if (players.isEmpty()) {
			return;
		}

		if (players.size() < 3) {
			players = Collections.singletonList(players.get(0));
		} else if (players.size() == 3) {
			players = players.subList(0, 1);
		} else if (players.size() <= 10) {
			players = players.subList(0, players.size() / 2);
		} else {
			players = players.subList(0, players.size() / 3 + 2);
		}

		players.forEach(player -> mTargetLocs.add(player.getLocation()));
		spawnGolems();
	}

	private void spawnGolems() {
		final List<LivingEntity> golems = new ArrayList<>();
		final World world = mFrostGiant.mBoss.getWorld();

		mTargetLocs.forEach(location -> {
			while (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				location.add(0, -1, 0);
			}

			try {
				final LivingEntity golem = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(location.clone().add(0, HIT_HEIGHT, 0), GOLEM_NAME));
				golems.add(golem);
			} catch (final NullPointerException noSummon) {
				MMLog.warning(() -> "[SpellAirGolemStrike] The Library of Souls is missing the entry for " + GOLEM_NAME);
				return;
			}

			world.playSound(location, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0.5f);
		});

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT >= CHARGE_DURATION) {
					this.cancel();
					return;
				}

				golems.forEach(golem -> {
					((Mob) golem).getPathfinder().stopPathfinding();
					((Mob) golem).setTarget(null);
					final Location loc = golem.getLocation();
					loc.setYaw(loc.getYaw() + 45);
					golem.teleport(loc);
				});

				mT += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);

		final int chargeRunnablePeriod = 2;
		new BukkitRunnable() {
			final PPCircle mFlameCircle = new PPCircle(Particle.FLAME, new Location(world, 0, 0, 0), HIT_RADIUS).count(36).delta(0.05);
			final PPCircle mDragonBreathCircle = new PPCircle(Particle.DRAGON_BREATH, new Location(world, 0, 0, 0), HIT_RADIUS + 1).count(36).delta(0.1).extra(0.04);
			float mPitch = 1;

			@Override
			public void run() {
				if (mChargeManager.getTime() % Constants.HALF_TICKS_PER_SECOND == 0) {
					mTargetLocs.forEach(location -> {
						mFlameCircle.location(location).spawnAsEntityActive(mFrostGiant.mBoss);
						new PartialParticle(Particle.VILLAGER_ANGRY, location, 2, 0.5, 0.5, 0.5).spawnAsEntityActive(mFrostGiant.mBoss);
						world.playSound(location, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.2f, mPitch);
					});
					mPitch += 0.05f;
				}

				if (mChargeManager.nextTick(chargeRunnablePeriod)) {
					mChargeManager.reset();

					/* Each entry in mTargetLocs corresponds to the same index entry in golems */
					mTargetLocs.forEach(location -> {
						golems.get(mTargetLocs.indexOf(location)).teleport(location);
						new Hitbox.UprightCylinderHitbox(location, HIT_HEIGHT, HIT_RADIUS).getHitPlayers(true)
							.forEach(player -> DamageUtils.damage(mFrostGiant.mBoss, player, DamageType.MELEE, 45, null, false, true, SPELL_NAME));

						final Location particleLoc = location.clone();
						for (double y = location.getY() + 0.1; y < location.getY() + HIT_HEIGHT; y++) {
							particleLoc.setY(y);
							mFlameCircle.location(particleLoc).spawnAsEntityActive(mFrostGiant.mBoss);
							mDragonBreathCircle.location(particleLoc).spawnAsEntityActive(mFrostGiant.mBoss);
						}

						world.playSound(location, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.2f, mPitch);
						world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 1, 0.5f);
						world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.5f);
					});

					mTargetLocs.clear();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, chargeRunnablePeriod);
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 5;
	}

	@Override
	public boolean canRun() {
		return mCanRun;
	}
}
