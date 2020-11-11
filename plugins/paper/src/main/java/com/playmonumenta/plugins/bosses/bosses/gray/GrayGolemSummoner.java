package com.playmonumenta.plugins.bosses.bosses.gray;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;

public class GrayGolemSummoner extends GrayStrongSummonerBase {
	public static final String identityTag = "boss_gray_summ_golem";
	public static final int detectionRange = 35;
	private static final String name = "ConjuredGolem";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayGolemSummoner(plugin, boss);
	}

	public GrayGolemSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, name);
	}
}
