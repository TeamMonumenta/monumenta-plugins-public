package com.playmonumenta.plugins.bosses.bosses.lich;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellLichCurse;

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
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			event.getDamager().remove();
			Player p = (Player) event.getEntity();
			Lich.cursePlayer(mPlugin, p, 120);
		}
	}
}
