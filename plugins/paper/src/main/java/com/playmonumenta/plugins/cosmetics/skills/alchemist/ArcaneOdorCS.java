package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.particle.PPCircle;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ArcaneOdorCS extends EmpoweringOdorCS {

	public static final String NAME = "Arcane Odor";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Knowing which parts of potion mixing can be",
			"done less meticulously can speed up the process,",
			"with only very few side-effects.",
			"Some of those may even be beneficial..."
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENCHANTING_TABLE;
	}

	@Override
	public void applyEffects(Player caster, Player target, int duration) {
		new PPCircle(Particle.ENCHANTMENT_TABLE, target.getLocation().add(0, 0.75, 0), 0.5)
			.countPerMeter(ArcanePotionsCS.ENCHANT_PARTICLE_PER_METER)
			.directionalMode(true).delta(0, -0.5, 0).extra(1)
			.spawnAsPlayerActive(caster);
		// no sound
	}

}
