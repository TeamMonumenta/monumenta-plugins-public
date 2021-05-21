package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellGenericCharge;

/**
 * @deprecated use boss_charger instead like this:
 * <blockquote><pre>
 * /boss var Tags add boss_charger
 * /boss var Tags add boss_charger[damage=25]
 * </pre></blockquote>
 * @G3m1n1Boy
 *
 */
public class ChargerStrongBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_chargerstrong";
	public static final int detectionRange = 20;

	private static final int DAMAGE = 25;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ChargerStrongBoss(plugin, boss);
	}


	public ChargerStrongBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellGenericCharge(plugin, boss, detectionRange, DAMAGE)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
