package com.playmonumenta.plugins.bosses.bosses.gray;

import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GraySummoned extends BossAbilityGroup {
	public static final String identityTag = "boss_gray_summoned";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GraySummoned(plugin, boss);
	}

	public GraySummoned(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		if (event.getEntity() != null && event.getEntity().hasMetadata("MonumentaBossesGrayExorcism")) {
			mBoss.damage(15);
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2, false, true));
		}
	}
}
