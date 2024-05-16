package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VoodooBondsCS implements CosmeticSkill {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private boolean mCurseSpreadThisTick = false;
	private boolean mCurseDeathThisTick = false;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VOODOO_BONDS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.JACK_O_LANTERN;
	}

	public void launchPin(Player player, Location startLoc, Location endLoc, boolean doSound) {
		World world = player.getWorld();

		if (doSound) {
			world.playSound(startLoc, Sound.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0f, 2.0f);
			world.playSound(startLoc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.PLAYERS, 1.0f, 1.5f);
			world.playSound(startLoc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 1.0f, 0.5f);
			world.playSound(startLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 1.15f);
		}

		new PPLine(Particle.REDSTONE, startLoc, endLoc).data(COLOR).countPerMeter(6).delta(0.07).spawnAsPlayerActive(player);
		new PPLine(Particle.SPELL_WITCH, startLoc, endLoc)
			.directionalMode(true).delta(0, -1, 0).extra(1).countPerMeter(4).spawnAsPlayerActive(player);

		if (startLoc.distance(endLoc) > 1) {
			Vector direction = LocationUtils.getDirectionTo(endLoc, startLoc);
			Location particleLoc = startLoc.clone().add(direction);
			double length = 2;
			for (int i = 0; i < 10; i++) {
				new PPCircle(Particle.FALLING_OBSIDIAN_TEAR, particleLoc, 0.35)
					.axes(VectorUtils.rotateTargetDirection(direction, 90, 0), VectorUtils.rotateTargetDirection(direction, 0, -90))
					.count(3).spawnAsPlayerActive(player);

				particleLoc.add(direction.clone().multiply(length));
				if (particleLoc.distance(endLoc) < 2) {
					break;
				}
			}
		}
	}

	public void hitMob(Player player, LivingEntity mob, boolean doSound) {
		World world = player.getWorld();
		Location loc = LocationUtils.getEntityCenter(mob);

		if (doSound) {
			world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.5f, 0.55f);
			world.playSound(loc, Sound.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.5f, 0.5f);
			world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.5f, 1.5f);
		}

		new PartialParticle(Particle.SQUID_INK, loc, 8, 0.25, 0.5, 0.25, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_WITCH, mob.getEyeLocation(), 15, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
	}

	public void hitPlayer(Player mPlayer, Player p) {
		p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 0.75f);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01).spawnAsPlayerActive(mPlayer);
	}

	public void curseTick(Player player, Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity mob) {
			Location loc = mob.getEyeLocation();
			new PartialParticle(Particle.SPELL_WITCH, loc, 6, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(player);
		}
	}

	public void curseSpread(Player player, LivingEntity toMob, LivingEntity sourceMob) {
		World world = sourceMob.getWorld();
		if (!mCurseSpreadThisTick) {
			mCurseSpreadThisTick = true;
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mCurseSpreadThisTick = false);
			world.playSound(sourceMob.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.35f, 0.75f);
		}
		new PPLine(Particle.SPELL_WITCH, sourceMob.getLocation(), toMob.getLocation()).countPerMeter(4).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPELL_WITCH, toMob.getLocation(), 6, 0.3, 0.3, 0.3, 0.001).spawnAsPlayerActive(player);
	}

	public void curseDeath(Player player, LivingEntity toMob, LivingEntity sourceMob) {
		World world = sourceMob.getWorld();
		if (!mCurseDeathThisTick) {
			mCurseDeathThisTick = true;
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mCurseDeathThisTick = false);
			world.playSound(sourceMob.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
			world.playSound(sourceMob.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1.0f, 0.5f);
			new PartialParticle(Particle.SOUL, LocationUtils.getEntityCenter(sourceMob), 25, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(player);
		}
		new PartialParticle(Particle.SOUL, LocationUtils.getEntityCenter(toMob), 25, 0.1, 0.1, 0.1, 0.1).spawnAsPlayerActive(player);
	}
}
