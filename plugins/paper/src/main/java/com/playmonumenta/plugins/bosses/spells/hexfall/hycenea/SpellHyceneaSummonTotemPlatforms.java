package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class SpellHyceneaSummonTotemPlatforms extends Spell {
	private final int mRange;
	private final int mCooldownTicks;
	private final Location mSpawnLoc;

	public SpellHyceneaSummonTotemPlatforms(int range, int cooldownTicks, Location spawnLoc) {
		mRange = range;
		mCooldownTicks = cooldownTicks;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		for (Entity islandArmorStand : mSpawnLoc.getNearbyEntities(mRange, mRange, mRange).stream().filter(entity -> entity.getScoreboardTags().contains("Hycenea_Island")).toList()) {
			if (islandArmorStand.getNearbyEntities(1, 1, 1).stream().filter(entity -> entity.getScoreboardTags().contains("boss_totemplatform")).toList().isEmpty()) {
				LibraryOfSoulsIntegration.summon(islandArmorStand.getLocation(), islandArmorStand.getScoreboardTags().contains("Hycenea_Island_Life") ? "LifeTotem" : "DeathTotem");
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
