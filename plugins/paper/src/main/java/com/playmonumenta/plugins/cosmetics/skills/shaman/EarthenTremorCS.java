package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class EarthenTremorCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.EARTHEN_TREMOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DIRT;
	}

	public void earthenTremorEffect(Player player, Location location, double radius) {
		DisplayEntityUtils.groundBlockQuake(location.add(0, 0.1, 0), radius, List.of(Material.PODZOL, Material.DIRT, Material.MUD), new Display.Brightness(12, 12));
		World world	= player.getWorld();
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.7f, 0.4f);
		world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.2f, 2f);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.2f, 0.8f);
	}

	public void totemLandingEffect(Player player, Location location, double radius) {
		Location loc = location.add(0, 0.2, 0);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 70, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.PODZOL)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 70, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.GRANITE)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 70, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.IRON_ORE)).spawnAsPlayerActive(player);

		World world	= player.getWorld();
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(location, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.3f, 0.6f);
		world.playSound(location, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.2f, 1.9f);
		world.playSound(location, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 0.7f, 0.6f);
	}

	public void cursedEarthEffect(Player player, Location location, double radius, boolean meleeHit) {
		Location loc = location.add(0, 0.2, 0);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.MYCELIUM)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, loc, 50, radius * 0.75, 0.25, radius * 0.75, 0.1,
			Bukkit.createBlockData(Material.SCULK)).spawnAsPlayerActive(player);

		World world	= location.getWorld();

		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.7f, 0.5f);
		world.playSound(location, Sound.ENTITY_FROG_TONGUE, SoundCategory.PLAYERS, 2f, 0.5f);
		world.playSound(location, Sound.ENTITY_FROG_TONGUE, SoundCategory.PLAYERS, 2f, 0.5f);
		world.playSound(location, Sound.ENTITY_FROG_TONGUE, SoundCategory.PLAYERS, 2f, 0.5f);
		if (meleeHit) {
			world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.8f);
			world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
		}
		else {
			world.playSound(location, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.7f, 0.7f);
			world.playSound(location, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 1.6f);
		}
	}

	public void cursedEarthGain(Player player, Entity mob) {
		player.getWorld().playSound(mob.getLocation(), Sound.BLOCK_BEEHIVE_ENTER, SoundCategory.PLAYERS, 0.5f, 0.5f);
	}

	public void cursedEarthTick(Player player, Entity mob, boolean oneHertz) {
		if (!oneHertz) {
			return;
		}

		new PartialParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, mob.getHeight() * 0.5, 0), 25, mob.getWidth() / 2,
			0.25, mob.getWidth() / 2, 0.1,
			Bukkit.createBlockData(Material.PODZOL)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, mob.getHeight() * 0.5, 0), 25, mob.getWidth() / 2,
			0.25, mob.getWidth() / 2, 0.1,
			Bukkit.createBlockData(Material.MYCELIUM)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, mob.getHeight() * 0.5, 0), 25, mob.getWidth() / 2,
			0.25, mob.getWidth() / 2, 0.1,
			Bukkit.createBlockData(Material.WARPED_FUNGUS)).spawnAsPlayerActive(player);
	}

	public void cursedEarthExpire(Player player, Entity mob) {
		player.getWorld().playSound(mob.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 2f, 0.5f);
		player.getWorld().playSound(mob.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 2f, 0.6f);
	}
}
