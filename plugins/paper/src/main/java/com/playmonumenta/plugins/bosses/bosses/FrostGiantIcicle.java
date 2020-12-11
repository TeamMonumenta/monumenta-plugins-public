package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFallingIcicle;

/*
 * Boss exclusively used for the armor stands in the icicles for Frost Giant so that they fall and detect collision.
 * The Frost Giant quest also uses this, and the code can be used anywhere that needs falling icicles that use ice blocks.
 */
public class FrostGiantIcicle extends BossAbilityGroup {
	public static final String identityTag = "boss_frostgianticicle";
	public static final int detectionRange = 60;

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FrostGiantIcicle(plugin, boss);
	}

	public FrostGiantIcicle(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellFallingIcicle(mPlugin, mBoss)
		                            );

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
