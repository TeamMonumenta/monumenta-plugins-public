package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindTargetedPlayer;

public class TwistedDespairBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_twisteddespair";
	public static final int detectionRange = 20;

	public static final int TP_BEHIND_COOLDOWN = 20 * 8;
	public static final int LIFE_TIME = 20 * 28;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TwistedDespairBoss(plugin, boss);
	}

	public TwistedDespairBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellTpBehindTargetedPlayer(plugin, boss, TP_BEHIND_COOLDOWN)));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isDead() && mBoss.isValid()) {
					mBoss.setHealth(0);
				}
			}
		}.runTaskLater(plugin, LIFE_TIME);
	}

}
