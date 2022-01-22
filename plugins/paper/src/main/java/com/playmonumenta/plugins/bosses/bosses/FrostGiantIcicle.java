package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFallingIcicle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/*
 * Boss exclusively used for the armor stands in the icicles for Frost Giant so that they fall and detect collision.
 * The Frost Giant quest also uses this, and the code can be used anywhere that needs falling icicles that use ice blocks.
 */
public class FrostGiantIcicle extends BossAbilityGroup {
	public static final String identityTag = "boss_frostgianticicle";
	public static final int detectionRange = 60;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FrostGiantIcicle(plugin, boss);
	}

	public FrostGiantIcicle(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellFallingIcicle(mPlugin, boss)
		                            );

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
