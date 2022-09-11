package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WhispersBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_whispers";
	public static final int detectionRange = 50;

	public static class Parameters extends BossParameters {
		public double PERCENT_DAMAGE = 0;
	}

	final Parameters mParam;


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WhispersBoss(plugin, boss);
	}

	public WhispersBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new WhispersBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player && !event.getType().equals(DamageType.TRUE) && !event.isBlocked()) {
			double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double trueDamage = mParam.PERCENT_DAMAGE / 100.0 * maxHealth;
			PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			double resistanceLevel = (resistance == null ? 0 : resistance.getAmplifier() + 1);
			double resistanceMod = (resistanceLevel >= 5 ? 1.0 : 1.0 / (1.0 - 0.2 * resistanceLevel));
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (!player.isDead()) {
					DamageUtils.damage(null, damagee, new DamageEvent.Metadata(DamageType.TRUE, null, null, "Whispers"), trueDamage * resistanceMod, true, false, false);
				}
				},
				1);
		}
	}
}

