package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SoulStrainCS extends DivineJusticeCS {

	public static final String NAME = "Soul Strain";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The deadliest strike is the one that",
			"pierces the soul itself. For when the mind",
			"is crippled, the flesh falls with it."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_PEARL;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustTransition CYAN = new Particle.DustTransition(Color.fromRGB(0, 235, 235), Color.fromRGB(0, 126, 112), 1.15f);
	private static final double[] ANGLE = {200, -22.5, -95};

	@Override
	public void justiceOnDamage(Player player, LivingEntity enemy, World world, Location enemyLoc, double widerWidthDelta, int combo, boolean enhanced) {
		Vector dir = player.getEyeLocation().getDirection();
		world.playSound(enemyLoc, Sound.ITEM_AXE_WAX_OFF, SoundCategory.PLAYERS, 1.7f, 1.1f);
		world.playSound(enemyLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(enemyLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.6f, 0.8f);
		world.playSound(enemyLoc, Sound.BLOCK_SHROOMLIGHT_FALL, SoundCategory.PLAYERS, 1.7f, 0.7f);
		world.playSound(enemyLoc, Sound.BLOCK_SCULK_BREAK, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(enemyLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 0.8f);

		if (combo == 2) {
			world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, enhanced ? 0.85f : 0.7f, 1.2f);
			world.playSound(enemyLoc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, enhanced ? 0.9f : 0.75f, 1.1f);
			world.playSound(enemyLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, enhanced ? 1f : 0.8f, 1.3f);
			// Hieroglyph for "Die"
			double width = Math.min(enemy.getWidth() * 1.15, 1.25);
			Vector front = dir.clone().setY(0).normalize().multiply(-width);
			Vector left = VectorUtils.rotateTargetDirection(front, 90, 0);
			Vector right = VectorUtils.rotateTargetDirection(front, -90, 0);
			Location loc = enemyLoc.clone().add(front);
			Location loc1 = loc.clone().add(left);
			Location loc2 = loc.clone().add(right);
			double hieroglyphRadius = enemyLoc.distance(loc.clone().add(left));
			for (int i = 0; i < 2; i++) {
				double delta = 0.1 * i;
				final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(180 - 60 * i, 0, 60 - 20 * i), 1.0f - i * 0.2f);
				// Axe
				new PPLine(Particle.REDSTONE, loc1, loc1.clone().subtract(front.clone().multiply(2))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc1.clone().subtract(front), loc.clone().subtract(front.clone().multiply(1.5))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc1.clone().subtract(front.clone().multiply(2)), loc.clone().subtract(front.clone().multiply(1.5))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				// Head
				new PPLine(Particle.REDSTONE, loc2, loc2.clone().subtract(front.clone().multiply(2))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPCircle(Particle.REDSTONE, loc.clone().add(right.clone().multiply(0.5)).subtract(front.clone().multiply(1.5)), width / 2).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
			}
			new PPCircle(Particle.ENCHANTMENT_TABLE, enemyLoc, hieroglyphRadius).countPerMeter(8).extraRange(0.1, 0.15).innerRadiusFactor(1)
				.directionalMode(true).delta(-2, 0.2, 8).rotateDelta(true).spawnAsPlayerActive(player);
		}

		if (enemyLoc.getY() + 1 > enemy.getEyeLocation().getY()) {
			enemyLoc = enemy.getEyeLocation();
		} else {
			enemyLoc.add(0, 1, 0);
		}
		enemyLoc.setDirection(dir);

		new PartialParticle(Particle.SCULK_CHARGE_POP, enemyLoc, 8, 0.1, 0.2 * enemy.getHeight(), 0.1, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, enemyLoc, 6, 0.1, 0.2 * enemy.getHeight(), 0.1, 0.05).spawnAsPlayerActive(player);

		ParticleUtils.drawHalfArc(enemyLoc.clone().subtract(dir.clone().multiply(2.25)), 2, ANGLE[combo], 0, 155, 1, 0.1, (Location l, int ring, double angleProgress) -> {
			new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 2, 0.06, 0.06, 0.06, 0).data(CYAN).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_MOB, l, FastUtils.roundRandomly(0.25)).delta(0, 0.96, 0.96).extra(1).directionalMode(true).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SPELL_MOB_AMBIENT, l, FastUtils.roundRandomly(0.35)).delta(0, 0.9, 0.8).extra(1).directionalMode(true).spawnAsPlayerActive(player);
		});
	}

	@Override
	public void justiceKill(Player player, Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WARDEN_LISTENING_ANGRY, SoundCategory.PLAYERS, 1.25f, 1.2f);
		new PartialParticle(Particle.SOUL, loc, 20, 0.5, 0.8, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SHRIEK, loc.clone().add(0, 1.5, 0), 3, 0.0, 0.0, 0.0, 0.0).data(0).spawnAsPlayerActive(player);
	}

	@Override
	public void justiceHealSound(List<Player> players, float pitch) {
		for (Player healedPlayer : players) {
			healedPlayer.playSound(healedPlayer.getLocation(), Sound.ENTITY_SKELETON_HORSE_AMBIENT, SoundCategory.PLAYERS, 0.85f, 1.75f);
		}
	}
}
