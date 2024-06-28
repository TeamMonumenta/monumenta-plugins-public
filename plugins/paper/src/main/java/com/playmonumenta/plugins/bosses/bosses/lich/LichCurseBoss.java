package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellLichCurse;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class LichCurseBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lichcurse";
	public static final int detectionRange = 55;

	public LichCurseBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = List.of(
			new SpellLichCurse(mBoss)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (damagee instanceof Player player) {
			mBoss.remove();
			Lich.cursePlayer(player, 120);
		}
	}
}
