package com.playmonumenta.plugins.bosses.bosses;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;

public class CarapaceBoss extends BossAbilityGroup {

	private static class DamageInstance {

		public final int mTime;
		public final double mDamage;

		public DamageInstance(int time, double damage) {
			mTime = time;
			mDamage = damage;
		}
	}

	public static final String identityTag = "boss_carapace";
	public static final int detectionRange = 40;

	private static final String SPEED_EFFECT_NAME = "CarapaceSpeedEffect";
	private static final int PERIOD = 20 * 10;

	private final com.playmonumenta.plugins.Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mCarapaceHealth;
	private final double mSpeedEffect;

	private final List<DamageInstance> mDamageInstancesPeriod = new LinkedList<DamageInstance>();
	private final List<DamageInstance> mDamageInstancesReactivate = new LinkedList<DamageInstance>();

	private double mDamageCounterPeriod = 0;
	private double mDamageCounterReactivate = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CarapaceBoss(plugin, boss);
	}

	public CarapaceBoss(Plugin plugin, LivingEntity boss) {
		mPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mBoss = boss;

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);

		double carapaceHealth = 0;
		double speedEffect = 0;

		for (String tag : mBoss.getScoreboardTags()) {
			if (tag.startsWith(identityTag) && !tag.equals(identityTag)) {
				try {
					String[] values = tag.substring(identityTag.length()).split(",");

					AttributeInstance health = mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
					if (health != null) {
						carapaceHealth = Integer.parseInt(values[0]) / 100.0 * health.getValue();
					}

					speedEffect = Integer.parseInt(values[1]) / 100.0;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		mCarapaceHealth = carapaceHealth;
		mSpeedEffect = speedEffect;
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();

		int time = mBoss.getTicksLived();
		double damage = event.getDamage();
		DamageInstance newDamageInstance = new DamageInstance(time, damage);

		Iterator<DamageInstance> iter = mDamageInstancesPeriod.iterator();
		while (iter.hasNext()) {
			DamageInstance damageInstance = iter.next();
			if (time - damageInstance.mTime > PERIOD) {
				mDamageCounterPeriod -= damageInstance.mDamage;
				iter.remove();
			}
		}

		mDamageInstancesPeriod.add(newDamageInstance);

		double remainingCarapaceHealth = mCarapaceHealth - mDamageCounterPeriod;

		if (remainingCarapaceHealth > 0) {
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.05f, 2f);
			world.playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.05f, 0.5f);

			double newDamage = damage - remainingCarapaceHealth;
			if (newDamage > 0) {
				event.setDamage(newDamage);
			} else {
				event.setDamage(0);
			}
		}

		mDamageCounterPeriod += damage;

		mDamageInstancesReactivate.add(newDamageInstance);
		mDamageCounterReactivate += damage;

		if (event.getDamage() > 0) {
			int reactivateTick = 0;
			while (mDamageCounterReactivate > mCarapaceHealth) {
				DamageInstance damageInstance = mDamageInstancesReactivate.remove(0);
				mDamageCounterReactivate -= damageInstance.mDamage;
				reactivateTick = damageInstance.mTime;
			}

			int duration = PERIOD - mBoss.getTicksLived() + reactivateTick;

			NavigableSet<Effect> effects = mPlugin.mEffectManager.getEffects(mBoss, SPEED_EFFECT_NAME);
			if (effects == null) {
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.3f, 0.2f);

				if (mSpeedEffect > 0) {
					mPlugin.mEffectManager.addEffect(mBoss, SPEED_EFFECT_NAME, new PercentSpeed(duration, mSpeedEffect, SPEED_EFFECT_NAME));
				}
			} else {
				effects.last().setDuration(duration);
			}
		}
	}

}
