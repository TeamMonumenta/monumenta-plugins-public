package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class CelestialBlessingCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CELESTIAL_BLESSING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SUGAR;
	}

	public void tickEffect(Player player, Player target, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = target.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.15, 0.7, 0.15, 0.1).spawnAsPlayerBuff(target);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 2, 0.2, 0.4, 0.2, 0.1).spawnAsPlayerBuff(target);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 5, 0.3, 0.5, 0.3, 0.1)
			.data(new Particle.DustTransition(Color.fromRGB(0x66FF66), Color.WHITE, 0.9f)).spawnAsPlayerBuff(target);
	}

	public void loseEffect(Player player, Player target) {
		Location loc = target.getLocation();
		target.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1f, 0.65f);
		new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerBuff(target);
		new PPCircle(Particle.SPELL_INSTANT, loc.clone().add(0, 0.2, 0), 0.5)
			.count(30)
			.directionalMode(true)
			.rotateDelta(true)
			.delta(0.095, 0, 0)
			.extra(2)
			.spawnAsPlayerBuff(target);
		new PPFlower(Particle.REDSTONE, loc, 2)
			.petals(6)
			.count(110)
			.sharp(true)
			.angleStep(0.08)
			.transitionColors(Color.WHITE, Color.fromRGB(0x66FF66), 1.12f)
			.spawnAsPlayerBuff(target);
	}

	public void startEffectTargets(Player player) {
		Location loc = player.getLocation();
		player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.75f);
		player.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.75f, 1.25f);
		player.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.75f, 1.1f);
	}

	public void enhanceExtension(Player player) {
		player.sendActionBar(Component.text("Blessing Extended!").color(NamedTextColor.GOLD));
		Location loc = player.getLocation();
		World world = player.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 2f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.75f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.75f, 1.35f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.75f, 0.9f);
	}

	public void startEffectCaster(Player caster, double radius) {
		Location loc = caster.getLocation();
		new PPFlower(Particle.REDSTONE, loc, radius / 4.0)
			.petals(8)
			.count(400)
			.sharp(true)
			.angleStep(0.2)
			.transitionColors(Color.WHITE, Color.fromRGB(0x66FF66), 1f)
			.spawnAsPlayerActive(caster);

		// "Flower" Explosion
		Location spawnLoc = loc.clone().add(0, 0.2, 0);
		Vector vector = new Vector(0.095, 0, 0);
		for (int degree = 0; degree < 360; degree += 3) {
			Vector dir = VectorUtils.rotateYAxis(vector, degree);
			if (degree % 6 == 0) {
				new PartialParticle(Particle.END_ROD, spawnLoc)
					.directionalMode(true)
					.delta(dir.getX(), dir.getY(), dir.getZ())
					.extra(radius + FastUtils.sin(Math.toRadians(degree) * 6))
					.spawnAsPlayerActive(caster);
			} else {
				new PartialParticle(Particle.END_ROD, spawnLoc)
					.directionalMode(true)
					.delta(dir.getX(), dir.getY(), dir.getZ())
					.extra((radius + FastUtils.sin(Math.toRadians(degree) * 6)) * 0.92)
					.spawnAsPlayerActive(caster);
			}
		}
	}
}
