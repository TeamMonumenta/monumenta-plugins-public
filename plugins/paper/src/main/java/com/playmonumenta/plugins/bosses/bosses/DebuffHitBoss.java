package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DebuffHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_debuffhit";
	public static final int detectionRange = 50;
	public static final String SLOWNESS_TAG = "BossDebuffhitSlowness";
	public static final String WEAKNESS_TAG = "BossDebuffhitWeakness";
	public final int mDebuffDuration = 60;
	public final double mSlownessPotency = -0.1;
	public final double mWeaknessPotency = -0.1;

	public DebuffHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			if (BossUtils.bossDamageBlocked(player, mBoss.getLocation()) && event.getType() != DamageType.MAGIC) {
				return;
			}
		}
		int rand = FastUtils.RANDOM.nextInt(4);
		switch (rand) {
			case 0 -> com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(damagee, SLOWNESS_TAG,
				new PercentSpeed(mDebuffDuration, mSlownessPotency, SLOWNESS_TAG));
			case 1 -> com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(damagee, WEAKNESS_TAG,
				new PercentDamageDealt(mDebuffDuration, mWeaknessPotency).damageTypes(DamageType.getScalableDamageType()));
			case 2 ->
				damagee.addPotionEffect(new PotionEffect(PotionEffectType.POISON, mDebuffDuration, 0, false, true));
			default ->
				damagee.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, mDebuffDuration, 0, false, true));
		}
	}
}
