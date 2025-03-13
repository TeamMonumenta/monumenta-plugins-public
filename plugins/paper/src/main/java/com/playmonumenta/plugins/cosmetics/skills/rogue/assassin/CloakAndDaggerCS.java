package com.playmonumenta.plugins.cosmetics.skills.rogue.assassin;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.StealthCosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
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

public class CloakAndDaggerCS implements StealthCosmeticSkill {
	private static final int[] ANGLES = {0, 180};
	private static final double[] OFFSET_X = {0.5, -0.5};
	private static final Color ACCENT_COLOR = Color.fromRGB(0x29073c);

	private int mCombo = 0;
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CLOAK_AND_DAGGER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	public void castEffects(Player mPlayer) {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1, 1);
		new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 30, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
	}

	public void activationEffects(Player mPlayer, LivingEntity enemy) {
		Location loc = enemy.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.5f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 25, 0.25, 0.5, 0.25, 0.2f).spawnAsPlayerActive(mPlayer);

		loc.add(0, 1, 0);

		Location pLoc = mPlayer.getLocation().add(0, 1, 0);
		Vector dir = LocationUtils.getDirectionTo(loc, pLoc.clone().subtract(0, 0.5, 0)).multiply(3.6);
		loc.setDirection(dir);
		Vector vec = VectorUtils.rotateYAxis(dir, 90).normalize().setY(0).multiply(OFFSET_X[mCombo] * 3);
		ParticleUtils.drawHalfArc(loc.clone().subtract(dir).add(vec), 3.2, ANGLES[mCombo], 20, 100, 4, 0.35, false, 45,
			(location, ring, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, location)
					.data(new Particle.DustOptions(ParticleUtils.getTransition(ACCENT_COLOR, Color.fromRGB(0x6b0000), angleProgress), 1.1f))
					.spawnAsPlayerActive(mPlayer);
		});

		mCombo++;
		mCombo %= 2;
	}
}
