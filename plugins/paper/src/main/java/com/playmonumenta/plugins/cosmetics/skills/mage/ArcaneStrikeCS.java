package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ArcaneStrikeCS implements CosmeticSkill {
	private static final Particle.DustOptions COLOR_1 = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final Particle.DustOptions COLOR_2 = new Particle.DustOptions(Color.fromRGB(217, 122, 255), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ARCANE_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_SWORD;
	}

	public void onStrike(Plugin plugin, Player player, World world, Location enemyLoc, Location playerLoc, double radius) {
		new PartialParticle(Particle.DRAGON_BREATH, enemyLoc, 75, 0, 0, 0, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_NORMAL, enemyLoc, 35, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_WITCH, enemyLoc, 150, 2.5, 1, 2.5, 0.001).spawnAsPlayerActive(player);
		world.playSound(enemyLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.4f);
		world.playSound(enemyLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.4f);

		world.playSound(playerLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.0f);
		world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(playerLoc, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(playerLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.7f, 2.0f);
		world.playSound(playerLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(playerLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.0f, 1.7f);

		new BukkitRunnable() {
			double mD = 30;

			@Override
			public void run() {
				Vector vec;
				for (double degree = mD; degree < mD + 30; degree += 8) {
					double radian1 = Math.toRadians(degree);
					double cos = FastUtils.cos(radian1);
					double sin = FastUtils.sin(radian1);
					for (double r = 1; r < radius; r += 0.5) {
						vec = new Vector(cos * r, 1, sin * r);
						vec = VectorUtils.rotateXAxis(vec, playerLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, playerLoc.getYaw());

						Location l = playerLoc.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_1).spawnAsPlayerActive(player);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_2).spawnAsPlayerActive(player);
					}
				}
				mD += 30;
				if (mD >= 150) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}
}
