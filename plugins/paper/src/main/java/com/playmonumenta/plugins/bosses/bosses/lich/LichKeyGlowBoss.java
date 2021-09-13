package com.playmonumenta.plugins.bosses.bosses.lich;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellLichKeyGlow;

public class LichKeyGlowBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lich_keyglow";
	public static final int detectionRange = 55;
	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LichKeyGlowBoss(plugin, boss);
	}

	public LichKeyGlowBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellLichKeyGlow(mBoss)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
