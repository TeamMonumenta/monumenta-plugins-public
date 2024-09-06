package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.DeathImmunity;
import com.playmonumenta.plugins.effects.hexfall.LifeImmunity;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellPassiveTotemicShields extends Spell {

	final Plugin mMonumentaPlugin;
	final List<Entity> mLifeIslands;
	final List<Entity> mDeathIslands;
	final LivingEntity mBoss;
	final Location mSpawnLoc;
	final int mRadius = 6;
	int mT = 0;

	public SpellPassiveTotemicShields(Plugin mMonumentaPlugin, Location mSpawnLoc, LivingEntity mBoss) {
		this.mMonumentaPlugin = mMonumentaPlugin;
		Collection<Entity> nearbyEntities = mSpawnLoc.getNearbyEntities(HyceneaRageOfTheWolf.detectionRange, HyceneaRageOfTheWolf.detectionRange, HyceneaRageOfTheWolf. detectionRange);
		mLifeIslands = nearbyEntities.stream().filter(entity -> entity.getScoreboardTags().contains("Hycenea_Island_Life")).collect(Collectors.toList());
		mDeathIslands = nearbyEntities.stream().filter(entity -> entity.getScoreboardTags().contains("Hycenea_Island_Death")).collect(Collectors.toList());
		this.mSpawnLoc = mSpawnLoc;
		this.mBoss = mBoss;
	}

	@Override
	public void run() {
		for (Entity armorStand : mLifeIslands) {
			if (!armorStand.getScoreboardTags().contains("Hycenea_TotemicDestruction_ShieldActive")) {
				continue;
			}

			for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
				if (LocationUtils.xzDistance(player.getLocation(), armorStand.getLocation()) <= mRadius) {
					mMonumentaPlugin.mEffectManager.addEffect(player, LifeImmunity.GENERIC_NAME, new LifeImmunity(20));
				}
			}

			if (mT % 20 == 0) {
				new PPCircle(Particle.REDSTONE, armorStand.getLocation().clone().add(0, 0.25, 0), mRadius)
					.count(50)
					.data(new Particle.DustOptions(Color.fromRGB(0, 204, 0), 1.65f))
					.ringMode(true)
					.spawnAsBoss();
			}
		}

		for (Entity armorStand : mDeathIslands) {
			if (!armorStand.getScoreboardTags().contains("Hycenea_TotemicDestruction_ShieldActive")) {
				continue;
			}

			for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
				if (LocationUtils.xzDistance(player.getLocation(), armorStand.getLocation()) <= mRadius) {
					mMonumentaPlugin.mEffectManager.addEffect(player, DeathImmunity.GENERIC_NAME, new DeathImmunity(20));
				}
			}

			if (mT % 20 == 0) {
				new PPCircle(Particle.REDSTONE, armorStand.getLocation().clone().add(0, 0.25, 0), mRadius)
					.count(50)
					.data(new Particle.DustOptions(Color.fromRGB(153, 76, 37), 1.65f))
					.ringMode(true)
					.spawnAsBoss();
			}
		}
		mT++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
