package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTffBookSummon;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/* TODO: Merge this with SpawnMobsBoss */
public class TffBookSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tffbooksummon";
	public static final int detectionRange = 30;

	public TffBookSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = List.of(
			new SpellTffBookSummon(plugin, boss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
