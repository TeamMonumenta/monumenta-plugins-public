package com.playmonumenta.plugins.bosses.bosses.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellSourcelessGaze;
import org.bukkit.entity.LivingEntity;

public class SourcelessGazeBoss extends BossAbilityGroup {
	public static String identityTag = "boss_sourcelessgaze";

	public static class Parameters extends BossParameters {
		@BossParam(help = "duration before the gaze ends and the faceless one is summoned.")
		public int GAZE_DURATION = 20 * 5;
		@BossParam(help = "los name of the summoned mob")
		public String SUMMON_NAME = "FacelessOne";
	}

	public SourcelessGazeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters parameters = BossParameters.getParameters(boss, identityTag, new Parameters());
		constructBoss(new SpellSourcelessGaze(plugin, boss, parameters), 100);
	}
}
