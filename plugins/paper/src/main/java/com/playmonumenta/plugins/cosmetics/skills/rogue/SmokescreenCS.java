package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SmokescreenCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SMOKESCREEN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DEAD_TUBE_CORAL;
	}

	public void smokescreenEffects(Player mPlayer, World world, Location loc, double radius) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PPCircle(Particle.SMOKE_LARGE, loc, radius * mTicks / 4)
					.countPerMeter(8)
					.delta(radius / 6, 0.5, radius / 6)
					.spawnAsPlayerActive(mPlayer);
				mTicks++;
				if (mTicks >= 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.35f);
	}

	public void residualEnhanceEffects(Player mPlayer, World world, Location mCloudLocation, double radius) {
		// Visuals are based off of Hekawt's UndeadRogue Smokescreen Spell
		new PartialParticle(Particle.SMOKE_NORMAL, mCloudLocation, 3, 0.3, 0.05, 0.3, 0.075).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, mCloudLocation, 75, radius * 0.7, 0.2, radius * 0.7, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, mCloudLocation, 2, 0.3, 0.05, 0.3, 0.075).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, mCloudLocation, 30, radius * 0.7, 0.8, radius * 0.7, 0.025).spawnAsPlayerActive(mPlayer);

		AbilityUtils.playPassiveAbilitySound(mCloudLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1, 0.7f);
	}

	public String getProjectileName() {
		return "Smokescreen Projectile";
	}
}
