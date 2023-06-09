package com.playmonumenta.plugins.bosses.bosses.gray;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class GrayDemonSummoner extends GrayStrongSummonerBase {
	public static final String identityTag = "boss_gray_summ_demon";
	public static final int detectionRange = 35;
	private static final String name = "ConjuredDemon";

	public GrayDemonSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, name);
	}
}
