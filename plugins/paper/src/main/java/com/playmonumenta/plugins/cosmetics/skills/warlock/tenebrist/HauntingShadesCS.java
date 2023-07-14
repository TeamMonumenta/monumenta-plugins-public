package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
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

public class HauntingShadesCS implements CosmeticSkill {

	private static final String AS_NAME = "HauntingShade";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HAUNTING_SHADES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	public String getAsName() {
		return AS_NAME;
	}

	public void shadesStartSound(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_RAVAGER_STUNNED, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.3f, 1.0f);
	}

	public void shadesTrailParticle(Player player, Location bLoc, Vector dir, double distance) {
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, bLoc, 16, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsPlayerActive(player);
	}

	public void shadesTickEffect(Plugin plugin, World world, Player player, Location bLoc, double mAoeRadius, int mT) {
		if (mT % 10 == 0) {
			new BukkitRunnable() {
				double mRadius = 0;
				final Location mLoc = bLoc.clone().add(0, 0.15, 0);

				@Override
				public void run() {
					mRadius += 1.25;
					new PPCircle(Particle.REDSTONE, mLoc, mRadius).count(36).delta(0.2).extra(0.1).data(COLOR).spawnAsPlayerActive(player);
					new PPCircle(Particle.SMOKE_NORMAL, mLoc, mRadius).count(12).extra(0.15).spawnAsPlayerActive(player);
					if (mRadius >= mAoeRadius + 1) {
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		}

		if (mT % 20 == 0) {
			AbilityUtils.playPassiveAbilitySound(bLoc, Sound.ENTITY_PHANTOM_DEATH, 0.3f, 0.5f);
			AbilityUtils.playPassiveAbilitySound(bLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 0.5f);
		}
	}

	public void shadesEndEffect(World world, Player player, Location bLoc, double radius) {
		new PartialParticle(Particle.REDSTONE, bLoc, 45, 0.2, 1.1, 0.2, 0.1, COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 40, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(player);
		world.playSound(bLoc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.6f, 0.1f);
		world.playSound(bLoc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(bLoc, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 0.6f, 0.1f);
	}
}

