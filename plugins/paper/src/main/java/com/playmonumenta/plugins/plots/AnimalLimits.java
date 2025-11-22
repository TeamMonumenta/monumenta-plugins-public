package com.playmonumenta.plugins.plots;

import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class AnimalLimits implements Listener {
	public static final int MAX_ANIMALS_PER_NEARBY_PLOT_CHUNK = 20;
	public static final int MAX_NEARBY_DISTANCE = 1;

	private static final Map<UUID, Integer> mLastPlotAnimalWarning = new HashMap<>();

	// Cancel breeding if on a plot full of animals
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityBreedEvent(EntityBreedEvent event) {
		Entity entity = event.getEntity();
		if (!maySummonPlotAnimal(entity.getLocation())) {
			event.setCancelled(true);
		}
	}

	// Cancel chicken spawn eggs
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void thrownEggHatchEvent(ThrownEggHatchEvent event) {
		if (!event.isHatching()) {
			// Nothing was going to spawn anyway
			return;
		}
		Class<? extends Entity> entityClass = event.getHatchingType().getEntityClass();
		if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)) {
			return;
		}
		Location location = event.getEgg().getLocation();
		int amount = maySummonPlotAnimals(location, event.getNumHatches());
		event.setNumHatches((byte) amount);
	}

	// Stop tracking last warning of unloaded world
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		mLastPlotAnimalWarning.remove(event.getWorld().getUID());
	}

	public static boolean mayUsePossibleSpawnEgg(Location loc, @Nullable ItemStack possibleEgg) {
		if (possibleEgg == null) {
			return true;
		}

		Class<? extends Entity> entityType = ItemUtils.getSpawnEggType(possibleEgg).getEntityClass();
		if (entityType == null || !LivingEntity.class.isAssignableFrom(entityType)) {
			return true;
		}

		return maySummonPlotAnimal(loc);
	}

	// Also used in MonsterEggOverride
	public static boolean maySummonPlotAnimal(Location loc) {
		return maySummonPlotAnimals(loc, 1) > 0;
	}

	public static int maySummonPlotAnimals(Location loc, int summonAttemptAmount) {
		if (!ZoneUtils.isInPlot(loc)) {
			return summonAttemptAmount;
		}

		Optional<Zone> optionalZone = ZoneUtils.getZone(loc);
		if (optionalZone.isEmpty()) {
			// Fall back on killinator functions for plots world
			return summonAttemptAmount;
		}
		Zone zone = optionalZone.get();
		if (!zone.hasProperty(ZoneUtils.ZoneProperty.PLOT.getPropertyName())) {
			// Fall back on killinator functions for plots world
			return summonAttemptAmount;
		}

		Audience audience = Audience.empty();
		BoundingBox bb = BoundingBox.of(zone.minCorner(), zone.maxCornerExclusive());
		World world = loc.getWorld();
		int animalsSeen = 0;
		int animalsLimit = 0;

		Chunk locChunk = loc.getChunk();
		int maxCx = locChunk.getX() + MAX_NEARBY_DISTANCE;
		int maxCz = locChunk.getZ() + MAX_NEARBY_DISTANCE;
		for (int cx = locChunk.getX() - MAX_NEARBY_DISTANCE; cx <= maxCx; cx++) {
			for (int cz = locChunk.getZ() - MAX_NEARBY_DISTANCE; cz <= maxCz; cz++) {
				if (!world.isChunkLoaded(cx, cz)) {
					continue;
				}
				BoundingBox chunkBb = new BoundingBox(
					16 * cx, world.getMinHeight(), 16 * cz,
					16 * cx + 15, world.getMaxHeight(), 16 * cz + 15
				);
				if (!chunkBb.overlaps(bb)) {
					continue;
				}
				Chunk chunk = world.getChunkAt(cx, cz);
				animalsLimit += MAX_ANIMALS_PER_NEARBY_PLOT_CHUNK;

				for (Entity entity : chunk.getEntities()) {
					if (!bb.contains(entity.getLocation().toVector())) {
						continue;
					}
					if (entity instanceof Player) {
						audience = Audience.audience(audience, entity);
						continue;
					}
					if (entity instanceof LivingEntity) {
						animalsSeen++;
					}
				}
			}
		}

		int animalsRemaining = animalsLimit - animalsSeen;
		int maySummonAmount = Math.min(summonAttemptAmount, animalsRemaining);
		animalsRemaining -= maySummonAmount;
		Integer lastWarningCount = mLastPlotAnimalWarning.getOrDefault(world.getUID(), animalsLimit);
		mLastPlotAnimalWarning.put(world.getUID(), animalsRemaining);
		if (animalsRemaining != lastWarningCount && animalsRemaining <= 5) {
			String msg;
			if (animalsRemaining >= 2) {
				msg = "You may have " + animalsRemaining + " more mobs nearby.";
			} else if (animalsRemaining == 1) {
				msg = "You may have 1 more mobs nearby.";
			} else if (animalsRemaining == 0) {
				msg = "You may have no more mobs nearby.";
			} else {
				msg = "Spawn attempt cancelled, you have too many mobs nearby.";
			}
			audience.sendMessage(Component.text(msg, NamedTextColor.RED));
		}

		return maySummonAmount;
	}
}
