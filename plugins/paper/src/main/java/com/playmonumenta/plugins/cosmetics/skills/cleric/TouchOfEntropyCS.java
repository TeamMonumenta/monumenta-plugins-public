package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class TouchOfEntropyCS extends HandOfLightCS {
	//Twisted theme

	public static final String NAME = "Touch of Entropy";

	private static final Particle.DustOptions ENTRO_COLOR = new Particle.DustOptions(Color.fromRGB(60, 0, 127), 1.0f);
	private static final Particle.DustOptions DRAIN_COLOR = new Particle.DustOptions(Color.fromRGB(160, 0, 32), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HAND_OF_LIGHT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRUCTURE_VOID;
	}

	@Override
	public void lightHealEffect(Player mPlayer, Location loc) {
		new PartialParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc.add(0, 1, 0), 30, 0.7, 0.7, 0.7, 0.01, DRAIN_COLOR).spawnAsPlayerActive(mPlayer);
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.2f);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.15f, 1.6f);
	}

	@Override
	public void lightHealCastEffect(World world, Location userLoc, Plugin mPlugin, Player mPlayer, float radius, double angle) {
		final float HEALING_RADIUS = radius;
		final double HEALING_DOT_ANGLE = angle;
		world.playSound(userLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.2f);
		world.playSound(userLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.15f, 1.6f);
		ParticleUtils.explodingConeEffect(mPlugin, mPlayer, HEALING_RADIUS, Particle.SMOKE_NORMAL, 0.35f, Particle.DRAGON_BREATH, 3.0f, HEALING_DOT_ANGLE);
	}

	@Override
	public void lightDamageEffect(Player mPlayer, Location loc) {
		new PartialParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc.add(0, 1, 0), 30, 0.7, 0.7, 0.7, 0.01, ENTRO_COLOR).spawnAsPlayerActive(mPlayer);
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0.6f);
	}

	@Override
	public void lightDamageCastEffect(World world, Location userLoc, Plugin mPlugin, Player mPlayer, float radius, double angle) {
		final float DAMAGE_RADIUS = radius;
		final double HEALING_DOT_ANGLE = angle;
		world.playSound(userLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 0.6f);
		ParticleUtils.explodingConeEffect(mPlugin, mPlayer, DAMAGE_RADIUS, Particle.SMOKE_NORMAL, 0.35f, Particle.DRAGON_BREATH, 3.0f, HEALING_DOT_ANGLE);
	}
}
