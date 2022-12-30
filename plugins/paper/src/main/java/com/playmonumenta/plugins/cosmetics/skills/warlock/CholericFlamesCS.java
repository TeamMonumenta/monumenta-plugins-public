package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class CholericFlamesCS implements CosmeticSkill {

	public static final ImmutableMap<String, CholericFlamesCS> SKIN_LIST = ImmutableMap.<String, CholericFlamesCS>builder()
		.put(InfernalFlamesCS.NAME, new InfernalFlamesCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.CHOLERIC_FLAMES;
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CHARGE;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void flameEffects(Player player, World world, Location loc, double range) {
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.35f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.35f);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 30, 0, 0, 0, 0.15).spawnAsPlayerActive(player);

		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = player.getLocation().add(0, 0.15, 0);

			@Override
			public void run() {
				mRadius += 1.25;
				new PPCircle(Particle.FLAME, mLoc, mRadius).ringMode(true).count(40).extra(0.125).spawnAsPlayerActive(player);
				new PPCircle(Particle.SOUL_FIRE_FLAME, mLoc, mRadius).ringMode(true).count(40).extra(0.125).spawnAsPlayerActive(player);
				new PPCircle(Particle.SMOKE_NORMAL, mLoc, mRadius).ringMode(true).count(20).extra(0.15).spawnAsPlayerActive(player);
				if (mRadius >= range + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}
}
