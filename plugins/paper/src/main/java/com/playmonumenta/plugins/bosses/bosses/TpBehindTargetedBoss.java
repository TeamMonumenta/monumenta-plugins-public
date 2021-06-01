package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;

/**
 * @deprecated
 * use boss_tpbehind instead, like this:
 * <blockquote><pre>
 * /boss var Tags add boss_tpbehind
 * /boss var Tags add boss_tpbehind[random=false,range=50]
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class TpBehindTargetedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpbehindtargeted";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TpBehindTargetedBoss(plugin, boss);
	}

	public TpBehindTargetedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTpBehindPlayer(plugin, boss, 240)));


		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
