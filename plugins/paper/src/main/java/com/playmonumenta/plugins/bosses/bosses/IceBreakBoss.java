package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellIceBreak;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class IceBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_icebreak";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new IceBreakBoss(plugin, boss);
	}

	public IceBreakBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(new SpellIceBreak(boss));

		boss.setRemoveWhenFarAway(false);
		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
