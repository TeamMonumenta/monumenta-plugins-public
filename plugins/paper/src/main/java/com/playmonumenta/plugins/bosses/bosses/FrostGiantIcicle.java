package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFallingIcicle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/*
 * Boss exclusively used for the armor stands in the icicles for Frost Giant so that they fall and detect collision.
 * The Frost Giant quest also uses this, and the code can be used anywhere that needs falling icicles that use ice blocks.
 */
public class FrostGiantIcicle extends BossAbilityGroup {
	public static final String identityTag = "boss_frostgianticicle";
	public static final int detectionRange = 60;

	public FrostGiantIcicle(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(new SpellFallingIcicle(mPlugin, boss), detectionRange);
	}
}
