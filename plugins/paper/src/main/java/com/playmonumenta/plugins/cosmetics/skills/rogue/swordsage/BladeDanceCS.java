package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
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

public class BladeDanceCS implements CosmeticSkill {
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

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
		new PartialParticle(Particle.CLOUD, modifiedLoc, 20, 0.25, 0.5, 0.25, 0.15).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, modifiedLoc, 6, 0.45, 0.5, 0.45, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(player);
	}

	public void danceTick(Player player, World world, Location loc, int tick, double danceRadius) {
		float pitch = 0.5f + (tick % 2 == 0 ? tick : tick - 1) * 0.1f / 2.0f;
		double r = danceRadius - (3 * pitch);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 3, r, 2, r, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 4, r, 2, r, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 4, r, 2, r, 0).spawnAsPlayerActive(player);
		if (tick % 2 == 0) {
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
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.4f, 2.0f);

		new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 6, 0.45, 0.5, 0.45, 0).spawnAsPlayerActive(player);

		new BukkitRunnable() {
			int mTicks = 0;
			double mRadians = 0;

			@Override
			public void run() {
				Vector vec = new Vector(FastUtils.cos(mRadians) * danceRadius / 1.5, 0, FastUtils.sin(mRadians) * danceRadius / 1.5);

				Location loc2 = player.getEyeLocation().add(vec);
				new PartialParticle(Particle.SWEEP_ATTACK, loc2, 5, 1, 0.25, 1, 0).spawnAsPlayerActive(player);
				new PartialParticle(Particle.CRIT, loc2, 10, 1, 0.25, 1, 0.3).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, loc2, 10, 1, 0.25, 1, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(player);
				world.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 1.5f);

				if (mTicks >= 5) {
					this.cancel();
				}

				mTicks++;
				mRadians += Math.toRadians(72);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void danceHit(Player player, World world, LivingEntity mob, Location mobLoc) {
		world.playSound(mobLoc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(mobLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		new PartialParticle(Particle.SWEEP_ATTACK, mobLoc, 5, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, mobLoc, 10, 0.25, 0.5, 0.25, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, mobLoc, 15, 0.35, 0.5, 0.35, 0, SWORDSAGE_COLOR).spawnAsPlayerActive(player);
	}
}
