package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class TacticalManeuverCS implements CosmeticSkill {

	public static final ImmutableMap<String, TacticalManeuverCS> SKIN_LIST = ImmutableMap.<String, TacticalManeuverCS>builder()
		.put(PrestigiousManeuverCS.NAME, new PrestigiousManeuverCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.TACTICAL_MANEUVER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRING;
	}

	@Override
	public String getName() {
		return null;
	}

	public void maneuverStartEffect(World world, Player mPlayer, Vector dir) {
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 1.7f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 63, 0.25, 0.1, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
	}

	public void maneuverBackEffect(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.PLAYERS, 1, 1.2f);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 15, 0.1f, 0, 0.1f, 0.125f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 10, 0.1f, 0, 0.1f, 0.15f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 25, 0.1f, 0, 0.1f, 0.15f).spawnAsPlayerActive(mPlayer);
	}

	public void maneuverTickEffect(Player mPlayer) {
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 5, 0.25, 0.1, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
	}

	public void maneuverHitEffect(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 2.0f, 0.5f);
		new PartialParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 63, 0.25, 0.1, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
	}
}
