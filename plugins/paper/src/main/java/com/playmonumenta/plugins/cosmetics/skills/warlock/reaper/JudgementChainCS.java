package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
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
import org.bukkit.scoreboard.Team;

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

	public void onPassDamage(Player player, LivingEntity chainedMob, LivingEntity selectedMob) {
		Location loc = LocationUtils.getHalfHeightLocation(selectedMob);
		new PartialParticle(Particle.REDSTONE, loc, 15, 0.5, 0.5, 0.5, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 15, 0.5, 0.5, 0.5, 0, DARK_COLOR).spawnAsPlayerActive(player);
	}

	public void onSummonChain(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.3f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
	}

	public void chain(Player player, Location playerLoc, Location targetLloc, double delta, int t) {
		new PartialParticle(Particle.REDSTONE, targetLloc, 1, delta, delta, delta, 0, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, targetLloc, 1, delta, delta, delta, 0, DARK_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, targetLloc, 1, delta, delta, delta, 0).spawnAsPlayerActive(player);

		// actual chain - 1 particle per meter, and particles slowly inch towards the player
		new PPLine(Particle.REDSTONE, playerLoc, targetLloc).shift((20 - (t % 20)) / 20.0).countPerMeter(1).delta(0.05).extra(0.075).data(DARK_COLOR).spawnAsPlayerActive(player);
	}

	public void onBreakChain(Player player, LivingEntity target, boolean isLevelTwo, double effectRadius, double damageRadius) {
		World world = player.getWorld();
		Location midPlayerLoc = LocationUtils.getHalfHeightLocation(player);
		Location playerLoc = player.getLocation();
		Location targetLoc = target.getLocation();

		new PartialParticle(Particle.REDSTONE, midPlayerLoc, 15, effectRadius, effectRadius, effectRadius, 0.125, LIGHT_COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, midPlayerLoc, 15, effectRadius, effectRadius, effectRadius, 0.125, DARK_COLOR).spawnAsPlayerActive(player);
		if (isLevelTwo) {
			new PartialParticle(Particle.CRIT, midPlayerLoc, 30, damageRadius, damageRadius, damageRadius, 0.125).spawnAsPlayerActive(player);
		}

		world.playSound(targetLoc, Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(playerLoc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.7f, 0.3f);
		world.playSound(playerLoc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.6f, 0.7f);
		world.playSound(playerLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 2.0f, 1.1f);
	}

	public Team createTeam() {
		return ScoreboardUtils.getExistingTeamOrCreate("chainColor", NamedTextColor.DARK_GRAY);
	}
}
