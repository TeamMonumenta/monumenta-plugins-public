package com.playmonumenta.plugins.cosmetics.skills.warlock;

import com.playmonumenta.plugins.particle.PPLightning;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class NecromantiaCS extends CursedWoundCS {

	public static final String NAME = "Necromantia";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Life magic, death magic, what's the difference?",
			"A great sorcerer sees both sides of the coin."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.PURPLE_GLAZED_TERRACOTTA;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustTransition BLACK = new Particle.DustTransition(Color.fromRGB(128, 0, 160), Color.fromRGB(32, 0, 32), 1f);
	private static final Particle.DustTransition PURPLE_LARGE = new Particle.DustTransition(Color.fromRGB(255, 192, 255), Color.fromRGB(160, 0, 160), 1.8f);

	@Override
	public void onAttack(Player player, Entity entity) {
		new PPLightning(Particle.DUST_COLOR_TRANSITION, entity.getLocation()).duration(3).init(entity.getHeight(), 1, 0.5).count(1).delta(0.3).data(BLACK).spawnAsPlayerActive(player);
	}

	@Override
	public void onCriticalAttack(World world, Player player, LivingEntity mob, int cooldowns) {
		Location loc = player.getLocation();
		world.playSound(loc, "block.vault.place", SoundCategory.PLAYERS, 1.4f, 0.7f);
		world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 0.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_STEP, SoundCategory.PLAYERS, 1f, 0.75f);
		new PartialParticle(Particle.SMOKE_NORMAL, mob.getLocation(), 2, 0.8, 0.8, 0.8, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void onEffectApplication(Player player, Entity entity) {
		new PartialParticle(Particle.SPELL_WITCH, entity.getLocation(), 3, 0.8, 0.8, 0.8, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void onReleaseStoredEffects(Player player, Entity enemy, double radius) {
		Location eLoc = enemy.getLocation();
		World world = enemy.getWorld();
		world.playSound(eLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.2f, 1.18f);
		world.playSound(eLoc, "block.vault.break", SoundCategory.PLAYERS, 1.6f, 0f);
		world.playSound(eLoc, "block.vault.open_shutter", SoundCategory.PLAYERS, 1.5f, 0.94f);

		double height = Math.min(5, enemy.getHeight() + 3);
		new PPLightning(Particle.DUST_COLOR_TRANSITION, eLoc).duration(3).init(height, 1, 0.5).count((int) height).delta(0.1).data(PURPLE_LARGE).spawnAsPlayerActive(player);
		for (int i = -1; i < 2; i += 2) {
			for (int j = -1; j < 2; j += 2) {
				Vector dir = new Vector(i, 0, j);
				Location location = eLoc.clone().add(0, 0.5, 0).setDirection(dir);
				ParticleUtils.drawHalfArc(location, radius / 3, 190, 90, 270, 1, 0,
					(Location l, int ring) ->
						new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, FastUtils.roundRandomly(0.8), 0.05, 0.05, 0.05, 0).data(BLACK).spawnAsPlayerActive(player));
				ParticleUtils.drawHalfArc(location, radius * 2 / 3, 200, 100, 290, 1, 0,
					(Location l, int ring) ->
						new PartialParticle(Particle.SPELL_WITCH, l, FastUtils.roundRandomly(0.5), 0, 0, 0, 0).spawnAsPlayerActive(player));
				ParticleUtils.drawHalfArc(location, radius, 210, 110, 310, 1, 0,
					(Location l, int ring) ->
						new PartialParticle(Particle.SPELL_MOB, l, FastUtils.roundRandomly(0.5), 0.4, 0, 0.5, 1).directionalMode(true).spawnAsPlayerActive(player));
			}
		}
	}

	@Override
	public void onStoreEffects(Player player, World world, Location loc, LivingEntity entity) {
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.PLAYERS, 1.2f, 0.8f);
		new PartialParticle(Particle.SMOKE_LARGE, entity.getLocation(), 15, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_MOB_AMBIENT, entity.getLocation(), 15, 0.8, 0.8, 0.8, 0).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_WITCH, loc, 10, 0.8, 0.8, 0.8, 0).spawnAsPlayerActive(player);
	}
}
