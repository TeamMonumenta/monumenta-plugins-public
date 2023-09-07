package com.playmonumenta.plugins.plots;

import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.Zone;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.BoundingBox;

public class AnimalLimits implements Listener {
	public static final int MAX_ANIMALS_IN_PLAYER_PLOT = 80;

	public static final EnumSet<EntityType> PLOT_ANIMALS = EnumSet.of(
		EntityType.AXOLOTL,
		EntityType.BEE,
		EntityType.CAT,
		EntityType.CHICKEN,
		EntityType.COW,
		EntityType.DONKEY,
		EntityType.FOX,
		EntityType.GOAT,
		EntityType.HOGLIN,
		EntityType.HORSE,
		EntityType.LLAMA,
		EntityType.MULE,
		EntityType.MUSHROOM_COW,
		EntityType.OCELOT,
		EntityType.PANDA,
		EntityType.PARROT,
		EntityType.PIG,
		EntityType.POLAR_BEAR,
		EntityType.RABBIT,
		EntityType.RAVAGER,
		EntityType.SHEEP,
		EntityType.SHULKER,
		EntityType.SKELETON_HORSE,
		EntityType.SLIME,
		EntityType.STRIDER,
		EntityType.TRADER_LLAMA,
		EntityType.TURTLE,
		EntityType.WOLF,
		EntityType.ZOMBIE_HORSE
	);

	public static final EnumSet<Material> PLOT_ANIMAL_EGGS = EnumSet.of(
		Material.AXOLOTL_SPAWN_EGG,
		Material.CAT_SPAWN_EGG,
		Material.CHICKEN_SPAWN_EGG,
		Material.COW_SPAWN_EGG,
		Material.DONKEY_SPAWN_EGG,
		Material.FOX_SPAWN_EGG,
		Material.GOAT_SPAWN_EGG,
		Material.HOGLIN_SPAWN_EGG,
		Material.HORSE_SPAWN_EGG,
		Material.LLAMA_SPAWN_EGG,
		Material.MULE_SPAWN_EGG,
		Material.MOOSHROOM_SPAWN_EGG,
		Material.OCELOT_SPAWN_EGG,
		Material.PANDA_SPAWN_EGG,
		Material.PARROT_SPAWN_EGG,
		Material.PIG_SPAWN_EGG,
		Material.POLAR_BEAR_SPAWN_EGG,
		Material.RABBIT_SPAWN_EGG,
		Material.SHEEP_SPAWN_EGG,
		Material.SHULKER_SPAWN_EGG,
		Material.SKELETON_HORSE_SPAWN_EGG,
		Material.SLIME_SPAWN_EGG,
		Material.STRIDER_SPAWN_EGG,
		Material.TRADER_LLAMA_SPAWN_EGG,
		Material.TURTLE_SPAWN_EGG,
		Material.WOLF_SPAWN_EGG,
		Material.ZOMBIE_HORSE_SPAWN_EGG
	);

	private static final Map<UUID, Integer> mLastPlotAnimalWarning = new HashMap<>();

	// Cancel breeding if on a plot full of animals
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityBreedEvent(EntityBreedEvent event) {
		Entity entity = event.getEntity();
		if (!PLOT_ANIMALS.contains(entity.getType())) {
			return;
		}
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
		if (!PLOT_ANIMALS.contains(event.getHatchingType())) {
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

	// Also used in MonsterEggOverride
	public static boolean maySummonPlotAnimal(Location loc) {
		return maySummonPlotAnimals(loc, 1) > 0;
	}

	public static int maySummonPlotAnimals(Location loc, int amount) {
		if (!ZoneUtils.isInPlot(loc)) {
			return amount;
		}

		Optional<Zone> optionalZone = ZoneUtils.getZone(loc);
		if (optionalZone.isEmpty()) {
			// Fall back on killinator functions for plots world
			return amount;
		}
		Zone zone = optionalZone.get();
		if (!zone.hasProperty(ZoneUtils.ZoneProperty.PLOT.getPropertyName())) {
			// Fall back on killinator functions for plots world
			return amount;
		}

		int animalsRemaining = MAX_ANIMALS_IN_PLAYER_PLOT;
		World world = loc.getWorld();
		BoundingBox bb = BoundingBox.of(zone.minCorner(), zone.maxCornerExclusive());
		Set<Player> players = new HashSet<>();
		for (Entity entity : world.getNearbyEntities(bb)) {
			if (entity instanceof Player) {
				players.add((Player) entity);
			}
			if (PLOT_ANIMALS.contains(entity.getType())) {
				--animalsRemaining;
			}
		}

		int maySummonAmount = Math.min(amount, animalsRemaining);
		animalsRemaining -= maySummonAmount;
		Integer lastWarningCount = mLastPlotAnimalWarning.getOrDefault(world.getUID(), MAX_ANIMALS_IN_PLAYER_PLOT);
		mLastPlotAnimalWarning.put(world.getUID(), animalsRemaining);
		if (animalsRemaining != lastWarningCount && animalsRemaining <= 5) {
			String msg;
			if (animalsRemaining >= 2) {
				msg = "You may have " + animalsRemaining + " more animals on your plot.";
			} else if (animalsRemaining == 1) {
				msg = "You may have 1 more animal on your plot.";
			} else if (animalsRemaining == 0) {
				msg = "You may have no more animals on your plot.";
			} else {
				msg = "Spawn attempt cancelled, you have too many mobs.";
			}
			for (Player player : players) {
				player.sendMessage(Component.text(msg, NamedTextColor.RED));
			}
		}

		return maySummonAmount;
	}
}
