package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.CrowdControlImmunity;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBasePassiveAbility;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellPotionCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class LichAlchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lichalch";
	public static final int detectionRange = 20;
	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LichAlchBoss(plugin, boss);
	}

	public LichAlchBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mBoss = boss;
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellPotionCloud(plugin, mBoss)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBasePassiveAbility(20 * 4, new CrowdControlImmunity(mBoss))
		);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, null);
	}
}
