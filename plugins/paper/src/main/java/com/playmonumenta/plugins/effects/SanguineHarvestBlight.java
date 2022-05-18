package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Similar to Vulnerability,
 * Blight is not considered a debuff itself but increases damage dealt to the entity based
 * on the amount of debuffs the entity has.
 */
public class SanguineHarvestBlight extends Effect {
	public static final String GENERIC_NAME = "SanguineHarvestBlightEffect";

	private final double mAmount;
	private final Plugin mPlugin;
	private final @Nullable EnumSet<DamageType> mAffectedDamageTypes;

	private static final List<PotionEffectType> DEBUFFS = Arrays.asList(
		PotionEffectType.WITHER,
		PotionEffectType.SLOW,
		PotionEffectType.WEAKNESS,
		PotionEffectType.SLOW_DIGGING,
		PotionEffectType.POISON,
		PotionEffectType.BLINDNESS,
		PotionEffectType.CONFUSION,
		PotionEffectType.HUNGER
	);

	public SanguineHarvestBlight(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes, Plugin plugin) {
		super(duration);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mPlugin = plugin;
	}

	public SanguineHarvestBlight(int duration, double amount, Plugin plugin) {
		this(duration, amount, null, plugin);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	public EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			double amount = mAmount;
			if (EntityUtils.isBoss(entity) && amount > 0) {
				amount = amount / 2;
			}
			event.setDamage(event.getDamage() * (1 + amount * countDebuffs(entity)));
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(-mAmount) + " Resistance Per Debuff";
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedDamageTypes != null) {
			types = "";
			for (DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("SanguineHarvestBlight duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}

	private int countDebuffs(LivingEntity mob) {
		int debuffCount = 0;
		for (PotionEffectType effectType : DEBUFFS) {
			PotionEffect effect = mob.getPotionEffect(effectType);
			if (effect != null) {
				debuffCount++;
			}
		}

		if (mob.getFireTicks() > 0) {
			debuffCount++;
		}

		if (EntityUtils.isStunned(mob)) {
			debuffCount++;
		}

		if (EntityUtils.isParalyzed(mPlugin, mob)) {
			debuffCount++;
		}

		if (EntityUtils.isSilenced(mob)) {
			debuffCount++;
		}

		if (EntityUtils.isBleeding(mPlugin, mob)) {
			debuffCount++;
		}

		//Custom slow effect interaction
		if (EntityUtils.isSlowed(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.SLOW) == null) {
			debuffCount++;
		}

		//Custom weaken interaction
		if (EntityUtils.isWeakened(mPlugin, mob) && mob.getPotionEffect(PotionEffectType.WEAKNESS) == null) {
			debuffCount++;
		}

		//Custom vuln interaction
		if (EntityUtils.isVulnerable(mPlugin, mob)) {
			debuffCount++;
		}

		//Custom DoT interaction
		if (EntityUtils.hasDamageOverTime(mPlugin, mob)) {
			debuffCount++;
		}

		return debuffCount;
	}
}
