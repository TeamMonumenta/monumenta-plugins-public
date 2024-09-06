package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class SpellToxicFumes extends Spell {

	private static final String ABILITY_NAME = "Toxic Fumes";
	private final int mRadius;
	private final int mRange;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final int mDamageInterval;
	private int mT;

	public SpellToxicFumes(int radius, int range, LivingEntity boss, Location spawnLoc, int damageInterval) {
		mRadius = radius;
		mRange = range;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mDamageInterval = damageInterval;
	}

	@Override
	public void run() {
		if (mT++ % mDamageInterval == 0) {
			for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
				if (LocationUtils.xzDistance(player.getLocation(), mSpawnLoc) > mRadius && player.getEyeLocation().getY() < mSpawnLoc.getY() - 1) {

					PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME);
					PlayerUtils.playerTeleport(player, mSpawnLoc.clone().add(0, 8, 0));

					new PPExplosion(Particle.SLIME, player.getLocation())
						.count(10)
						.delta(1)
						.spawnAsBoss();

					player.playSound(player, Sound.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.HOSTILE, 0.2f, 1f);
					player.playSound(player, Sound.BLOCK_SLIME_BLOCK_FALL, SoundCategory.HOSTILE, 0.2f, 1f);
				}
			}

			for (LivingEntity entity : EntityUtils.getNearbyMobs(mSpawnLoc, mRange)) {
				if ((EntityUtils.isHostileMob(entity) || entity instanceof Wolf) && LocationUtils.xzDistance(entity.getLocation(), mSpawnLoc) > mRadius && entity.getLocation().getY() < mSpawnLoc.getY() - 3 && !entity.getScoreboardTags().contains("Boss")) {
					DamageUtils.damage(null, entity, DamageEvent.DamageType.TRUE, 50000, null, false, false, ABILITY_NAME);

					new PPExplosion(Particle.SLIME, entity.getLocation())
						.count(10)
						.delta(1)
						.spawnAsBoss();
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

