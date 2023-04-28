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
		world.playSound(loc, Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.PLAYERS, 2f, 0.5f);
		world.playSound(loc, Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.PLAYERS, 2f, 0.75f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.75f, 2f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 0.75f, 1.5f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.75f, 1.5f);

		// big circle on the ground
		ArcanePotionsCS.drawAlchemyCircle(player, loc.add(0, 0.25, 0), radius, true, ArcanePotionsCS.BISMUTH_ORE, true);

	}

	@Override
	public void remedyPulseEffect(World world, Location playerLoc, Player player, int pulse, int maxPulse, double radius) {
		if (pulse == 0) {
			return;
		}

		for (int i = 0; i < 2; i++) {
			AbilityUtils.playPassiveAbilitySound(playerLoc, pulse < maxPulse - 1 ? Sound.BLOCK_AMETHYST_BLOCK_STEP : Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, 1, 0.5f);
		}

	}

	@Override
	public void remedyPeriodicEffect(Location loc, Player player, int ticks) {
		// nothing, only do pulse effect
	}

	@Override
	public void remedyApplyEffect(Player caster, Player p) {
		// small circle under affected players (includes caster)
		new PPCircle(Particle.ENCHANTMENT_TABLE, p.getLocation().add(0, 0.75, 0), 0.5)
			.ringMode(true).countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.directionalMode(true).delta(0, -0.5, 0).extra(1)
			.spawnAsPlayerActive(caster);
	}

	@Override
	public void remedyHealBuffEffect(Player caster, Player p) {
		// this is a pretty superfluous effect
	}

}
