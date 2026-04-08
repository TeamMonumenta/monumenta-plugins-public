package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class GruesomeAlchemyCS implements CosmeticSkill {
	public static Color BRUTAL_COLOR = Color.fromRGB(235, 200, 255);
	public static Color GRUESOME_COLOR = Color.fromRGB(150, 245, 32);
	public static Particle.DustOptions DIZZY_DUST_OPTIONS = new Particle.DustOptions(Color.fromRGB(242, 199, 147), 1f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.GRUESOME_ALCHEMY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public Color getBrutalColor() {
		return BRUTAL_COLOR;
	}

	public Color getGruesomeColor() {
		return GRUESOME_COLOR;
	}

	public void effectsOnSwap(Player mPlayer, boolean isGruesomeBeforeSwap) {
		mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.9f, 1.0f);
		if (isGruesomeBeforeSwap) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 1, 1.25f);
		} else {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS, 1, 0.75f);
		}
	}

	public void effectsOnSplash(Player mPlayer, Location loc, boolean isGruesome, double radius, boolean isSpecialPot) {
		// Select color of the particle, based off of mojank
		// (to color the SPELL_MOB particle, you have to use the delta values)
		Color color = isGruesome ? GRUESOME_COLOR : BRUTAL_COLOR;
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 1, 3,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> {
					new PartialParticle(Particle.SPELL_MOB, location, 1, color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0, 1).directionalMode(true).spawnAsPlayerActive(mPlayer);
				})
			)
		);
	}

	public Color splashColor(boolean isGruesome) {
		if (isGruesome) {
			return GRUESOME_COLOR;
		} else {
			return BRUTAL_COLOR;
		}
	}

	public void brutalPeriodicEffects(LivingEntity target, int stacks, int maxStacks, int level) {
	}

	public void brutalDotTickEffects(LivingEntity target) {
		new PartialParticle(Particle.SQUID_INK, target.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
		// These particles are from Brutal Alchemy. Cosmetics made for Brutal Alchemy
		// should therefore be done in combination with Gruesome Alchemy.
	}

	public void brutalDotExplosionEffects(LivingEntity target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.25f, 2f);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.25f, 2f);
		new PartialParticle(Particle.EXPLOSION_LARGE, LocationUtils.getHalfHeightLocation(target))
			.minimumCount(1)
			.spawnAsEnemy();
		new PartialParticle(Particle.SQUID_INK, target.getEyeLocation(), 60)
			.extra(0.2)
			.spawnAsEnemy();
	}

	public void brutalDotAdditionalTicksEffects(LivingEntity target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1.5f, 1.5f);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1.5f, 1.5f);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.HOSTILE, 0.75f, 2f);
	}

	public void dizzyEffects(LivingEntity target) {
		new PartialParticle(Particle.REDSTONE, target.getEyeLocation(), 15)
			.delta(0.2, 0.1, 0.2)
			.data(DIZZY_DUST_OPTIONS)
			.spawnAsEnemy();
	}

	public void stunEffects(LivingEntity target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.HOSTILE, 1f, 2f);
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.6f, 2f);
	}
}
