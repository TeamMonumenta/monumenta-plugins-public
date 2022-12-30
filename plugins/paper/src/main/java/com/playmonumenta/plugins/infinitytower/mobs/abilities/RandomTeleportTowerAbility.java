package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class RandomTeleportTowerAbility extends TowerAbility {
	public RandomTeleportTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		EntityUtils.addAttribute(boss, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("ITnoSpeedForYou", -200, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

		Spell spell = new Spell() {

			@Override
			public void run() {
				Location loc = mGame.getRandomLocation();
				boolean teleported = false;
				while (!teleported) {
					loc.add(0, 1, 0);

					if (!loc.getBlock().isSolid() && !loc.clone().add(0, 1, 0).getBlock().isSolid()) {
						//found a good location, start the tp.
						teleported = true;
						new BukkitRunnable() {
							int mTimer = 0;
							final Location mLoc = loc;
							@Override
							public void run() {
								if (mGame.isTurnEnded() || mGame.isGameEnded() || mBoss.isDead() || !mBoss.isValid()) {
									cancel();
								}
								if (this.isCancelled()) {
									return;
								}

								if (mTimer <= 20) {
									new PartialParticle(Particle.SOUL, mLoc, 30, 0.2, 1.5, 0.2, 0.05).spawnAsEntityActive(mBoss);
								} else {
									mBoss.teleport(mLoc);

									for (LivingEntity target : mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs()) {
										if (target.getLocation().distance(mLoc) < 2) {
											DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, 5, null, false, false);
										}
									}
									cancel();
								}
								mTimer += 2;
							}
						}.runTaskTimer(plugin, 0, 2);
					}
				}
			}

			@Override
			public int cooldownTicks() {
				return 160;
			}
		};


		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 100) + 20);

	}
}
