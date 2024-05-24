package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ThunderStepCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.THUNDER_STEP;
	}

	@Override
	public Material getDisplayItem() {
		return Material.HORN_CORAL;
	}

	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	public void castEffect(Player player, double ratio) {
		Location location = player.getLocation();
		World world = location.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 1.5f);
		new PartialParticle(Particle.REDSTONE, location, (int) (100 * ratio * ratio), 2.5 * ratio, 2.5 * ratio, 2.5 * ratio, 3, COLOR_YELLOW).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, location, (int) (100 * ratio * ratio), 2.5 * ratio, 2.5 * ratio, 2.5 * ratio, 3, COLOR_AQUA).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10)
			.minimumCount(1).spawnAsPlayerActive(player);
	}

	public void onDamage(Player player, LivingEntity enemy, int mobParticles) {
			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			new PartialParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(player);
			new PartialParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(player);
		}
}
