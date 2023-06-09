package com.playmonumenta.plugins.parrots;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.plugin.Plugin;

public class RainbowParrot extends BossAbilityGroup {
	public static final String identityTag = "RainbowParrot";

	public RainbowParrot(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = List.of(
			new RainbowParrotPassive((Parrot) boss, 1)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, 20, null, 20);
	}
}
