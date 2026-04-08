package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class RampageCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.RAMPAGE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	public void onHitMob(Player player, LivingEntity mob) {
		new PartialParticle(Particle.VILLAGER_ANGRY, mob.getLocation(), 5, 0, 0, 0, 0.1).spawnAsPlayerActive(player);
	}

	public void onStackGain(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_AMBIENT, SoundCategory.PLAYERS, 0.2f, 1.3f);
		world.playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 0.2f, 1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.3f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.3f, 0.7f);
	}

	public void onCast(Player player, Location loc, World world, double radius) {
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 60)
			.extra(radius * 0.07)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_LARGE, loc).minimumCount(1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, player.getHeight() / 2, 0), 10, 1, 2, 1, 0).spawnAsPlayerActive(player);

		new PPCircle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 0.5)
			.count(100)
			.delta(0.07, 0, 0)
			.directionalMode(true).rotateDelta(true)
			.extra(radius - 1).extraVariance(0.5)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, loc.clone().add(0, 0.1, 0), 0.5)
			.count(100)
			.delta(0.07, 0, 0)
			.directionalMode(true).rotateDelta(true)
			.extra(radius - 1).extraVariance(0.5)
			.spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.4f, 0.6f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.6f, 0.7f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.4f, 0.1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.4f, 0.1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.7f, 1.7f);
	}

	public void tick(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, player.getLocation())
			.count(4)
			.delta(0.2, 0.6, 0.2)
			.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0xff975e), 1.0f))
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, LocationUtils.getHalfHeightLocation(player))
			.count(6)
			.delta(0.2, 0.6, 0.2)
			.extra(0.01)
			.spawnAsPlayerActive(player);
	}

	public void loseEffect(Player player) {
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 2f, 0.4f);
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 2f, 0.7f);
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.4f, 1.5f);
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.7f, 0.4f);
	}
}
