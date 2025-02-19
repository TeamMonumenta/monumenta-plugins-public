package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class GenericBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_generic";

	public static class Parameters extends BossParameters {
		public int DETECTION = 40;

		public BossBar.Color BAR_COLOR = BossBar.Color.WHITE;

		public BossBar.Overlay BAR_STYLE = BossBar.Overlay.PROGRESS;

		public boolean BOSS_FOG = true;
	}

	public GenericBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);
		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		BossBarManager bossBar = new BossBarManager(boss, p.DETECTION, p.BAR_COLOR, p.BAR_STYLE, null, p.BOSS_FOG);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), p.DETECTION, bossBar);
	}
}
