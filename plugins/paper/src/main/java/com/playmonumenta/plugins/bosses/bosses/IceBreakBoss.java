package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellIceBreak;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class IceBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_icebreak";
	public static final int detectionRange = 40;

	public IceBreakBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = List.of(new SpellIceBreak(boss));

		boss.setRemoveWhenFarAway(false);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
