package com.playmonumenta.plugins.bosses.bosses.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.hexfall.hycenea.SpellVoodooCommand;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class VoodooTotemBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_voodooTotem";
	public static final int detectionRange = 30;

	public static class Parameters extends BossParameters {
		public int ACTION_DELAY = 5;
		@BossParam(help = "True = circle, False = donut")
		public boolean ACTION_TYPE = true;
		public int DAMAGE = 500;
		public int COOLDOWN = 20 * 15;
		public double CIRCLE_MIN_RAD = 0;
		public double CIRCLE_MAX_RAD = 5;
		public double DONUT_MIN_RAD = 3;
		public double DONUT_MAX_RAD = 12;
	}

	public VoodooTotemBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		VoodooTotemBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new VoodooTotemBoss.Parameters());
		List<Spell> passiveSpells = List.of(
			new SpellVoodooCommand(plugin, boss, p.DAMAGE, p.ACTION_DELAY, p.COOLDOWN, p.ACTION_TYPE ? p.CIRCLE_MIN_RAD : p.DONUT_MIN_RAD, p.ACTION_TYPE ? p.CIRCLE_MAX_RAD : p.DONUT_MAX_RAD)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null, 100, 1);
	}
}
