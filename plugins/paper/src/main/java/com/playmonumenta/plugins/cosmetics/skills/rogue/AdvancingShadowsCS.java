package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AdvancingShadowsCS implements CosmeticSkill {

	private static final float[] PITCHES = {1.6f, 1.8f, 1.6f, 1.8f, 2f};

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ADVANCING_SHADOWS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	public void tpParticle(Player mPlayer, LivingEntity target) {
		Vector vecFromEntity = mPlayer.getLocation().subtract(target.getLocation()).toVector().normalize();
		Location pLoc = mPlayer.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = pLoc.getDirection().normalize();

		Vector[] axes = {pVec, pVec.clone().crossProduct(mPlayer.getLocation().getDirection())};

		for (int i = 0; i < 12; i++) {
			Vector dir = vecFromEntity.clone().rotateAroundAxis(axes[0], FastUtils.randomDoubleInRange(-Math.PI / 2, Math.PI / 2))
				.rotateAroundAxis(axes[1], FastUtils.randomDoubleInRange(-Math.PI / 2, Math.PI / 2));
			new PartialParticle(Particle.SMOKE_LARGE, mPlayer.getLocation().clone().add(0, 1, 0))
				.delta(dir.getX(), dir.getY() + FastUtils.randomDoubleInRange(-1, 1), dir.getZ())
				.directionalMode(true)
				.extra(0.35)
				.spawnAsPlayerActive(mPlayer);
		}
		new PPCircle(Particle.SPELL_WITCH, mPlayer.getLocation().clone().add(0, 1, 0), 1.5)
			.axes(axes[0], axes[1])
			.count(35)
			.delta(0.1)
			.spawnAsPlayerActive(mPlayer);
	}

	public void tpTrail(Player mPlayer, Location loc, int i) {
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 1.0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.025).spawnAsPlayerActive(mPlayer);
	}

	public void tpSound(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.1f);
	}

	public void tpSoundFail(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.8f);
	}

	public void tpChain(World world, Player mPlayer) {
		for (int i = 0; i < PITCHES.length; i++) {
			float pitch = PITCHES[i];
			new BukkitRunnable() {
				@Override
				public void run() {
					world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1, pitch);
				}
			}.runTaskLater(Plugin.getInstance(), i);
		}
	}
}
