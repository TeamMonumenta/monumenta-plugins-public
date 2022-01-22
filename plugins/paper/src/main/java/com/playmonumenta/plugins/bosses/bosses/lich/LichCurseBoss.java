package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellLichCurse;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class LichCurseBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lichcurse";
	public static final int detectionRange = 55;
	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LichCurseBoss(plugin, boss);
	}

	public LichCurseBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new SpellLichCurse(mBoss)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			event.getDamager().remove();
			Lich.cursePlayer(mPlugin, player, 120);
		}
	}
}
