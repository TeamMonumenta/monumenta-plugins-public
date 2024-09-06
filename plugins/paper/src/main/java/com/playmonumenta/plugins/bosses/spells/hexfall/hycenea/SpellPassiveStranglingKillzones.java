package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class SpellPassiveStranglingKillzones extends Spell {

	static final String ABILITY_NAME = "Strangling Aura (☠)";
	final List<Entity> mIslandArmorStands;
	final LivingEntity mBoss;
	final Location mSpawnLoc;
	final int mRadius = 8;
	int mT = 0;

	public SpellPassiveStranglingKillzones(Location mSpawnLoc, LivingEntity mBoss) {
		mIslandArmorStands = mSpawnLoc.getNearbyEntities(HyceneaRageOfTheWolf.detectionRange, HyceneaRageOfTheWolf.detectionRange, HyceneaRageOfTheWolf.detectionRange)
			.stream().filter(entity -> entity.getScoreboardTags().contains("Hycenea_Island")).collect(Collectors.toList());
		this.mSpawnLoc = mSpawnLoc;
		this.mBoss = mBoss;
	}

	@Override
	public void run() {
		for (Entity armorStand : mIslandArmorStands) {
			if (!armorStand.getScoreboardTags().contains("Hycenea_StranglingRupture_KillzoneActive")) {
				continue;
			}

			for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
				if (LocationUtils.xzDistance(player.getLocation(), armorStand.getLocation()) <= mRadius) {
					PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME, true, true, true);
				}
			}

			for (LivingEntity entity : EntityUtils.getNearbyMobs(mSpawnLoc, HyceneaRageOfTheWolf.detectionRange)) {
				if ((EntityUtils.isHostileMob(entity) || entity instanceof Wolf) && !entity.getScoreboardTags().contains("Boss") && LocationUtils.xzDistance(entity.getLocation(), armorStand.getLocation()) <= mRadius) {
					DamageUtils.damage(null, entity, DamageEvent.DamageType.TRUE, 50000, null, false, false, ABILITY_NAME);
				}
			}

			new PPCircle(Particle.REDSTONE, armorStand.getLocation().clone().add(0, mT % 20, 0), mRadius)
				.countPerMeter(0.05)
				.data(new Particle.DustOptions(Color.fromRGB(153, 76, 37), 3f))
				.spawnAsBoss();

		}
		mT++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
