package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TwistedDespairBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_twisteddespair";
	public static final int detectionRange = 20;

	public static final int TP_BEHIND_COOLDOWN = 20 * 8;
	public static final int LIFE_TIME = 20 * 28;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TwistedDespairBoss(plugin, boss);
	}

	public TwistedDespairBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellTpBehindPlayer(plugin, boss, TP_BEHIND_COOLDOWN)));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!boss.isDead() && boss.isValid()) {
					boss.setHealth(0);
				}
			}
		}.runTaskLater(plugin, LIFE_TIME);
	}

}
