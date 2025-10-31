package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AmplifyingHexCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.AMPLIFYING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void onCast(Player player, double radius, double angle) {
		ParticleUtils.explodingConeEffectSkill(Plugin.getInstance(), player, (float) radius, Particle.SPELL_WITCH, 0.8f, Particle.SMOKE_NORMAL, 0.35f, Math.cos(Math.toRadians(angle)), player);
		Location pLoc = player.getLocation().add(0, 0.2, 0);
		Vector direction = pLoc.getDirection();
		direction.setY(0).normalize();
		Vector vec1 = VectorUtils.rotateYAxis(direction, angle);
		Vector vec2 = VectorUtils.rotateYAxis(direction, -angle);
		Vector dir1 = VectorUtils.rotateYAxis(direction, 90).multiply(angle / 100);
		Vector dir2 = VectorUtils.rotateYAxis(direction, -90).multiply(angle / 100);
		// If it's so small this ribbon effect is gonna be too big
		if (angle < 25) {
			dir1 = new Vector();
			dir2 = new Vector();
		}

		List<List<Vector>> linesToSpawn = new ArrayList<>(List.of(
			List.of(
				new Vector(),
				vec1.clone().multiply(0.4).add(dir1),
				vec2,
				direction
			),
			List.of(
				new Vector(),
				vec2.clone().multiply(0.4).add(dir2),
				vec1,
				direction
			)
		));
		// if it's so big we can add 2 more lines
		if (angle >= 60) {
			linesToSpawn.add(
				List.of(new Vector(),
					dir2,
					direction,
					vec1

				));
			linesToSpawn.add(
				List.of(new Vector(),
					dir1,
					direction,
					vec2
				));
		}

		for (List<Vector> lines : linesToSpawn) {
			new PPBezier(Particle.SPELL_WITCH, lines.stream().map(vec -> {
				vec = pLoc.toVector().add(vec.clone().multiply(radius));
				return new Location(player.getWorld(), vec.getX(), vec.getY(), vec.getZ());
			}).toList())
				.count((int) (radius * 8))
				.delay((int) (radius + 10))
				.delta(0.5, 0, 0.5)
				.spawnAsPlayerActive(player);
		}

		World world = player.getWorld();
		world.playSound(pLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(pLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.3f, 0.9f);
		world.playSound(pLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(pLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(pLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.3f, 1.4f);
		world.playSound(pLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(pLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.7f);
	}

	public void onHit(Player player, LivingEntity mob) {

	}
}
