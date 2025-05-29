package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;

public class SpellAbhorrentHallucinationTeleport extends Spell {
	private final LivingEntity mBoss;
	private final Location mCenterLocation;
	private final int mFloorYLevel;
	private final int mTeleportRange;

	private final SpellCooldownManager mSpellCooldownManager;

	public SpellAbhorrentHallucinationTeleport(LivingEntity boss, Location centerLocation, int teleportRange) {
		mBoss = boss;
		mCenterLocation = centerLocation;
		mFloorYLevel = mCenterLocation.getBlockY() - 1;
		mTeleportRange = teleportRange;
		mSpellCooldownManager = new SpellCooldownManager(60 * 20, 20, boss::isValid, boss::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();
		doTeleport();
	}

	// Teleport without setting the cooldown
	public void doTeleport() {
		Location newLocation = LocationUtils.randomSafeLocationInDonut(mBoss.getLocation(), mTeleportRange, mTeleportRange,
			location -> {
				Location floor = location.clone();
				floor.setY(mFloorYLevel);
				return floor.getBlock().getType() != SpellLiminalCorruption.MATERIAL &&
					!location.getBlock().isSolid() && location.distance(mCenterLocation) <= 25;
			});
		new PPLine(Particle.DUST_COLOR_TRANSITION, mBoss.getLocation().add(0, 0.5, 0), newLocation.clone().add(0, 0.5, 0))
			.countPerMeter(10)
			.data(new Particle.DustTransition(Color.BLACK, Color.fromRGB(0x6b0000), 5.0f))
			.spawnAsBoss();
		mBoss.teleport(newLocation);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 0.1f);
		new PPCircle(Particle.SQUID_INK, mBoss.getLocation(), 0.5)
			.directionalMode(true)
			.rotateDelta(true)
			.delta(1, 0, 0)
			.extra(0.25)
			.count(20)
			.spawnAsBoss();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown();
	}
}
