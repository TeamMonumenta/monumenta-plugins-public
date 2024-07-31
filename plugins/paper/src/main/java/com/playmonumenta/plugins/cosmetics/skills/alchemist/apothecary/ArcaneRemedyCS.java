package com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary;

import com.playmonumenta.plugins.cosmetics.skills.alchemist.ArcanePotionsCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArcaneRemedyCS extends WardingRemedyCS {

	public static final String NAME = "Arcane Remedy";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A complex sigil granting equal protection",
			"from the dangers of the natural world and",
			"the contraptions in an Alchemist's workshop.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void remedyStartEffect(World world, Location loc, Player player, double radius) {

		// sounds
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.6f, 1.7f);
		world.playSound(loc, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(loc, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.0f);

		// big circle on the ground
		ArcanePotionsCS.drawAlchemyCircle(player, loc.add(0, 0.25, 0), radius, 3, true, ArcanePotionsCS.BISMUTH_ORE, true, true, false);

	}

	@Override
	public void remedyPulseEffect(World world, Location playerLoc, Player player, int pulse, int maxPulse, double radius) {
		if (pulse == 0) {
			return;
		}

		AbilityUtils.playPassiveAbilitySound(playerLoc, pulse < maxPulse - 1 ? Sound.BLOCK_AMETHYST_BLOCK_STEP : Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, 0.7f, 0.4f);
		AbilityUtils.playPassiveAbilitySound(playerLoc, Sound.ITEM_TRIDENT_RETURN, 0.8f, 0.8f);

	}

	@Override
	public void remedyPeriodicEffect(Location loc, Player player, int ticks) {
		// nothing, only do pulse effect
	}

	@Override
	public void remedyApplyEffect(Player caster, Player p) {
		// small circle under affected players (includes caster)
		new PPCircle(Particle.ENCHANTMENT_TABLE, p.getLocation().add(0, 0.75, 0), 0.5)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.directionalMode(true).delta(0, -0.5, 0).extra(1)
			.spawnAsPlayerActive(caster);
	}

	@Override
	public void remedyHealBuffEffect(Player caster, Player p) {
		// this is a pretty superfluous effect
	}

}
