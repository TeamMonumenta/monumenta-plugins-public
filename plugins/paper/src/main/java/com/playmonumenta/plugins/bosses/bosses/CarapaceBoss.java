package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

	private final double mCarapaceHealth;
	private final double mSpeedEffect;

	private final List<DamageInstance> mDamageInstancesPeriod = new ArrayList<DamageInstance>();
	private final List<DamageInstance> mDamageInstancesReactivate = new ArrayList<DamageInstance>();

	private double mDamageCounterPeriod = 0;
	private double mDamageCounterReactivate = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CarapaceBoss(plugin, boss);
	}

	public CarapaceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);

		double carapaceHealth = 0;
		double speedEffect = 0;

		for (String tag : boss.getScoreboardTags()) {
			if (tag.startsWith(identityTag) && !tag.equals(identityTag)) {
				try {
					String[] values = tag.substring(identityTag.length()).split(",");

					AttributeInstance health = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
					if (health != null) {
						carapaceHealth = Integer.parseInt(values[0]) / 100.0 * health.getValue();
					}

					speedEffect = Integer.parseInt(values[1]) / 100.0;
				} catch (Exception e) {
					e.printStackTrace();
				}

				break;
			}
		}

		mCarapaceHealth = carapaceHealth;
		mSpeedEffect = speedEffect;
	}

	@Override
	public void onHurt(DamageEvent event) {
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

			double newDamage = damage - remainingCarapaceHealth;
			if (newDamage > 0) {
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.3f, 0.2f);
				event.setDamage(newDamage);
			} else {
				world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.05f, 2f);
				world.playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.05f, 0.5f);
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

			EffectManager.getInstance().addEffect(mBoss, SPEED_EFFECT_NAME, new PercentSpeed(duration, mSpeedEffect, SPEED_EFFECT_NAME));
		}
	}

}
