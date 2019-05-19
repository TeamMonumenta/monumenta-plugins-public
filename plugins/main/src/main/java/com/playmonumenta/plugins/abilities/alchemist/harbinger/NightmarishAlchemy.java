package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Your Alchemist Potions deal +3/7 damage and have a 20%
 * chance of causing mobs to attack nearby mobs for 8 seconds.
 * If you splash 5 or more mobs, at least 1 is guaranteed to be
 * confused.
 */

public class NightmarishAlchemy extends Ability {
	private static final int NIGHTMARISH_ALCHEMY_1_DAMAGE = 3;
	private static final int NIGHTMARISH_ALCHEMY_2_DAMAGE = 7;
	private static final int NIGHTMARISH_ALCHEMY_CONFUSION_DURATION = 20 * 8;
	private static final float NIGHTMARISH_ALCHEMY_CONFUSION_CHANCE = 0.2f;
	private static final int NIGHTMARISH_ALCHEMY_CONFUSION_RANGE = 8;

	private boolean guaranteedApplicationApplied = false;

	private static final Particle.DustOptions NIGHTMARISH_CONFUSION_COLOR = new Particle.DustOptions(
	    Color.fromRGB(62, 0, 102), 1.0f);

	public NightmarishAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Nightmarish";
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		guaranteedApplicationApplied = false;
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int damage = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_DAMAGE : NIGHTMARISH_ALCHEMY_2_DAMAGE;
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity) && entity instanceof Creature) {
						EntityUtils.damageEntity(mPlugin, entity, damage, mPlayer);
						boolean confuse = false;
						if (!EntityUtils.isBoss(entity)) {
							if (mRandom.nextFloat() < NIGHTMARISH_ALCHEMY_CONFUSION_CHANCE) {
								confuse = true;
							} else if (!guaranteedApplicationApplied && affectedEntities.size() >= 5) {
								confuse = true;
								guaranteedApplicationApplied = true;
							}
						}

						if (confuse) {
							List<LivingEntity> mobs = EntityUtils.getNearbyMobs(entity.getLocation(), NIGHTMARISH_ALCHEMY_CONFUSION_RANGE);
							for (LivingEntity mob : mobs) {
								if (mob.getUniqueId() != entity.getUniqueId()) {
									((Creature) entity).setTarget(mob);
									new BukkitRunnable() {
										int t = 0;
										double rotation = 0;

										@Override
										public void run() {
											t++;
											rotation += 20;

											double radian1 = Math.toRadians(rotation);
											Location l = mob.getLocation();
											l.add(Math.cos(radian1) * 0.5, mob.getHeight() + 0.25, Math.sin(radian1) * 0.5);
											mob.getWorld().spawnParticle(Particle.REDSTONE, l, 2, 0, 0, 0, NIGHTMARISH_CONFUSION_COLOR);
											l.subtract(Math.cos(radian1) * 0.5, mob.getHeight() + 0.25, Math.sin(radian1) * 0.5);

											if (t >= NIGHTMARISH_ALCHEMY_CONFUSION_DURATION) {
												this.cancel();
												((Creature) entity).setTarget(null);
											}
										}
									}.runTaskTimer(mPlugin, 0, 1);
								}
								break;
							}
						}
					}
				}
			}
			mWorld.spawnParticle(Particle.SPELL_WITCH, potion.getLocation(), 50, 1, 0, 1, 1);
		}

		return true;
	}
}
