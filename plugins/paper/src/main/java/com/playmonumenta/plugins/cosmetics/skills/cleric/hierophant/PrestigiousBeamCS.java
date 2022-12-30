package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
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
import org.jetbrains.annotations.Nullable;

public class PrestigiousBeamCS extends HallowedBeamCS implements PrestigeCS {

	public static final String NAME = "Prestigious Beam";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
	private static final Particle.DustOptions BURN_COLOR = new Particle.DustOptions(Color.fromRGB(255, 180, 0), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"BEAM_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HALLOWED_BEAM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPECTRAL_ARROW;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void beamHealEffect(World world, Player mPlayer, LivingEntity pe, Vector dir, double range) {
		Location loc = mPlayer.getEyeLocation();
		// Launch sound
		world.playSound(loc, Sound.BLOCK_TRIPWIRE_DETACH, SoundCategory.PLAYERS, 0.6f, 0.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.2f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.3f, 0.5f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.PLAYERS, 0.75f, 0.75f);
		world.playSound(loc, Sound.ITEM_HONEY_BOTTLE_DRINK, 2.5f, 1.75f);

		double distance = loc.distance(pe.getEyeLocation()) - 0.75;
		double beamRadius;
		int units;
		for (double i = 0; i < range; i += 0.75) {
			// Beam effect
			loc.add(dir.clone().multiply(0.75));
			beamRadius = 0.2 * distance * FastUtils.sin(i / distance * 3.1416);
			units = (int) Math.ceil(beamRadius * 6.4);
			ParticleUtils.drawRing(loc, units, dir, beamRadius,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
			);
			ParticleUtils.drawRing(loc, units / 2 + 1, dir, beamRadius + 0.1,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
			);

			// Sound near target
			if (loc.distance(pe.getEyeLocation()) < 1.25) {
				loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 1.5f);
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);
				break;
			}
		}

		// Effect on target
		new PartialParticle(Particle.SPELL_INSTANT, pe.getLocation(), 500, 2.5, 0.15, 2.5, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, pe.getLocation(), 25, 1.0, 0.15, 1.0, 0.1).spawnAsPlayerActive(mPlayer);
		for (int i = 0; i < 5; i++) {
			final double healEffectRadius = i * 1.25;
			final int healRingUnits = i * 16;
			new BukkitRunnable() {
				@Override
				public void run() {
					ParticleUtils.drawRing(pe.getLocation().clone().add(0, 0.125, 0), healRingUnits, new Vector(0, 1, 0), healEffectRadius,
						(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0, 0.1, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
					);
				}
			}.runTaskLater(Plugin.getInstance(), i / 2);
		}
	}

	@Override
	public void beamHarm(World world, Player mPlayer, LivingEntity e, Vector dir, double range) {
		Location loc = mPlayer.getEyeLocation();
		world.playSound(loc, Sound.BLOCK_TRIPWIRE_DETACH, SoundCategory.PLAYERS, 0.6f, 0.75f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.2f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.8f, 0.5f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.PLAYERS, 0.45f, 0.75f);

		for (double i = 0; i < range; i += 0.75) {
			loc.add(dir);
			for (int j = 0; j < 3; j++) {
				final double theta = i * 3.1416 * 0.25 + j * 3.1416 * 0.667;
				ParticleUtils.drawCurve(loc, 0, 0, dir.clone().multiply(0.6),
					t -> 0,
					t -> FastUtils.cos(theta),
					t -> FastUtils.sin(theta),
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 4, 0.1, 0.1, 0.1, 0, BURN_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawCurve(loc, 0, 0, dir.clone().multiply(0.6),
					t -> 0,
					t -> FastUtils.cos(theta),
					t -> FastUtils.sin(theta),
					(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0, 0, 0, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
				);
			}
			if (loc.distance(e.getEyeLocation()) < 1.25) {
				loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.25f);
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.25f, 1.25f);
				break;
			}
		}
	}

	@Override
	public void beamHarmCrusade(Player mPlayer, Location eLoc) {
		new PartialParticle(Particle.SPIT, eLoc, 30, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 60, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, eLoc, 100, 1.25, 1, 1.25, 0.5, BURN_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void beamHarmOther(Player mPlayer, Location eLoc) {
		new PartialParticle(Particle.SPIT, eLoc, 30, 0, 0, 0, 0.25f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, eLoc, 30, 1, 1, 1, 0.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, eLoc, 75, 1.25, 1, 1.25, 0.5, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
	}
}
