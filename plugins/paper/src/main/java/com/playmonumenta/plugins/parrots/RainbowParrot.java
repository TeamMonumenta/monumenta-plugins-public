package com.playmonumenta.plugins.parrots;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.plugin.Plugin;

public class RainbowParrot extends BossAbilityGroup {
	public static final String identityTag = "RainbowParrot";


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RainbowParrot(plugin, boss);
	}

	public RainbowParrot(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new RainbowParrotPassive((Parrot) boss, 1)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, 20, null, 20);
	}
}
