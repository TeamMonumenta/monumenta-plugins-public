package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class GenericBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_generic";

	public static class Parameters extends BossParameters {
		public int DETECTION = 40;

		public BarColor BAR_COLOR = BarColor.WHITE;

		public BarStyle BAR_STYLE = BarStyle.SOLID;

		public boolean BOSS_FOG = true;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GenericBoss(plugin, boss);
	}

	public GenericBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);
		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		BossBarManager bossBar = new BossBarManager(plugin, boss, p.DETECTION, p.BAR_COLOR, p.BAR_STYLE, null, p.BOSS_FOG);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), p.DETECTION, bossBar);
	}
}
