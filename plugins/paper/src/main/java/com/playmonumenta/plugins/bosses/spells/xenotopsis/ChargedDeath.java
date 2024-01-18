package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ChargedDeath extends Spell {

	// the delay between each run of the passive, in ticks
	private static final int TOTAL_DELAY = 400;

	// the length of the windup portion, in ticks
	private static final int WINDUP_DURATION = 20 * 3;

	// the delay between placing a location and striking a location (for each part), in ticks
	private static final int WINDUP_LOCATION_DELAY = 4;
	private static final int STRIKE_LOCATION_DELAY = 2;

	// the radius of the attack
	private static final double STRIKE_RADIUS = 1.0;

	// the attack and death damage of the attack
	private static final int ATTACK_DAMAGE = 85;
	private static final int DEATH_DAMAGE = 10;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;

	private final int mDelay;

	private int mDelayTicks = 0;

	public ChargedDeath(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mXenotopsis = xenotopsis;

		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true).size();
		mDelay = (int) (TOTAL_DELAY / (1 + 2 * Math.log10(playerCount))); // formula for cooldown reduction: delay / (1 + 2log(c))
	}

	@Override
	public void run() {
		mDelayTicks += 5;
		if (mDelayTicks > mDelay) {
			mDelayTicks -= mDelay;

			List<Player> validPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true);

			if (validPlayers.size() == 0) {
				return;
			}

			attackPlayer(validPlayers.get(FastUtils.randomIntInRange(0, validPlayers.size() - 1)));
		}
	}

	private void attackPlayer(Player player) {
		mWorld.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 1.2f, 1.73f);

		new BukkitRunnable() {
			int mTicks = 0;
			final List<Location> mTargetLocations = new ArrayList<>();
			int mIndex = 0;

			@Override
			public void run() {
				if (mTicks < WINDUP_DURATION) {
					if (mTicks % WINDUP_LOCATION_DELAY == 0) {
						Location loc = player.getLocation();
						if (loc.distanceSquared(mBoss.getLocation()) > Xenotopsis.DETECTION_RANGE * Xenotopsis.DETECTION_RANGE) {
							this.cancel();
							return;
						}
						loc.setY(mXenotopsis.mSpawnLoc.getY());
						mTargetLocations.add(loc);
					}

					// effects
					mWorld.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.8f, 1.87f);
					mTargetLocations.forEach(location -> new PartialParticle(Particle.CRIT_MAGIC, location.clone().add(0, 0.3, 0)).count(12).delta(0.3, 0.3, 0.3).extra(0.02).spawnAsBoss());
				} else {
					if (mTicks % STRIKE_LOCATION_DELAY == 0 && mTargetLocations.size() > 0) {
						Location location = mTargetLocations.get(mIndex);

						// visual effect
						mWorld.strikeLightningEffect(location);
						new PartialParticle(Particle.CLOUD, location.clone().add(0, 0.1, 0)).count(20).delta(0.5, 0.2, 0.5).extra(0.04).spawnAsBoss();


						Hitbox hitbox = new Hitbox.UprightCylinderHitbox(location.clone().add(0, -2, 0), 4, STRIKE_RADIUS);
						for (Player player : hitbox.getHitPlayers(true)) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mXenotopsis.scaleDamage(ATTACK_DAMAGE), null, false, true, "Charged Death");
							mXenotopsis.changePlayerDeathValue(player, DEATH_DAMAGE, false);
						}

						mIndex++;
					}
				}

				mTicks++;
				if (mTicks > WINDUP_DURATION + STRIKE_LOCATION_DELAY * mTargetLocations.size() || mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
