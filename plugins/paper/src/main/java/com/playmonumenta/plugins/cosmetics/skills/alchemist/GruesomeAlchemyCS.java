package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class GruesomeAlchemyCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.GRUESOME_ALCHEMY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
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
		double deltaX = isGruesome ? 1 : 0;
		double deltaY = isGruesome ? 0 : 1;
		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, radius, 1, 3,
			Arrays.asList(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> {
					new PartialParticle(Particle.SPELL_MOB, location, 1, deltaX, deltaY, 0, 1).directionalMode(true).spawnAsPlayerActive(mPlayer);
				})
			)
		);
	}

	public Color splashColor(boolean isGruesome) {
		if (isGruesome) {
			return Color.RED;
		} else {
			return Color.fromRGB(0x00FF00);
		}
	}

	public void damageOverTimeEffects(LivingEntity target) {
		new PartialParticle(Particle.SQUID_INK, target.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
	}

}
