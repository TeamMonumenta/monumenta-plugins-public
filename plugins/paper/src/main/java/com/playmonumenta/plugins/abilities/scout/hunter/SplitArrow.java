package com.playmonumenta.plugins.abilities.scout.hunter;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

/*
 * Split Arrow: When you shoot an enemy, the arrow chains
 * onto the nearest enemy within 5 blocks of the shot enemy,
 * dealing 6 / 10 damage + BM + SS
 */
public class SplitArrow extends Ability {

	private static final int SPLIT_ARROW_1_DAMAGE = 6;
	private static final int SPLIT_ARROW_2_DAMAGE = 10;
	private static final double SPLIT_ARROW_CHAIN_RANGE = 5;

	public SplitArrow(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Split Arrow");
		mInfo.mLinkedSpell = Spells.SPLIT_ARROW;
		mInfo.mScoreboardId = "SplitArrow";
		mInfo.mShorthandName = "SA";
		mInfo.mDescriptions.add("When you hit an enemy with an arrow, the next nearest enemy within 5 blocks takes 6 damage. Any effects the arrows might have are also applied.");
		mInfo.mDescriptions.add("Damage to the second target is increased to 10.");
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.isCritical()) {
			double damage = getAbilityScore() == 1 ? SPLIT_ARROW_1_DAMAGE : SPLIT_ARROW_2_DAMAGE;
			damage += AbilityUtils.getArrowBonusDamage(arrow);
			if (damagee.hasMetadata("PinningShotEnemyIsPinned")) {
				damage *= damagee.getMetadata("PinningShotEnemyIsPinned").get(0).asDouble();
			}

			LivingEntity nearestMob = null;
			double closestDistance = SPLIT_ARROW_CHAIN_RANGE + 1;
			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), SPLIT_ARROW_CHAIN_RANGE, damagee)) {
				double distance = damagee.getLocation().distance(mob.getLocation());
				if (distance < closestDistance) {
					nearestMob = mob;
					closestDistance = distance;
				}
			}

			if (nearestMob != null) {
				Location loc = damagee.getEyeLocation();
				Location eye = nearestMob.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(eye, loc);
				for (int i = 0; i < 50; i++) {
					loc.add(dir.clone().multiply(0.1));
					mWorld.spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0);
					if (loc.distance(eye) < 0.4) {
						break;
					}
				}
				mWorld.spawnParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6);
				mWorld.spawnParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6);
				mWorld.playSound(eye, Sound.ENTITY_ARROW_HIT, 1, 1.2f);
				EntityUtils.damageEntity(mPlugin, nearestMob, damage, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
				MovementUtils.knockAway(damagee, nearestMob, 0.125f, 0.35f);

				if (arrow.getFireTicks() > 0) {
					// Since Flame sets enemies on fire for 5 seconds.
					nearestMob.setFireTicks(100);
				}
			}
		}

		return true;
	}

}
