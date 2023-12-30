package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LootTableArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;

public class SimulateLoot {
	public static final String COMMAND = "simulate";
	public static final int ROLLS_PER_TICK = 10;

	public static class ItemOdds {
		public int mRolled = 0;
		public int mTotalRolled = 0;
		public SortedMap<Integer, Integer> mSizeRollCount = new TreeMap<>();
	}

	public static class OddsSorter implements Comparator<Map.Entry<ItemStack, ItemOdds>> {
		@Override
		public int compare(Map.Entry<ItemStack, ItemOdds> o1, Map.Entry<ItemStack, ItemOdds> o2) {
			int result;

			result = Integer.compare(o1.getValue().mRolled, o2.getValue().mRolled);
			if (result != 0) {
				return result;
			}

			return Integer.compare(o1.getValue().mTotalRolled, o2.getValue().mTotalRolled);
		}
	}

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.simulateloot");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(new MultiLiteralArgument("loot"))
			.withArguments(new IntegerArgument("rolls", 1))
			.withArguments(new LootTableArgument("loot table"))
			.executesNative((nativeSender, args) -> {
				int numRolls = (int) args[1];
				LootTable lootTable = (LootTable) args[2];
				LootContext lootContext = new LootContext
					.Builder(nativeSender.getLocation())
					.build();
				run(plugin, nativeSender, numRolls, lootTable, lootContext);
			})
			// Add additional loot context details here, such as luck values, killer/killed mob, etc
			.register();
	}

	public static void run(final Plugin plugin,
	                final NativeProxyCommandSender sender,
	                final int numRolls,
	                final LootTable lootTable,
	                final LootContext lootContext) {
		CommandSender caller = sender.getCaller();
		CommandSender callee = sender.getCallee();
		final Audience audience;
		if (caller.equals(callee)) {
			audience = caller;
		} else {
			audience = Audience.audience(sender.getCallee(), sender.getCaller());
		}
		final BossBar progressBar = BossBar.bossBar(Component.text("Simulating Loot (0/" + numRolls + ")...",
				NamedTextColor.BLUE),
			0.0f,
			BossBar.Color.BLUE,
			BossBar.Overlay.NOTCHED_20);
		audience.showBossBar(progressBar);

		new BukkitRunnable() {
			int mRollsDone = 0;
			final Map<ItemStack, ItemOdds> mRolledItemCounts = new HashMap<>();
			int mTotalItems = 0;

			@Override
			public void run() {
				int rollsThisTick = Math.min(ROLLS_PER_TICK, numRolls - mRollsDone);
				mRollsDone += rollsThisTick;
				for (int i = 0; i < rollsThisTick; i++) {
					// Merge items into "stacks", ignoring max items per stack
					Map<ItemStack, Integer> mergedStacks = new HashMap<>();
					for (ItemStack itemStack : lootTable.populateLoot(FastUtils.RANDOM, lootContext)) {
						ItemStack singleStack = itemStack.asOne();
						int stackSize = itemStack.getAmount();

						mergedStacks.put(singleStack,
							mergedStacks.getOrDefault(singleStack, 0) + stackSize);
					}

					// Track statistics for each roll
					for (Map.Entry<ItemStack, Integer> mergedEntry : mergedStacks.entrySet()) {
						ItemStack singleStack = mergedEntry.getKey();
						int stackSize = mergedEntry.getValue();

						ItemOdds itemOdds = mRolledItemCounts.computeIfAbsent(singleStack, k -> new ItemOdds());
						itemOdds.mRolled++;
						itemOdds.mTotalRolled += stackSize;
						itemOdds.mSizeRollCount.put(stackSize,
							itemOdds.mSizeRollCount.getOrDefault(stackSize, 0) + 1);
						mTotalItems += stackSize;
					}
				}

				if (mRollsDone != numRolls) {
					// Keep rolling
					progressBar.progress((float) mRollsDone / (float) numRolls);
					progressBar.name(Component.text("Simulating Loot (" + mRollsDone + "/" + numRolls + ")...",
						NamedTextColor.BLUE));
				} else {
					// Done
					audience.hideBossBar(progressBar);

					audience.sendMessage(Component.text(
						"Statistics after rolling " + lootTable.getKey() + " " + numRolls + " times:",
						NamedTextColor.GOLD));
					boolean itemParity = false;
					for (Map.Entry<ItemStack, ItemOdds> itemOddsEntry
						: mRolledItemCounts.entrySet().stream().sorted(new OddsSorter()).toList()) {
						ItemStack item = itemOddsEntry.getKey();
						ItemOdds itemOdds = itemOddsEntry.getValue();

						audience.sendMessage(Component.text(
								String.format(
									"- %6.2f%% (1 in %3.1f) chance of ",
									100.0f * itemOdds.mRolled / numRolls,
									(float) numRolls / (float) itemOdds.mRolled
								),
								itemParity ? NamedTextColor.BLUE : NamedTextColor.GREEN)
							.append(item.displayName()
								.hoverEvent(item))
							.append(Component.text(
								String.format(" in %d rolls; average count %4.2f",
								itemOdds.mRolled,
								(float) itemOdds.mTotalRolled / (float) itemOdds.mRolled)
							)));
						itemParity = !itemParity;

						if (itemOdds.mSizeRollCount.size() <= 1) {
							continue;
						}

						boolean sizeParity = false;
						for (Map.Entry<Integer, Integer> sizeEntries : itemOdds.mSizeRollCount.entrySet()) {
							int count = sizeEntries.getKey();
							int occurrences = sizeEntries.getValue();

							audience.sendMessage(Component.text(String.format(
									"  - %6.2f%% (1 in %3.1f) rolled %d items",
									100.0f * occurrences / itemOdds.mRolled,
									(float) itemOdds.mRolled / (float) occurrences,
									count
								),
								sizeParity ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY));
							sizeParity = !sizeParity;
						}
					}

					audience.sendMessage(Component.text("Total items rolled: " + mTotalItems,
						NamedTextColor.GOLD));

					// Remember to cancel the event when done!
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}
}
