package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellEarthsWrath extends Spell {
	private static final String SPELL_NAME = "Earth's Wrath";
	private static final String SLOWNESS_SRC = "EarthsWrathSlowness";
	private static final double SLOWNESS_POTENCY = -0.5;
	private static final int DEBUFF_DURATION = 20 * 10;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mY;
	private final ChargeUpManager mChargeUp;

	public SpellEarthsWrath(Plugin plugin, LivingEntity boss, double y) {
		mPlugin = plugin;
		mBoss = boss;
		mY = y;

		mChargeUp = Kaul.defaultChargeUp(boss, (int) (20 * 2.15), SPELL_NAME);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location centerLoc = mBoss.getLocation();

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % 2 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 2, 1);
				}
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 2, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);
				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.reset();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2, 1);
					Location loc = mBoss.getLocation();
					loc.setY(mY);
					Location particleLoc = loc.clone();
					particleLoc.setY(mY + 0.1);
					BukkitRunnable runnable = new BukkitRunnable() {
						int mTicks = 0;

						@Override
						public void run() {
							mTicks++;
							double radius = mTicks * 0.45;
							new PPCircle(Particle.DAMAGE_INDICATOR, particleLoc, radius)
								.count(50)
								.delta(1, 0, 0)
								.rotateDelta(true)
								.directionalMode(true)
								.extra(1)
								.distanceFalloff(40)
								.spawnAsBoss();
							new PPCircle(Particle.CLOUD, particleLoc, radius)
								.countPerMeter(1)
								.distanceFalloff(40)
								.extra(0.05)
								.spawnAsBoss();
							new PPCircle(Particle.BLOCK_CRACK, particleLoc, radius)
								.countPerMeter(1)
								.data(Material.COARSE_DIRT.createBlockData())
								.distanceFalloff(40)
								.extra(1)
								.spawnAsBoss();

							for (Player player : PlayerUtils.playersInRange(loc, radius + 1, true)) {
								if (player.getLocation().getY() - 0.4 <= mY &&
									Math.abs(LocationUtils.xzDistance(player.getLocation(), loc) - radius) < 1
								) {
									DamageUtils.damage(mBoss, player, DamageType.MAGIC, 24, null, false, true, SPELL_NAME);
									MovementUtils.knockAway(centerLoc, player, -0.6f, 0.8f);
									com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
										new PercentSpeed(DEBUFF_DURATION, SLOWNESS_POTENCY, SLOWNESS_SRC));
									player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, DEBUFF_DURATION, -4));
								}
							}
							if (mTicks >= 20 * 3) {
								this.cancel();
							}
						}
					};
					runnable.runTaskTimer(mPlugin, 5, 1);
					mActiveRunnables.add(runnable);

				}
			}
		};
		runnable.runTaskTimer(mPlugin, 1, 1);
		mActiveRunnables.add(runnable);
	}


	@Override
	public int cooldownTicks() {
		return 20 * 12;
	}

}
