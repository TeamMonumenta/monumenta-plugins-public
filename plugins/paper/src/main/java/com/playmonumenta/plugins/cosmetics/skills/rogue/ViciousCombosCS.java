package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ViciousCombosCS implements CosmeticSkill {
	private static final Color ACCENT_COLOR = Color.fromRGB(0x29073c);

	private static final int[] ANGLES = {210, 270, 330};
	private int mCombo = 0;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VICIOUS_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ZOMBIE_HEAD;
	}

	public void enhancedCombo(World world, Player player, LivingEntity target) {
		world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS, 0.45f, 1.8f);
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.25f, 1.2f);
	}

	public void comboOnKill(World world, Location loc, Player player, double range, LivingEntity target) {
		world.playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(loc, Sound.ENTITY_STRAY_DEATH, SoundCategory.PLAYERS, 0.2f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.4f, 0.9f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.3f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.3f, 0.7f);

		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0))
			.count(20)
			.delta(0.2, 0.5, 0.2)
			.extra(0.5)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.CRIT, loc.clone().add(0, 1, 0))
			.count(15)
			.delta(0.2, 0.5, 0.2)
			.extra(0.5)
			.spawnAsPlayerActive(player);

		Vector vec = loc.clone().subtract(player.getLocation()).toVector().normalize().multiply(3);
		Location fakeLoc = loc.clone().add(0, 0.5, 0).subtract(vec);
		double[] yawPitch = VectorUtils.vectorToRotation(vec);
		fakeLoc.setYaw((float) yawPitch[0]);
		fakeLoc.setPitch((float) yawPitch[1]);

		ParticleUtils.drawHalfArc(fakeLoc, fakeLoc.distance(loc), ANGLES[mCombo], 60, 120, 3, 0.5, false, 40, (location, rings, angleProgress) -> {
			new PartialParticle(Particle.REDSTONE, location)
				.data(new Particle.DustOptions(ParticleUtils.getTransition(ACCENT_COLOR, Color.fromRGB(0xaaaaaa), angleProgress), 1.0f))
				.spawnAsPlayerActive(player);
		});
		mCombo++;
		mCombo %= 3;
	}

	public void comboOnElite(World world, Location loc, Player player, double range, LivingEntity target) {
		world.playSound(loc, Sound.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(loc, Sound.ENTITY_STRAY_DEATH, SoundCategory.PLAYERS, 0.3f, 1.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.7f, 0.9f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.4f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.3f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.3f, 2.0f);

		Vector vec = loc.clone().subtract(player.getLocation()).toVector().normalize().multiply(3);
		Location fakeLoc = loc.clone().add(0, 0.5, 0).subtract(vec);
		double[] yawPitch = VectorUtils.vectorToRotation(vec);
		fakeLoc.setYaw((float) yawPitch[0]);
		fakeLoc.setPitch((float) yawPitch[1]);

		new PPCircle(Particle.CRIT, loc.clone().add(0, 0.25, 0), 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.9, 0, 0)
			.extra(range / 3)
			.countPerMeter(12)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0))
			.count(20)
			.delta(0.5)
			.extra(1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0))
			.count(100)
			.delta(range / 2)
			.spawnAsPlayerActive(player);


		for (int i = 0; i < 2; i++) {
			ParticleUtils.drawHalfArc(fakeLoc, fakeLoc.distance(loc), i == 0 ? -20 : 180 + 20, 30, 170, 8, 0.2, false, 40,
				(location, ring, angleProgress) -> {
					new PartialParticle(Particle.REDSTONE, location)
						.data(new Particle.DustOptions(ParticleUtils.getTransition(ACCENT_COLOR, Color.WHITE, angleProgress), 0.8f + (float) angleProgress * ring / 8 * 0.6f))
						.spawnAsPlayerActive(player);
				});
		}
	}
}
