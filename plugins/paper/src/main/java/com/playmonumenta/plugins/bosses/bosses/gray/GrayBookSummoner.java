package com.playmonumenta.plugins.bosses.bosses.gray;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.LivingEntity;

/* TODO: Merge this with SpawnMobsBoss */
public class GrayBookSummoner extends GraySwarmSummonerBase {
	public static final String identityTag = "boss_gray_summ_book";
	public static final int detectionRange = 35;
	private static final String name = "AnimatedText";

	public GrayBookSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, name);
	}
}
