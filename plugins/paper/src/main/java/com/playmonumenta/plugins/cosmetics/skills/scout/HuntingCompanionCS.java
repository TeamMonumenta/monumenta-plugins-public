package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HuntingCompanionCS implements CosmeticSkill {

	public static final ImmutableMap<String, HuntingCompanionCS> SKIN_LIST = ImmutableMap.<String, HuntingCompanionCS>builder()
		.put(TwistedCompanionCS.NAME, new TwistedCompanionCS())
		.build();

	private final String FOX_NAME = "FoxCompanion";
	private final String AXOLOTL_NAME = "AxolotlCompanion";
	private final String STRIDER_NAME = "StriderCompanion";
	private final String EAGLE_NAME = "EagleCompanion";
	private final String DOLPHIN_NAME = "DolphinCompanion";

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HUNTING_COMPANION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	@Override
	public String getName() {
		return null;
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

	public String getEagleName() {
		return EAGLE_NAME;
	}

	public String getDolphinName() {
		return DOLPHIN_NAME;
	}

	public void onSummon(World world, Location loc, Player player, LivingEntity summon) {
		switch (summon.getType()) {
			case FOX -> foxOnSummon(world, loc, player, summon);
			case AXOLOTL -> axolotlOnSummon(world, loc, player, summon);
			case STRIDER -> striderOnSummon(world, loc, player, summon);
			case PARROT -> eagleOnSummon(world, loc, player, summon);
			default -> dolphinOnSummon(world, loc, player, summon);
		}
	}

	public void foxOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		foxAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.75f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
	}

	public void axolotlOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		axolotlAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_AXOLOTL_SPLASH, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.5f);
		world.playSound(loc, Sound.ITEM_BUCKET_EMPTY_AXOLOTL, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
	}

	public void striderOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		striderAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_STRIDER_STEP_LAVA, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
	}

	public void eagleOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		eagleAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_PARROT_FLY, 2.0f, 0.5f);
		world.playSound(loc, Sound.BLOCK_SMALL_DRIPLEAF_FALL, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
	}

	public void dolphinOnSummon(World world, Location loc, Player player, LivingEntity summon) {
		dolphinAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_DOLPHIN_SPLASH, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.0f);
	}

	public void onDespawn(World world, Location loc, LivingEntity summon, Player player) {
		switch (summon.getType()) {
			case FOX -> foxOnDespawn(world, loc, player, summon);
			case AXOLOTL -> axolotlOnDespawn(world, loc, player, summon);
			case STRIDER -> striderOnDespawn(world, loc, player, summon);
			case PARROT -> eagleOnDespawn(world, loc, player, summon);
			default -> dolphinOnDespawn(world, loc, player, summon);
		}
	}

	public void foxOnDespawn(World world, Location loc, Player player, LivingEntity summon) {
		foxAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, 1.5f, 1.0f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 20).spawnAsPlayerActive(player);
	}

	public void axolotlOnDespawn(World world, Location loc, Player player, LivingEntity summon) {
		foxAmbient(world, loc);
		world.playSound(loc, Sound.ITEM_BUCKET_FILL_AXOLOTL, 1.5f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.5f);
		new PartialParticle(Particle.GLOW_SQUID_INK, loc, 5).spawnAsPlayerActive(player);
	}

	public void striderOnDespawn(World world, Location loc, Player player, LivingEntity summon) {
		striderAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_STRIDER_RETREAT, 1.5f, 1.0f);
		new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, loc, 5).spawnAsPlayerActive(player);
	}

	public void eagleOnDespawn(World world, Location loc, Player player, LivingEntity summon) {
		eagleAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_PARROT_IMITATE_PHANTOM, 1.5f, 1.0f);
		new PartialParticle(Particle.CLOUD, loc, 15).spawnAsPlayerActive(player);
	}

	public void dolphinOnDespawn(World world, Location loc, Player player, LivingEntity summon) {
		dolphinAmbient(world, loc);
		world.playSound(loc, Sound.ENTITY_DOLPHIN_JUMP, 1.5f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.5f);
		new PartialParticle(Particle.WATER_BUBBLE, loc, 10).spawnAsPlayerActive(player);
	}

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
			case STRIDER -> striderOnAggro(world, loc);
			case PARROT -> eagleOnAggro(world, loc);
			default -> dolphinOnAggro(world, loc);
		}
	}

	public void foxOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
	}

	public void axolotlOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_AXOLOTL_SWIM, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_VILLAGER_NO, 0.5f, 3.0f);
	}

	public void striderOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_STRIDER_HURT, 1.5f, 1.0f);
	}

	public void eagleOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1.0f, 2.0f);
	}

	public void dolphinOnAggro(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_DOLPHIN_HURT, 1.5f, 0.5f);
	}

	public void onAttack(World world, Location loc, LivingEntity summon) {
		switch (summon.getType()) {
			case FOX -> foxOnAttack(world, loc);
			case AXOLOTL -> axolotlOnAttack(world, loc);
			case STRIDER -> striderOnAttack(world, loc);
			case PARROT -> eagleOnAttack(world, loc);
			default -> dolphinOnAttack(world, loc);
		}
	}

	public void foxOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_BITE, 1.5f, 1.0f);
	}

	public void axolotlOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_AXOLOTL_ATTACK, 1.5f, 1.0f);
	}

	public void striderOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_STRIDER_EAT, 1.5f, 1.0f);
	}

	public void eagleOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1.0f, 2.0f);
	}

	public void dolphinOnAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_DOLPHIN_ATTACK, 1.5f, 1.0f);
	}

	public void foxAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
	}

	public void axolotlAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_AXOLOTL_IDLE_WATER, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_AXOLOTL_IDLE_WATER, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_AXOLOTL_IDLE_WATER, 1.5f, 1.2f);
	}

	public void striderAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_STRIDER_AMBIENT, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_STRIDER_AMBIENT, 1.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_STRIDER_HAPPY, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_STRIDER_HAPPY, 1.5f, 1.2f);
	}

	public void eagleAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1.5f, 0.6f);
		world.playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1.5f, 1.0f);
	}

	public void dolphinAmbient(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, 1.5f, 1.2f);
	}

	public void tick(LivingEntity summon, Player player, LivingEntity target, int t) {
		switch (summon.getType()) {
			case FOX -> foxTick(summon, player, target, t);
			case AXOLOTL -> axolotlTick(summon, player, target, t);
			case STRIDER -> striderTick(summon, player, target, t);
			case PARROT -> eagleTick(summon, player, target, t);
			default -> dolphinTick(summon, player, target, t);
		}
	}

	public void foxTick(LivingEntity summon, Player player, LivingEntity target, int t) {

	}

	public void axolotlTick(LivingEntity summon, Player player, LivingEntity target, int t) {

	}

	public void striderTick(LivingEntity summon, Player player, LivingEntity target, int t) {

	}

	public void eagleTick(LivingEntity summon, Player player, LivingEntity target, int t) {

	}

	public void dolphinTick(LivingEntity summon, Player player, LivingEntity target, int t) {

	}

}
