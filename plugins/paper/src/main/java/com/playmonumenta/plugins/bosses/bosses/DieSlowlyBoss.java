package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDieSlowly;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class DieSlowlyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dieslowly";
	public static final int detectionRange = 20;

	public static class Parameters extends BossParameters {
		@BossParam(help = "This mob needs a player within this range to die.")
		public int DETECTION = 20;
		@BossParam(help = "How many ticks per pulse.")
		public int TICKS_PER_PULSE = 10;
		@BossParam(help = "How much of its max health it loses every pulse.")
		public double DAMAGE_PERCENTAGE = 0.04;
	}

	Parameters mParameters = new Parameters();

	public DieSlowlyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		BossParameters.getParameters(boss, identityTag, mParameters);
		List<Spell> passiveSpells = List.of(new SpellDieSlowly(mBoss, mParameters));
		super.constructBoss(SpellManager.EMPTY, passiveSpells, mParameters.DETECTION, null, 0, mParameters.TICKS_PER_PULSE);
	}
}
