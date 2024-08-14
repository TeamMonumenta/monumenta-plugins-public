package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class JudgementChainCS implements CosmeticSkill {
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(217, 217, 217), 1.0f);
	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.JUDGEMENT_CHAIN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CHAIN;
	}

	public void onSummonChain(Player player, LivingEntity target, Location oldLoc) {
		World world = player.getWorld();
		Location playerLoc = player.getLocation();
		Location targetLoc = LocationUtils.getEntityCenter(target);

		new PartialParticle(Particle.SQUID_INK, targetLoc, 10).delta(0.5).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, targetLoc, 15).delta(0.5).spawnAsPlayerActive(player);

		new PPLine(Particle.SQUID_INK, oldLoc, targetLoc).countPerMeter(2).spawnAsPlayerActive(player);

		world.playSound(playerLoc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(playerLoc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(playerLoc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.3f, 0.8f);
		world.playSound(playerLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 0.4f);
		world.playSound(playerLoc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
		world.playSound(playerLoc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
	}

	public void chain(Player player, LivingEntity target, int t) {
		Location playerLoc = LocationUtils.getEntityCenter(player);
		Location targetLoc = LocationUtils.getEntityCenter(target);
		double delta = target.getWidth() / 2;

		new PartialParticle(Particle.REDSTONE, targetLoc, 1, delta, delta, delta, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, targetLoc, 1, delta, delta, delta, 0, DARK_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, targetLoc, 1, delta, delta, delta, 0).spawnAsPlayerActive(player);

		// actual chain - 1 particle per meter, and particles slowly inch towards the player
		new PPLine(Particle.REDSTONE, playerLoc, targetLoc).shift((20 - (t % 20)) / 20.0).countPerMeter(1).delta(0.05).extra(0.075).data(DARK_COLOR).spawnAsPlayerActive(player);
	}

	public void onBreakChain(Player player, LivingEntity target) {
		World world = player.getWorld();
		Location targetLoc = target.getLocation();

		new PartialParticle(Particle.REDSTONE, targetLoc, 15, target.getWidth() / 2, target.getWidth() / 2, target.getWidth() / 2, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, targetLoc, 15, target.getWidth() / 2, target.getWidth() / 2, target.getWidth() / 2, 0, DARK_COLOR).spawnAsPlayerActive(player);
		new PPLine(Particle.BLOCK_CRACK, LocationUtils.getEntityCenter(player), LocationUtils.getEntityCenter(target))
				.data(Material.CHAIN.createBlockData()).countPerMeter(10).delta(0.03).spawnAsPlayerActive(player);

		world.playSound(targetLoc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(targetLoc, Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(targetLoc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.5f, 0.3f);
		world.playSound(targetLoc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
	}

	public NamedTextColor glowColor() {
		return NamedTextColor.DARK_GRAY;
	}
}
