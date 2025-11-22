package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CleansingRainCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CLEANSING_RAIN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

	public void rainCast(Player player, double mRadius) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.45f, 0.8f);
	}

	public void rainCloud(Player player, double ratio, double mRadius) {
		new PartialParticle(Particle.CLOUD, player.getLocation().add(0, 4, 0), 4, 2.5 * ratio, 0.35, 2.5 * ratio, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WATER_DROP, player.getLocation().add(0, 2, 0), (int) (12 * ratio * ratio), 2.5 * ratio, 2, 2.5 * ratio, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 2, 0), (int) (1 * ratio * ratio), 2 * ratio, 1.5, 2 * ratio, 0.001).spawnAsPlayerActive(player);
	}

	public void rainEnhancement(Player player, double smallRatio, double mRadius) {
		new PartialParticle(Particle.CLOUD, player.getLocation().add(0, 4, 0), (int) (4 * smallRatio * smallRatio), 2.5 * smallRatio, 0.35, 2.5 * smallRatio, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WATER_DROP, player.getLocation().add(0, 2, 0), (int) (12 * smallRatio * smallRatio), 2.5 * smallRatio, 2, 2.5 * smallRatio, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 2, 0), (int) (1 * smallRatio * smallRatio), 2 * smallRatio, 1.5, 2 * smallRatio, 0.001).spawnAsPlayerActive(player);
	}
}
