package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.List;

public class EtherealAscensionCS implements CosmeticSkill {

	private static final Color LIGHT_COLOR = Color.fromRGB(255, 245, 235);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ETHEREAL_ASCENSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TOTEM_OF_UNDYING;
	}

	public void onLaunch(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BREEZE_SLIDE, SoundCategory.PLAYERS, 1.6f, 1.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.8f, 1.2f);
	}

	public void tickEffect(Player player, Location loc, double hoverHeight) {
		Vector random = new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-1, 1)).normalize();
		new PartialParticle(Particle.SPELL_MOB_AMBIENT, loc.clone().add(random).multiply(FastUtils.randomDoubleInRange(1.5, 3))).delta(LIGHT_COLOR.getRed(), LIGHT_COLOR.getGreen(), LIGHT_COLOR.getBlue()).extra(1).directionalMode(true).spawnAsPlayerActive(player);
	}

	public void orbShoot(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.0f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.4f);
	}

	public void orbTrail(Player player, Location loc, Location startLoc) {
		new PartialParticle(Particle.ELECTRIC_SPARK, loc, 1, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
	}

	public void orbImpact(Player player, World world, Location loc, double radius) {
		new PartialParticle(Particle.GUST, loc, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, loc, 8, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMALL_FLAME, loc, 10, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.5f, 1.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 1.1f);

	}

	public void orbBuff(Player player, World world, Location loc, List<Player> hitPlayers, double radius) {
		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1f, 2f);

	}

	public void forceEndAscension(Player player, World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
	}

	public void ascensionEnd(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.6f, 1.0f);
	}
}
