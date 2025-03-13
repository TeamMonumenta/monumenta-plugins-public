package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BladeDanceCS implements CosmeticSkill {
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(110, 0, 100), 1.0f);
	private final Color ACCENT_COLOR = Color.fromRGB(0x29073c);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BLADE_DANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRING;
	}

	public void danceStart(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.6f, 0.1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 2.0f, 1.0f);
		Location modifiedLoc = loc.clone().add(0, 1, 0);
		new PartialParticle(Particle.VILLAGER_ANGRY, modifiedLoc, 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(player);
	}

	public void danceTick(Player player, World world, Location loc, int tick, int duration, double danceRadius) {
		loc.setPitch(0);
		float pitch = 0.5f + (tick % 2 == 0 ? tick : tick - 1) * 0.1f / 2.0f;

		Location loc2 = LocationUtils.getHalfHeightLocation(player);
		loc2.setPitch(0);
		if (tick % 2 == 0) {
			double multiplier = (double) (duration - tick) / duration * 0.7 + 0.3;
			if (tick % 4 == 0) {
				ParticleUtils.drawHalfArc(loc2, danceRadius * multiplier, FastUtils.randomDoubleInRange(-10, 10), 0, 180, 2, 0.25, false, 60 * (tick + 2) / duration, (location, ring, angleProgress) -> {
					new PartialParticle(Particle.REDSTONE, location)
						.data(new Particle.DustOptions(ParticleUtils.getTransition(Color.fromRGB(0x999999), ACCENT_COLOR, (double) ring / 8 + (double) tick / duration), 0.8f))
						.spawnAsPlayerActive(player);
				});
			} else if (tick % 4 == 2) {
				ParticleUtils.drawHalfArc(loc2, danceRadius * multiplier, FastUtils.randomDoubleInRange(-10, 10), 180, 360, 2, 0.25, false, 60 * (tick + 2) / duration, (location, ring, angleProgress) -> {
					new PartialParticle(Particle.REDSTONE, location)
						.data(new Particle.DustOptions(ParticleUtils.getTransition(Color.fromRGB(0x999999), ACCENT_COLOR, (double) ring / 8 + (double) tick / duration), 0.8f))
						.spawnAsPlayerActive(player);
				});
			}
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, pitch);
			world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.4f, 2.0f);
			world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 0.6f);
		}
	}

	public void danceEnd(Player player, World world, Location loc, double danceRadius) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 1.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.2f, 1.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.6f, 2.0f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.8f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2.4f, 0.7f);

		new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(player);

		new PPCircle(Particle.CRIT, loc.clone().add(0, 1, 0), danceRadius / 2)
			.count(50)
			.delta(0.5, 0, 1)
			.rotateDelta(true).directionalMode(true)
			.extra(danceRadius / 2)
			.extraVariance(danceRadius / 2)
			.spawnAsPlayerActive(player);

		Location loc2 = LocationUtils.getHalfHeightLocation(player);
		loc2.setPitch(0);


		for (int i = 0; i < 2; i++) {
			ParticleUtils.drawHalfArc(loc2, danceRadius - 1, i == 0 ? -12 : 360 + 12, i * 180 + 90, 450 + i * 180, 8, 0.25, false, 60, (location, ring, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, location)
					.data(new Particle.DustOptions(ParticleUtils.getTransition(ACCENT_COLOR, Color.GRAY, angleProgress), 1.1f))
					.spawnAsPlayerActive(player);
			});
		}
	}

	public void danceHit(Player player, World world, LivingEntity mob, Location mobLoc) {
		world.playSound(mobLoc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(mobLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		new PartialParticle(Particle.SWEEP_ATTACK, mobLoc, 5, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(player);
	}
}
