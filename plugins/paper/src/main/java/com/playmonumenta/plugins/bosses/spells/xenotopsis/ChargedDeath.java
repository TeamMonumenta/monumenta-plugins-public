package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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
	private static final int TOTAL_DELAY = 240;

	// the length of the windup portion of each strike, in ticks
	// motion is where it tracks the player, and total is the total time
	private static final int WINDUP_MOTION_DURATION = 20;
	private static final int WINDUP_TOTAL_DURATION = 35;

	// the radius of the attack
	private static final double STRIKE_RADIUS = 2.25;

	// the attack and death damage of the attack
	private static final int ATTACK_DAMAGE = 75;
	private static final int DEATH_DAMAGE = 10;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;

	private final int mDelay;

	private final ArrayList<Player> mTargetedPlayers = new ArrayList<>();

	private int mDelayTicks = 0;

	public ChargedDeath(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mXenotopsis = xenotopsis;

		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true).size();
		mDelay = (int) (TOTAL_DELAY / (1 + 2 * Math.log(playerCount))); // formula for cooldown reduction: delay / (1 + 2log(c))
	}

	@Override
	public void run() {
		mDelayTicks += 5;
		if (mDelayTicks > mDelay) {
			mDelayTicks = 0;

			List<Player> validPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true);
			validPlayers.removeAll(mTargetedPlayers);

			if (validPlayers.size() == 0) {
				return;
			}

			attackPlayer(validPlayers.get(FastUtils.randomIntInRange(0, validPlayers.size() - 1)));
		}
	}

	private void attackPlayer(Player player) {
		mTargetedPlayers.add(player);

		new BukkitRunnable() {
		    int mTicks = 0;
			@Nullable Location mFinalLocation = null;

		    @Override
		    public void run() {
				if (mTicks < WINDUP_MOTION_DURATION) {
					if (mTicks % 10 == 0) {
						mWorld.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.8f, 1.87f);
					}

					if (mTicks == 0) {
						mWorld.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 1.2f, 1.73f);
					}

					// particle effects
					new PartialParticle(Particle.CRIT_MAGIC, player.getLocation().clone().add(0, 1.5, 0), 10, 0, 1, 0).spawnAsBoss();
				} else {
					if (mFinalLocation == null) {
						mFinalLocation = player.getLocation().clone();
					}

					if (mTicks % 5 == 0) {
						mWorld.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.8f, 1.35f);
					}
					if (mTicks == WINDUP_MOTION_DURATION) {
						mWorld.playSound(mFinalLocation, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1.5f, 0.5f);
					}

					// particle effects
					if (mTicks % 2 == 0) {
						new PartialParticle(Particle.CRIT_MAGIC, mFinalLocation.clone().add(0, 1.5, 0), 10, 0.3, 1, 0.3)
							.spawnAsBoss();
						new PPCircle(Particle.CLOUD, mFinalLocation, STRIKE_RADIUS)
							.count(7)
							.delta(0.07, 0.07, 0.07)
							.spawnAsBoss();
					}

					if (mTicks == WINDUP_TOTAL_DURATION) {
						mWorld.strikeLightningEffect(mFinalLocation);

						Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mFinalLocation, 3, STRIKE_RADIUS);
						for (Player player : hitbox.getHitPlayers(true)) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mXenotopsis.scaleDamage(ATTACK_DAMAGE), null, false, true, "Charged Death");
							mXenotopsis.changePlayerDeathValue(player, DEATH_DAMAGE, false);
						}
					}
				}

		        mTicks++;
				if (mTicks > WINDUP_TOTAL_DURATION || mBoss.isDead()) {
					mTargetedPlayers.remove(player);
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
