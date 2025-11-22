package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpellVoidCrystalTeleportPassive extends Spell {

	private final Vesperidys mVesperidys;
	private final LivingEntity mBoss;

	private int mCooldownTicks = 0;

	public SpellVoidCrystalTeleportPassive(Vesperidys vesperidys, LivingEntity boss) {
		mVesperidys = vesperidys;
		mBoss = boss;
	}

	@Override
	public void run() {
		if (mCooldownTicks > 0) {
			mCooldownTicks -= 5;
			return;
		}

		// If its platform exploded.
		Vesperidys.Platform platform = mVesperidys.mPlatformList.getPlatformNearestToEntity(mBoss);
		if (platform != null && platform.getCenter().distance(mBoss.getLocation()) > 3) {
			teleportRandomly();
		} else {
			for (LivingEntity le : EntityUtils.getNearbyMobs(mBoss.getLocation(), 3, mBoss)) {
				if (EntityUtils.isBoss(le)) {
					teleportRandomly();
					break;
				}
			}
		}
	}

	public void teleportRandomly() {
		mCooldownTicks = 5 * 20;

		List<Vesperidys.Platform> platforms = mVesperidys.mPlatformList.getShuffledPlatforms(null);
		Vesperidys.Platform selectedPlatform = null;

		// Prioritizes platforms which doesn't have Boss mobs (Void Magus, Void Crystal and Vesperidys) on it.
		for (Vesperidys.Platform platform : platforms) {
			selectedPlatform = platform;
			if (platform.getMechsOnPlatform().isEmpty() && platform.getPlayersOnPlatform().isEmpty()) {
				break;
			}
		}

		Location newLoc;
		if (selectedPlatform == null) {
			// Failsafe. Should not happen, ever.
			newLoc = mVesperidys.mSpawnLoc.clone().add(0, 1, 0);
		} else {
			newLoc = selectedPlatform.getCenter().clone().add(0, 1, 0);
		}
		Location bossLoc = mBoss.getLocation();

		for (int i = 0; i < 50; i++) {
			Vector vec = LocationUtils.getDirectionTo(newLoc, bossLoc);
			Location pLoc = mBoss.getEyeLocation();
			pLoc.add(vec.multiply(i * 0.5));
			if (pLoc.distance(mBoss.getEyeLocation()) > newLoc.distance(bossLoc)) {
				break;
			}
			new PartialParticle(Particle.VILLAGER_ANGRY, pLoc, 1, 0, 0, 0, 0).spawnAsBoss();
		}

		mBoss.getWorld().playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
		mBoss.getWorld().playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);

		mBoss.teleport(newLoc);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
