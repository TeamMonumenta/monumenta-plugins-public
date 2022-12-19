package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TagScalingBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tagscaling";
	public static final int detectionRange = 30;

	public static class Parameters extends BossParameters {
		@BossParam(help = "To to check for when scaling")
		public String TAG = "SKTHard";
		@BossParam(help = "% Value damage taken is decreased by")
		public double DAMAGE_REDUCTION = 0.2;
		@BossParam(help = "% Value damage dealt is increased by")
		public double DAMAGE_INCREASE = 0.2;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TagScalingBoss(plugin, boss);
	}

	private TagScalingBoss.Parameters mParams;

	public TagScalingBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new TagScalingBoss.Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (source instanceof Player && source.getScoreboardTags().contains(mParams.TAG)) {
			event.setDamage(event.getDamage() * (1d - mParams.DAMAGE_REDUCTION));
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player && damagee.getScoreboardTags().contains(mParams.TAG)) {
			event.setDamage((event.getDamage() * (1d + mParams.DAMAGE_INCREASE)));

		}
	}
}
