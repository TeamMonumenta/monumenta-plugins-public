package com.playmonumenta.plugins.bosses.bosses.gray;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

public class GraySummoned extends BossAbilityGroup {
	public static final String identityTag = "boss_gray_summoned";
	public static final int detectionRange = 40;

	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GraySummoned(plugin, boss);
	}

	public GraySummoned(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}

	@Override
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		if (event.getEntity() != null && event.getEntity().hasMetadata("MonumentaBossesGrayExorcism")) {
			ProjectileSource source = event.getEntity().getSource();
			if (source != null && source instanceof Entity) {
				mBoss.damage(15, (Entity)source);
			} else {
				mBoss.damage(15);

			}
			mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2, false, true));
		}
	}
}
