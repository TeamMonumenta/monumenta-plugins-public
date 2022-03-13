package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class EarthsWrathTowerAbility extends TowerAbility {

	private static final int COOLDOWN = 160;
	private static final int DURATION = 20 * 3;
	private static final int DAMAGE = 8;


	public EarthsWrathTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new Spell() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid() || mGame.isTurnEnded()) {
					return;
				}

				boss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 2, 1);
				Location loc = mBoss.getLocation().clone().add(0, 0.25, 0);

				for (int i = 0; i < 48; i++) {
					int j = i;

					BukkitRunnable runnable = new BukkitRunnable() {
						final BoundingBox mBox = BoundingBox.of(loc, 0.75, 0.4, 0.75);
						final double mRadian1 = Math.toRadians((7.5 * j));
						final Location mPoint = loc.clone().add(FastUtils.cos(mRadian1) * 0.5, 0, FastUtils.sin(mRadian1) * 0.5);
						final Vector mDir = LocationUtils.getDirectionTo(mPoint, loc);
						int mTicks = 0;
						final World mWorld = boss.getWorld();
						final Location mBossLoc = boss.getLocation();
						@Override
						public void run() {
							if (mTicks >= DURATION) {
								this.cancel();
							}

							if (isCancelled()) {
								return;
							}

							if (mBoss.isDead() || !mBoss.isValid() || mGame.isTurnEnded()) {
								cancel();
								return;
							}


							mTicks++;
							mBox.shift(mDir.clone().multiply(0.45));
							Location bLoc = mBox.getCenter().toLocation(mWorld);
							mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, bLoc, 1, 0.25, 0.25, 0.25, 0);
							mWorld.spawnParticle(Particle.CLOUD, bLoc, 1, 0, 0, 0, 0);
							for (LivingEntity target : (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs())) {
								if (target.getBoundingBox().overlaps(mBox)) {
									DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false);
									MovementUtils.knockAway(mBossLoc, target, -0.6f, 0.8f);
									if (EffectManager.getInstance() != null) {
										EffectManager.getInstance().addEffect(target, "ITKaulRoot", new PercentSpeed(60, -100, "ITKaulRoot"));
									}
								}
							}
						}

					};
					runnable.runTaskTimer(mPlugin, 5, 1);
				}
			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, 40);


	}
}
