package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class TwistedDespairBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_twisteddespair";
	public static final int detectionRange = 20;

	public static final int TP_BEHIND_COOLDOWN = 20 * 8;
	public static final int LIFE_TIME = 20 * 28;

	public TwistedDespairBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellTpBehindPlayer(plugin, boss, TP_BEHIND_COOLDOWN);

		super.constructBoss(spell, detectionRange);

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (!boss.isDead() && boss.isValid()) {
				boss.setHealth(0);
			}
		}, LIFE_TIME);
	}

}
