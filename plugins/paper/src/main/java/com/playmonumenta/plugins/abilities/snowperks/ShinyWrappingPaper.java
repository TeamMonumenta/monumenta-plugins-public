package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class ShinyWrappingPaper extends Ability {
	private static final String SCOREBOARD = "ShinyWrappingPaper";
	private static final int POINT_COST = 1;
	private static final int RANGE = 12;
	private static final LootTable T1_LOOT_TABLE = Bukkit.getLootTable(NamespacedKeyUtils.fromString("epic:r1/dungeons/koal/basic_chests/t1"));

	public static final AbilityInfo<ShinyWrappingPaper> INFO =
		new SnowPerkGui.SnowPerkInfo<>(ShinyWrappingPaper.class, "Shiny Wrapping Paper", ShinyWrappingPaper::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.PAPER)
			.description(getDescription());

	private final Map<Location, Integer> mGlowingTimers;
	private final Map<Location, BlockDisplay> mGlowingChests;

	public ShinyWrappingPaper(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mGlowingTimers = new HashMap<>();
		mGlowingChests = new HashMap<>();
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		List<Chunk> chunkList = LocationUtils.getSurroundingChunks(mPlayer.getLocation().getBlock(), 18);
		for (Chunk chunk : chunkList) {
			for (BlockState state : chunk.getTileEntities(b -> b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST, false)) {
				if (LocationUtils.blocksAreWithinRadius(mPlayer.getLocation().getBlock(), state.getBlock(), RANGE)
					&& state instanceof Chest chest && chest.getLootTable() != null && chest.getSeed() == 0) {
					// If no glowing display there, create one
					if (!mGlowingChests.containsKey(state.getBlock().getLocation())) {
						BlockDisplay glowDisplay = mPlayer.getWorld().spawn(state.getBlock().getLocation(), BlockDisplay.class, display -> {
							display.setVisibleByDefault(false);
							display.setGlowing(true);
							display.setBlock(Material.YELLOW_STAINED_GLASS.createBlockData());
							// Slightly smaller than chest size to avoid Z-fighting
							display.setTransformation(new Transformation(new Vector3f(0.07f, 0, 0.07f), new Quaternionf(), new Vector3f(0.86f), new Quaternionf()));
							display.setPersistent(false);
							EntityUtils.setRemoveEntityOnUnload(display);

							// T2 chests and other "key item" chests will be orange instead of yellow
							boolean isT1Chest = chest.getLootTable().equals(T1_LOOT_TABLE);
							display.setGlowColorOverride(Color.fromRGB(isT1Chest ? 0xF5E7A3 : 0xFF8317));
						});

						mPlayer.showEntity(mPlugin, glowDisplay);
						mGlowingChests.put(state.getBlock().getLocation(), glowDisplay);
					}

					// Reset time to 1s
					mGlowingTimers.put(state.getBlock().getLocation(), 20);
				}
			}
		}

		mGlowingTimers.entrySet().removeIf(entry -> {
			int newValue = entry.getValue() - 5;
			if (newValue <= 0) {
				Location loc = entry.getKey();
				BlockDisplay glowDisplay = mGlowingChests.remove(loc);
				if (glowDisplay != null) {
					glowDisplay.remove();
				}
				return true;
			}
			entry.setValue(newValue);
			return false;
		});
	}

	@Override
	public boolean blockBreakEvent(final BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.CHEST) {
			Location loc = event.getBlock().getLocation();
			mGlowingTimers.remove(loc);
			BlockDisplay glowDisplay = mGlowingChests.remove(loc);
			if (glowDisplay != null) {
				glowDisplay.remove();
			}
		}
		return true;
	}

	@Override
	public void invalidate() {
		mGlowingChests.forEach((location, blockDisplay) -> {
			if (blockDisplay != null && blockDisplay.isValid()) {
				blockDisplay.remove();
			}
		});
		mGlowingTimers.clear();
		mGlowingChests.clear();
	}

	public static Description<ShinyWrappingPaper> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Passively reveal the location and tier")
			.addLine("of all unopened chests within %d blocks.").statValues(stat(RANGE))
			.addLine()
			.addStat("Cost: %d Snow Point").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
