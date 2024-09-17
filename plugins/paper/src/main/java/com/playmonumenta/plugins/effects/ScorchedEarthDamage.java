package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.ScorchedEarthCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class ScorchedEarthDamage extends Effect {
	public static final String effectID = "ScorchedEarthDamage";
	private static final Set<DamageType> mIgnoredDamageTypes = EnumSet.of(
		DamageType.AILMENT,
		DamageType.FIRE,
		DamageType.OTHER,
		DamageType.TRUE,
		DamageType.FALL,
		DamageType.POISON
	);

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
		if (event.getSource() == null || event.isCancelled()) {
			return;
		}
		// prevent ability from procing from spam clicking
		if (event.getFlatDamage() <= 1) {
			return;
		}
		if (event.getAbility() == ClassAbility.SCORCHED_EARTH) {
			return;
		}
		DamageType type = event.getType();
		if (mIgnoredDamageTypes.contains(type)) {
			return;
		}
		if ((type == DamageType.MELEE && event.getDamager() instanceof Player player && player.getCooledAttackStrength(0.5f) > 0.9) ||
				(type == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, false)) ||
				(type != DamageType.MELEE && type != DamageType.PROJECTILE && event.getDamager() instanceof Player)) {
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
