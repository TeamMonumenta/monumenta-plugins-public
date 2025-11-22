package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellGenericCharge;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class Orangyboi extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_orangyboi";
	public static final int detectionRange = 35;

	public Orangyboi(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		Spell spell = new SpellGenericCharge(plugin, mBoss, detectionRange, 15.0F);

		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, null);

		super.constructBoss(spell, detectionRange, bossBar);
	}

	@Override
	public void init() {
		final int baseHealth = 125;
		final int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		final double bossHealth = baseHealth * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);
		EntityUtils.setMaxHealthAndHealth(mBoss, bossHealth);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
