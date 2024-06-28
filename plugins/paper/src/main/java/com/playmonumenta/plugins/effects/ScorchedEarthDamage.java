package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.ScorchedEarthCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ScorchedEarthDamage extends Effect {
	public static final String effectID = "ScorchedEarthDamage";

	private final double mDamage;
	private final Player mAlchemist;
	private final PlayerItemStats mStats;
	private final int mFireTickDuration;
	private final ScorchedEarthCS mCosmetic;

	public ScorchedEarthDamage(int duration, double damage, Player player, PlayerItemStats stats, int fireDuration, ScorchedEarthCS cosmetic) {
		super(duration, effectID);
		mDamage = damage;
		mAlchemist = player;
		mStats = stats;
		mFireTickDuration = fireDuration;
		mCosmetic = cosmetic;
	}

	@Override
	public double getMagnitude() {
		return mDamage;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.getSource() == null) {
			return;
		}
		DamageType type = event.getType();
		if (type != DamageType.AILMENT && type != DamageType.FIRE && type != DamageType.OTHER && type != DamageType.TRUE && event.getAbility() != ClassAbility.SCORCHED_EARTH
			    && (type != DamageType.MELEE || !(event.getDamager() instanceof Player player) || player.getCooledAttackStrength(0) > 0.5f)) {
			DamageUtils.damage(mAlchemist, entity, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.SCORCHED_EARTH, mStats), mDamage, true, false, false);
			EntityUtils.applyFire(Plugin.getInstance(), mFireTickDuration, entity, mAlchemist);
			mCosmetic.damageEffect(entity, mAlchemist);
		}
	}

	@Override
	public String toString() {
		return String.format("ScorchedEarthDamage duration=%d", this.getDuration());
	}
}
