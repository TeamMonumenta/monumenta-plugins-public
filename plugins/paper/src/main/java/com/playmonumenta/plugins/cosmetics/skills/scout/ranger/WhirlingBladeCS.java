package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WhirlingBladeCS implements CosmeticSkill {
	private float mStartingAngle = 0;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WHIRLING_BLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	public void onCast(Player player, Location loc, World world) {
		mStartingAngle = player.getLocation().getYaw();
	}

	public void hitMob(Player player, Location loc, World world) {
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.6f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.6f, 1.5f);
		new PartialParticle(Particle.CRIT, loc)
			.count(10)
			.extra(0.4)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.SWEEP_ATTACK, loc)
			.count(2)
			.delta(0.1)
			.spawnAsPlayerActive(player);
	}

	public void tick(Player player, Location bladeLoc, World world, Location loc, double throwRadius, double bladeRadius, int degrees) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.2f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.7f);
		world.playSound(loc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.2f, 1.7f);

		Location location = player.getEyeLocation().add(VectorUtils.rotateYAxis(new Vector(-1, -0.5, 0), mStartingAngle - degrees));
		float pitch = FastUtils.randomFloatInRange(8, 12) * (FastUtils.RANDOM.nextBoolean() ? -1.0f : 0.5f);
		location.setPitch(pitch);
		location.setYaw(mStartingAngle - degrees + 15);

		if (degrees % 60 == 0) {
			final int rings = (int) ((throwRadius + bladeRadius - 1.5) / 0.3);
			ParticleUtils.drawHalfArc(location, 1, 180, -30, 80, rings, 0.3, false, 90,
				(pLoc, ring, angleProgress) -> {
					new PartialParticle(Particle.REDSTONE, pLoc, 1)
						.count(1)
						.data(new Particle.DustOptions(ParticleUtils.getTransition(Color.fromRGB(150, 255, 255), Color.WHITE, Math.min(angleProgress + 0.5 * ring / rings, 1)), 0.7f + 0.8f * (float) angleProgress * ring / rings))
						.spawnAsPlayerActive(player);
				});
		}
	}

	public void end(World world, Location loc, Player player) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.75f);
	}
}
