package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
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
import org.jetbrains.annotations.Nullable;

public class HuntingCompanionCS implements CosmeticSkill {

	private static final Particle.DustOptions GRAY =
		new Particle.DustOptions(Color.fromRGB(211, 211, 211), 0.8f);

	private final String FOX_NAME = "FoxCompanion";
	private final String AXOLOTL_NAME = "AxolotlCompanion";
	private final String STRIDER_NAME = "StriderCompanion";

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HUNTING_COMPANION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	public String getFoxName() {
		return FOX_NAME;
	}

	public String getAxolotlName() {
		return AXOLOTL_NAME;
	}

	public String getStriderName() {
		return STRIDER_NAME;
	}

	// On Summon
	// ie. Being teleported or by transformation

	public void onSummon(World world, Location loc, Player player, LivingEntity summon) {
		switch (summon.getType()) {
			case FOX -> foxOnSummon(world, loc, player, summon);
			case AXOLOTL -> axolotlOnSummon(world, loc, player, summon);
			default -> striderOnSummon(world, loc, player, summon);
		}
	}

	public void foxOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		foxAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, SoundCategory.NEUTRAL, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundCategory.NEUTRAL, 0.75f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
	}

	public void axolotlOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		axolotlAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_AXOLOTL_SPLASH, SoundCategory.NEUTRAL, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.NEUTRAL, 1.0f, 1.5f);
		world.playSound(loc, Sound.ITEM_BUCKET_EMPTY_AXOLOTL, SoundCategory.NEUTRAL, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.0f, 1.5f);
	}

	public void striderOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		striderAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_STRIDER_STEP_LAVA, SoundCategory.NEUTRAL, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
	}

	// On Teleport

	public void onTeleport(World world, Location loc, Player player, LivingEntity summon) {
		switch (summon.getType()) {
			case FOX -> foxOnTeleport(world, loc, player, summon);
			case AXOLOTL -> axolotlOnTeleport(world, loc, player, summon);
			default -> striderOnTeleport(world, loc, player, summon);
		}
	}

	public void foxOnTeleport(World world, Location loc, Player player, LivingEntity summon) {
		foxAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, SoundCategory.NEUTRAL, 1.5f, 1.0f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 20).spawnAsPlayerActive(player);
	}

	public void axolotlOnTeleport(World world, Location loc, Player player, LivingEntity summon) {
		axolotlAmbient(world, loc);
		world.playSound(loc, Sound.ITEM_BUCKET_FILL_AXOLOTL, SoundCategory.NEUTRAL, 1.5f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.NEUTRAL, 1.0f, 1.5f);
		new PartialParticle(Particle.GLOW_SQUID_INK, loc, 5).spawnAsPlayerActive(player);
	}

	public void striderOnTeleport(World world, Location loc, Player player, LivingEntity summon) {
		striderAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_STRIDER_RETREAT, SoundCategory.NEUTRAL, 1.5f, 1.0f);
		new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, loc, 5).spawnAsPlayerActive(player);
	}

	// On Aggro

	public void onAggro(World world, Location loc, Player player, LivingEntity summon) {
		onAggroParticles(player, summon);
		onAggroSounds(world, loc, summon);
	}

	public void onAggroParticles(Player player, LivingEntity summon) {
		new PartialParticle(Particle.VILLAGER_ANGRY, summon.getEyeLocation(), 25).spawnAsPlayerActive(player);
	}

	public void onAggroSounds(World world, Location loc, LivingEntity summon) {
		switch (summon.getType()) {
			case FOX -> foxOnAggro(world, loc);
			case AXOLOTL -> axolotlOnAggro(world, loc);
			default -> striderOnAggro(world, loc);
		}
	}

	public void foxOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_AGGRO, SoundCategory.NEUTRAL, 1.5f, 1.0f);
	}

	public void axolotlOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_AXOLOTL_SWIM, SoundCategory.NEUTRAL, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 0.5f, 3.0f);
	}

	public void striderOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_STRIDER_HURT, SoundCategory.NEUTRAL, 1.5f, 1.0f);
	}

	// On Attack

	public void onAttack(World world, Location loc, Player player, LivingEntity summon) {
		switch (summon.getType()) {
			case FOX -> foxOnAttack(world, loc);
			case AXOLOTL -> axolotlOnAttack(world, loc);
			default -> striderOnAttack(world, loc);
		}
	}

	public void foxOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_BITE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
	}

	public void axolotlOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_AXOLOTL_ATTACK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
	}

	public void striderOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_STRIDER_EAT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
	}

	// Ambient

	public void foxAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 1.2f);
	}

	public void axolotlAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_AXOLOTL_IDLE_WATER, SoundCategory.NEUTRAL, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_AXOLOTL_IDLE_WATER, SoundCategory.NEUTRAL, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_AXOLOTL_IDLE_WATER, SoundCategory.NEUTRAL, 1.5f, 1.2f);
	}

	public void striderAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_STRIDER_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_STRIDER_AMBIENT, SoundCategory.NEUTRAL, 1.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_STRIDER_HAPPY, SoundCategory.NEUTRAL, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_STRIDER_HAPPY, SoundCategory.NEUTRAL, 1.5f, 1.2f);
	}

	// On Tick

	public void tick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {
		switch (summon.getType()) {
			case FOX -> foxTick(summon, player, target, t);
			case AXOLOTL -> axolotlTick(summon, player, target, t);
			default -> striderTick(summon, player, target, t);
		}
	}

	public void foxTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {

	}

	public void axolotlTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {

	}

	public void striderTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {

	}

	// On Tick

	public void pounceTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {
		switch (summon.getType()) {
			case FOX -> foxPounceTick(summon, player, target, t);
			case AXOLOTL -> axolotlPounceTick(summon, player, target, t);
			default -> striderPounceTick(summon, player, target, t);
		}
	}

	public void foxPounceTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {
		new PartialParticle(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(summon))
			.count(15)
			.delta(0.2, 0.2, 0.5)
			.data(GRAY)
			.spawnAsPlayerActive(player);
	}

	public void axolotlPounceTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {

	}

	public void striderPounceTick(LivingEntity summon, Player player, @Nullable LivingEntity target, int t) {

	}

	// On Jump

	public void onJump(World world, Location loc, Player player, LivingEntity summon, LivingEntity target) {
		switch (summon.getType()) {
			case FOX -> foxOnJump(world, loc, summon, player, target);
			case AXOLOTL -> axolotlOnJump(world, loc, summon, player, target);
			default -> striderOnJump(world, loc, summon, player, target);
		}
	}

	public void foxOnJump(World world, Location loc, LivingEntity summon, Player player, LivingEntity target) {
		world.playSound(loc, Sound.ENTITY_PARROT_FLY, SoundCategory.NEUTRAL, 1.5f, 1f);
		world.playSound(loc, Sound.ENTITY_FOX_SCREECH, SoundCategory.NEUTRAL, 1.5f, 1f);
	}

	public void axolotlOnJump(World world, Location loc, LivingEntity summon, Player player, LivingEntity target) {

	}

	public void striderOnJump(World world, Location loc, LivingEntity summon, Player player, LivingEntity target) {
		world.playSound(loc, Sound.ENTITY_PARROT_FLY, SoundCategory.NEUTRAL, 1.5f, 1f);
	}

	// On Pounce

	public void onPounce(World world, Location loc, Player player, LivingEntity summon, double radius) {
		Location startLoc = LocationUtils.getHalfHeightLocation(summon);

		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 2, 0.5f);
		world.playSound(loc, Sound.ENTITY_BREEZE_LAND, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_BIG_FALL, 1.2f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.5f);

		new PPCircle(Particle.CLOUD, startLoc, radius - 0.25)
			.ringMode(true)
			.count(8)
			.countPerMeter(1)
			.extra(0.4)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, startLoc, radius - 0.25)
			.ringMode(true)
			.count(16)
			.countPerMeter(8)
			.data(new Particle.DustOptions(Color.WHITE, 1.5f))
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.SWEEP_ATTACK, startLoc, radius - 0.25)
			.ringMode(true)
			.count(8)
			.countPerMeter(2)
			.spawnAsPlayerActive(player);
	}
}
