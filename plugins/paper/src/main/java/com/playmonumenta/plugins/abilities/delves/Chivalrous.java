package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.AntiRangeChivalrousBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import javax.annotation.Nullable;

import java.util.EnumSet;

public class Chivalrous extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.15,
			0.3,
			0.45
	};

	private static final String[] MOUNTS = {
			"SlimeMount",
			"MagmaCubeMount"
	};

	public static final String[] MOUNT_NAMES = {
			"Slime Mount",
			"Magma Cube Mount"
	};

	public static final String DESCRIPTION = "Enemies become Knights of Slime.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance to be Chivalrous."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance to be Chivalrous."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance to be Chivalrous."
			}
	};

	private final double mSpawnChance;

	private static final EnumSet<EntityType> CHIVALROUS_IMMUNE = EnumSet.of(
			EntityType.GHAST,
			EntityType.PHANTOM,
			EntityType.VEX,
			EntityType.BEE,
			EntityType.BLAZE
	);

	public Chivalrous(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.CHIVALROUS);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.CHIVALROUS);
			mSpawnChance = SPAWN_CHANCE[rank - 1];
		} else {
			mSpawnChance = 0;
		}
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		// Can't make magma cubes do 0 damage in Vanilla using attributes or Weakness
		if (MOUNT_NAMES[1].equals(damager.getCustomName())) {
			event.setCancelled(true);
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (!mob.isInsideVehicle() && !(CHIVALROUS_IMMUNE.contains(mob.getType())) && !EntityUtils.isBoss(mob) && !DelvesUtils.isDelveMob(mob)
				&& FastUtils.RANDOM.nextDouble() < mSpawnChance) {
			Entity mount = LibraryOfSoulsIntegration.summon(mob.getLocation(), MOUNTS[FastUtils.RANDOM.nextInt(MOUNTS.length)]);
			mount.addPassenger(mob);
			mob.addScoreboardTag(AntiRangeChivalrousBoss.identityTag);

			if (mob instanceof Creeper creeper) {
				creeper.setExplosionRadius((creeper.getExplosionRadius() + 1) / 2);
			}
		}
	}

}
