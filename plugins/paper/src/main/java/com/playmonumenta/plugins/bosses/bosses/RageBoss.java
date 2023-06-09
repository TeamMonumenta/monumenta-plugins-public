package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class RageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rage";
	public static final int detectionRange = 20;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Size of Rage Buff. Default 12 blocks.")
		public int RADIUS = 12;
		@BossParam(help = "Amount of Ticks until takes effect. Default 30 ticks.")
		public int DELAY = 30;
		@BossParam(help = "Amount of Ticks between Rage casts. Default 160 ticks (8 seconds)")
		public int COOLDOWN = 8 * 20;

		@BossParam(help = "Duration of Buffs in ticks. Default 120 ticks (6 seconds)")
		public int BUFF_DURATION = 6 * 20;
		@BossParam(help = "Power of strength modifier. Default 0.2 (+20% damage)")
		public double STRENGTH_POWER = 0.2;
		@BossParam(help = "Power of speed modifier. Default 0.2 (+20% speed)")
		public double SPEED_POWER = 0.2;
		@BossParam(help = "Power of KBResist. Default 0.0 (0 KBR)")
		public double KBR_POWER = 0.0;

		@BossParam(help = "Buff mobs with CCImmune. Defaults false.")
		public boolean CC_IMMUNE_BUFF = false;
	}

	public RageBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellRage(plugin, boss, p.RADIUS, p.DELAY, p.COOLDOWN, p.BUFF_DURATION, p.STRENGTH_POWER, p.SPEED_POWER, p.KBR_POWER, p.CC_IMMUNE_BUFF);

		super.constructBoss(spell, detectionRange);
	}
}
