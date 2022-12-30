package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class RedstoneBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_redstone";

	public static class Parameters extends BossParameters {
		@BossParam(help = "x coordinate for redstone block")
		public int X = 0;
		@BossParam(help = "y coordinate for redstone block")
		public int Y = 0;
		@BossParam(help = "z coordinate for redstone block")
		public int Z = 0;

		@BossParam(help = "detection range")
		public int DETECTION = 50;

		@BossParam(help = "type of block to be placed on death")
		public Material MATERIAL = Material.REDSTONE_BLOCK;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RedstoneBoss(plugin, boss);
	}

	private final Location mLocation;
	private final Material mMaterial;

	public RedstoneBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mLocation = new Location(boss.getWorld(), p.X, p.Y, p.Z);
		mMaterial = p.MATERIAL;

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), p.DETECTION, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mLocation.getBlock().setType(mMaterial);
	}
}
