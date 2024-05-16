package com.playmonumenta.plugins.bosses.bosses.gray;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.plugin.Plugin;

public class GraySummoned extends BossAbilityGroup {
	public static final String identityTag = "boss_gray_summoned";
	public static final int detectionRange = 40;

	private static final String SLOWNESS_SRC = "GraySummonedSlowness";

	public GraySummoned(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		if (event.getEntity().hasMetadata("MonumentaBossesGrayExorcism")) {
			DamageUtils.damage(null, mBoss, DamageEvent.DamageType.TRUE, 15.0);
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, SLOWNESS_SRC,
				new PercentSpeed(40, -0.5, SLOWNESS_SRC));
		}
	}
}
