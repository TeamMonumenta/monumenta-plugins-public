package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WardingRemedyCS implements CosmeticSkill {

	public static final ImmutableMap<String, WardingRemedyCS> SKIN_LIST = ImmutableMap.<String, WardingRemedyCS>builder()
		.put(PrestigiousRemedyCS.NAME, new PrestigiousRemedyCS())
		.build();

	private static final Color APOTHECARY_LIGHT_COLOR = Color.fromRGB(255, 255, 100);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.5f);

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.WARDING_REMEDY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_CARROT;
	}

	@Override
	public String getName() {
		return null;
	}

	public void remedyStartEffect(World world, Location loc, Player mPlayer, double radius) {
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 2f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 1.5f);

		new PartialParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 50, 0.25, 0.25, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 80, 2.8, 2.8, 2.8, new Particle.DustOptions(APOTHECARY_LIGHT_COLOR, 3.0f)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 40, 0.35, 0.5, 0.35, APOTHECARY_DARK_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 60, 0.25, 0.25, 0.25, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 0.15, 0), 100, 2.8, 0, 2.8, APOTHECARY_DARK_COLOR).spawnAsPlayerActive(mPlayer);
	}

	public ImmutableList<PPPeriodic> remedyPeriodicEffect(Location loc) {
		return new ImmutableList.Builder<PPPeriodic>()
			.add(new PPPeriodic(Particle.END_ROD, loc).count(1).delta(0.35, 0.15, 0.35).extra(0.05))
			.build();
	}

	public void remedyPulseEffect(World world, Location playerLoc, Player mPlayer, int pulse, int maxPulse, double radius) {
		world.playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.7f, 2f);

		new PPCircle(Particle.REDSTONE, playerLoc.clone().add(0, 0.15, 0), 6).ringMode(true).count(1).data(APOTHECARY_DARK_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, playerLoc.clone().add(0, 0.15, 0), 15, 2.8, 0, 2.8, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, playerLoc, 40, 2.8, 2.8, 2.8, new Particle.DustOptions(APOTHECARY_LIGHT_COLOR, 1.5f)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, playerLoc, 20, 2.8, 2.8, 2.8, 0).spawnAsPlayerActive(mPlayer);
	}

	public void remedyApplyEffect(Player mPlayer, Player p) {
		new PartialParticle(Particle.REDSTONE, p.getLocation().clone().add(0, 0.5, 0), 10, 0.35, 0.15, 0.35, new Particle.DustOptions(APOTHECARY_LIGHT_COLOR, 1.0f)).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL, p.getLocation().clone().add(0, 0.5, 0), 5, 0.35, 0.15, 0.35, 0).spawnAsPlayerActive(mPlayer);
	}

	public void remedyHealBuffEffect(Player mPlayer, Player p) {
		new PartialParticle(Particle.SPELL_WITCH, p.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3).spawnAsPlayerBuff(mPlayer);
		new PartialParticle(Particle.REDSTONE, p.getLocation().add(0, 1, 0), 3, 0.4, 0.5, 0.4, APOTHECARY_DARK_COLOR).spawnAsPlayerBuff(mPlayer);
	}

}
