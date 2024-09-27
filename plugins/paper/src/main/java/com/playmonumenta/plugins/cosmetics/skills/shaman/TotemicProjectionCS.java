package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class TotemicProjectionCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TOTEMIC_PROJECTION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_PEARL;
	}

	public void projectionCollision(Player player, Location dropCenter) {
		player.playSound(dropCenter, Sound.BLOCK_VINE_BREAK,
			SoundCategory.PLAYERS, 2.0f, 1.0f);
		new PartialParticle(Particle.REVERSE_PORTAL, dropCenter, 20).spawnAsPlayerActive(player);
		player.getWorld().playSound(dropCenter, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,
			SoundCategory.PLAYERS, 1.0f, 1.7f);
	}

	public void projectionAOE(Player player, Location dropCenter, double radius) {
		new PPCircle(Particle.REVERSE_PORTAL, dropCenter, radius).ringMode(false).countPerMeter(4).spawnAsPlayerActive(player);
	}
}
