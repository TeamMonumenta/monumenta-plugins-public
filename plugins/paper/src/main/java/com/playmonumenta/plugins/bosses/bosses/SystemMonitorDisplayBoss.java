package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSystemMonitorDisplay;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SystemMonitorDisplayBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_system_monitor_display";

	public SystemMonitorDisplayBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		final List<Spell> passiveSpells = List.of(
			new SpellSystemMonitorDisplay(boss)
		);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, 45, null, 1, 20);
	}
}
