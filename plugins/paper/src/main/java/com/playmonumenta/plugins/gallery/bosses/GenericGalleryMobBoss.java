package com.playmonumenta.plugins.gallery.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryUtils;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

public class GenericGalleryMobBoss extends BossAbilityGroup {
	public static final String identityTag = "GalleryMobAbility";

	private static final double DISTANCE = 15;
	private static final int DESPAWN_TIMER = 20 * 20;

	private static final EntityTargets TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, 100, false, EntityTargets.Limit.CLOSER_ONE);
	private static final EntityTargets TARGET_DESPAWN = new EntityTargets(EntityTargets.TARGETS.PLAYER, DISTANCE, true, EntityTargets.Limit.CLOSER_ONE);

	public GenericGalleryMobBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		if (!(boss instanceof Mob mob)) {
			GalleryUtils.printDebugMessage("BUG! somehow giving GenericGalleryMobBoss to a non Mob!! this is a bug! " + boss.getName());
			return;
		}

		if (boss instanceof Wolf || boss instanceof Golem || boss instanceof Dolphin || boss instanceof Ocelot) {
			boss.setRemoveWhenFarAway(true);
		}

		//Spell that handle the targeting system
		Spell targetSpell = new Spell() {
			private LivingEntity mLastTarget = null;

			@Override
			public void run() {
				if (EntityUtils.isStunned(mob)) {
					return;
				}

				if (mLastTarget != mob.getTarget() && mob.getTarget() instanceof Player) {
					mLastTarget = mob.getTarget();
				}

				if (mLastTarget != null) {
					if (!mLastTarget.isValid() || mLastTarget.isDead() || GalleryUtils.isPlayerDeath(mLastTarget) || AbilityUtils.isStealthed((Player) mLastTarget)) {
						mLastTarget = null;
						mob.setTarget(null);
					}
				}

				if (mLastTarget == null) {
					List<? extends LivingEntity> targets = TARGETS.getTargetsList(mob).stream().filter((player) -> !GalleryUtils.isPlayerDeath(player)).toList();
					if (targets.size() > 0) {
						mob.setTarget(targets.get(0));
						mLastTarget = targets.get(0);
					} else {
						mLastTarget = null;
						mob.setTarget(null);
					}
				}

				mob.setTarget(mLastTarget);
			}

			@Override
			public int cooldownTicks() {
				return 5;
			}

			@Override public void onHurtByEntity(DamageEvent event, Entity damager) {
				if ((mLastTarget == null || mob.getLocation().distanceSquared(mLastTarget.getLocation()) > mob.getLocation().distance(damager.getLocation())) && damager instanceof Player player && !AbilityUtils.isStealthed(player)) {
					mLastTarget = player;
				}
			}

			@Override public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
				if ((mLastTarget == null || mob.getLocation().distanceSquared(mLastTarget.getLocation()) > mob.getLocation().distance(source.getLocation())) && source instanceof Player player && !AbilityUtils.isStealthed(player)) {
					mLastTarget = player;
				}
			}
		};

		//Spell that handle the despawn system
		Spell despawnSpell = new Spell() {
			int mTimer = 0;
			@Override public void run() {
				mTimer += 10;

				if (mTimer >= DESPAWN_TIMER && TARGET_DESPAWN.getTargetsList(mBoss).size() <= 0) {
					GalleryUtils.despawnMob(mBoss);
				}
			}

			@Override public int cooldownTicks() {
				return 10;
			}

			@Override public void onDamage(DamageEvent event, LivingEntity damagee) {
				mTimer = 0;
			}

			@Override public void onHurtByEntity(DamageEvent event, Entity damager) {
				if (damager instanceof Player) {
					mTimer = 0;
				}
			}

			@Override public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
				if (source instanceof Player) {
					mTimer = 0;
				}
			}
		};



		super.constructBoss(SpellManager.EMPTY, List.of(targetSpell, despawnSpell), 150, null, 5);

	}
}
