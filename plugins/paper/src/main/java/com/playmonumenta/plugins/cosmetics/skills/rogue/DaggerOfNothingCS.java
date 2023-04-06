package com.playmonumenta.plugins.cosmetics.skills.rogue;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DaggerOfNothingCS extends DaggerThrowCS {
	//To test Ronde's effect, we need a skill with completely no effect.

	public static final String NAME = "Dagger of Nothing";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"New daggers of the EmpError"
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.BUCKET;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void daggerCastSound(World world, Location loc) {
		//Nope!
	}

	@Override
	public void daggerLineEffect(Location bLoc, Vector newDir, Player mPlayer) {
		//Mope!
	}

	@Override
	public void daggerHitEffect(World world, Location loc, Location bLoc, Player mPlayer) {
		//Nothing!
	}

	@Override
	public void daggerHitBlockEffect(Location bLoc, Player mPlayer) {
		//None!
	}
}
