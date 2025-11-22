package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.ChargedSpell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ChargeManagerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_charge_manager";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Initial delay in ticks for the first cast of this spell")
		public int DELAY = 20 * 5;

		@BossParam(help = "Period in ticks between the start of the last charge and next start.")
		public int COOLDOWN = 20 * 10;

		@BossParam(help = "Amount of casts of the spell per cooldown")
		public int CHARGE = 1;

		@BossParam(help = "Interval in ticks between charge casts")
		public int CHARGE_INTERVAL = 40;

		@BossParam(help = "Radius in blocks that this boss will check before doing anything")
		public int DETECTION = 24;
	}

	public ChargeManagerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters parameters = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(new ChargedSpell(parameters.CHARGE, parameters.CHARGE_INTERVAL, parameters.COOLDOWN), parameters.DETECTION, null, parameters.DELAY);
	}

}
