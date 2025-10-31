package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
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
import org.jetbrains.annotations.Nullable;

public class EssenceBurstCS extends ChallengeCS {

	public static final String NAME = "Essence Burst";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The world grants you its vigor.",
			"All that remains is to take the swing.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIAMOND;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void onCast(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.35f, 1.4f);
		world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.PLAYERS, 1.8f, 1.0f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.6f, 1f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.6f, 1.6f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.3f, 1.1f);

		new PartialParticle(Particle.EXPLOSION_NORMAL, loc).count(50).delta(0).extra(0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(player);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				final Location loc = player.getLocation().add(0, 1, 0);

				for (int i = 0; i < 3; i++) {
					new BukkitRunnable() {
						int mTicks2 = 0;
						final Vector mDir = VectorUtils.randomUnitVector();

						@Override
						public void run() {

							Particle.DustOptions mCyan = new Particle.DustOptions(Color.fromRGB(50 * mTicks2, 255 - 4 * mTicks2, 235), (float) (2.5 - 0.3 * mTicks2));
							new PartialParticle(Particle.REDSTONE, loc.clone().add(mDir.clone().multiply(4.2 - 0.6 * mTicks2)), 3, 0.12, 0.12, 0.12).data(mCyan).spawnAsPlayerActive(player);
							new PartialParticle(Particle.GLOW, loc.clone().add(mDir.clone().multiply(4.2 - 0.5 * mTicks2)), 1, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(player);
							new PartialParticle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(mDir.clone().multiply(4.2 - 0.5 * mTicks2)), 1, (double) mCyan.getColor().getRed() / 255, (double) mCyan.getColor().getGreen() / 255, (double) mCyan.getColor().getBlue() / 255, 1).directionalMode(true).spawnAsPlayerActive(player);

							mTicks2++;
							if (mTicks2 >= 5) {
								this.cancel();
							}
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 1);
				}

				mTicks++;
				if (mTicks >= 7) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 3);

		loc = LocationUtils.fallToGround(loc, 0);
		loc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(player, loc.clone().add(0, 0.15, 0), 0, 4, 0, 0,
			20, -0.22f, false, 0, 0.1, Particle.SOUL_FIRE_FLAME);

	}

	@Override
	public void onCastEffect(Player player, World world, Location loc) {
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc).count(5).delta(1).extra(0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, loc).count(6).delta(1).extra(0.1).spawnAsPlayerActive(player);
	}

	@Override
	public void killMob(Player player, World world, Location loc) {
		player.playSound(loc, Sound.ENTITY_BREEZE_SLIDE, SoundCategory.PLAYERS, 0.8f, 1.0f);
		new PartialParticle(Particle.GLOW, loc, 10, 0.6, 0.6, 0.6, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB_AMBIENT, loc, 5, 0.6, 0.8, 0.9, 1).directionalMode(true).spawnAsPlayerActive(player);
	}

	@Override
	public void maxMobs(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_GENERIC_WIND_BURST, SoundCategory.PLAYERS, 1.4f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.5f, 1.8f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc).count(40).delta(0).extra(0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, loc, 10, 0.6, 0.6, 0.6, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB_AMBIENT, loc, 10, 0.6, 0.8, 0.9, 1).directionalMode(true).spawnAsPlayerActive(player);

		loc = LocationUtils.fallToGround(loc, 0);
		loc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(player, loc.clone().add(0, 0.15, 0), 0, 0.5, 0, 0,
			20, 0.22f, false, 0, 0.1, Particle.SOUL_FIRE_FLAME);

	}
}
