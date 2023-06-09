package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * @deprecated use boss_tpbehind instead, like this:
 * <blockquote><pre>
 * /boss var Tags add boss_tpbehind
 * /boss var Tags add boss_tpbehind[random=false,range=50]
 * </pre></blockquote>
 * G3m1n1Boy
 */
@Deprecated
public class TpBehindTargetedBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpbehindtargeted";
	public static final int detectionRange = 20;

	public TpBehindTargetedBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellTpBehindPlayer(plugin, boss, 240);

		super.constructBoss(spell, detectionRange);
	}
}
