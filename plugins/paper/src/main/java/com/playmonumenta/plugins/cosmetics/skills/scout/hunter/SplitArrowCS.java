package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SplitArrowCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SPLIT_ARROW;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	public void splitArrowChain(Player player, LivingEntity sourceEnemy, LivingEntity nearestMob) {
		Location loc = sourceEnemy.getEyeLocation();
		Location eye = nearestMob.getEyeLocation();
		Vector dir = LocationUtils.getDirectionTo(eye, loc);
		for (int j = 0; j < 50; j++) {
			loc.add(dir.clone().multiply(0.1));
			new PartialParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
			if (loc.distance(eye) < 0.4) {
				break;
			}
		}
	}

	public void splitArrowEffect(Player player, LivingEntity nearestMob) {
		Location eye = nearestMob.getEyeLocation();
		World world = player.getWorld();
		new PartialParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6).spawnAsPlayerActive(player);
		world.playSound(eye, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1, 1.2f);
	}
}
