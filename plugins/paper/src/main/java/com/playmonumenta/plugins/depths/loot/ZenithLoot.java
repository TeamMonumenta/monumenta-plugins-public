package com.playmonumenta.plugins.depths.loot;

import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Vector;

public class ZenithLoot {

	public static final NamespacedKey CCS_KEY = NamespacedKeyUtils.fromString("epic:r3/items/currency/archos_ring");
	public static final NamespacedKey HCS_KEY = NamespacedKeyUtils.fromString("epic:r3/items/currency/hyperchromatic_archos_ring");
	public static final NamespacedKey POTION_KEY = NamespacedKeyUtils.fromString("epic:r3/depths2/potion_roll");
	public static final NamespacedKey RELIC_KEY = NamespacedKeyUtils.fromString("epic:r3/depths2/relic_roll");
	public static final NamespacedKey GEODE_KEY = NamespacedKeyUtils.fromString("epic:r3/items/currency/indigo_blightdust");
	public static final NamespacedKey AUGMENT_KEY = NamespacedKeyUtils.fromString("epic:r3/items/currency/weightedfortitude");
	public static final NamespacedKey POME_KEY = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_pome");
	public static final NamespacedKey TROPHY_KEY = NamespacedKeyUtils.fromString("epic:r3/depths2/uriddans_eternal_call");
	public static final NamespacedKey HALLOWEEN_KEY = NamespacedKey.fromString("epic:event/halloween2019/creepers_delight");

	public static final String DAILY_TAG = "zenith_daily";

	public static final int RELIC_CHANCE = 250;
	//Scales the currency drops by x per ascension level
	public static final double CURRENCY_PER_ASC_LEVEL = 0.1;
	//Scales the dungeon mats and relics by x per ascension level
	public static final double DUNGEON_PER_ASC_LEVEL = 0.2;

	//Starts at ascension level 0, ends at 15 odds
	public static final int[] ASCENSION_CHARM_ROLLS_SCORE = {18, 18, 17, 16, 15, 16, 15, 14, 13, 12, 13, 12, 11, 10, 9, 9};

	public static final Vector LOOT_ROOM_LOOT_OFFSET = new Vector(-21, 5, 0);

	public static final List<int[]> ASCENSION_CHARM_RARITY_ODDS = new ArrayList<>();

	static {
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{50, 35, 14, 1, 0}); //0
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{45, 30, 19, 5, 1});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{45, 30, 19, 5, 1});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{45, 30, 19, 5, 1});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{45, 30, 19, 5, 1});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{40, 25, 23, 10, 2});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{40, 25, 23, 10, 2});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{40, 25, 23, 10, 2});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{40, 25, 23, 10, 2});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{40, 25, 23, 10, 2});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{30, 25, 27, 15, 3});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{30, 25, 27, 15, 3});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{30, 25, 27, 15, 3});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{30, 25, 27, 15, 3});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{30, 25, 27, 15, 3});
		ASCENSION_CHARM_RARITY_ODDS.add(new int[]{20, 25, 31, 20, 4}); //15

	}


	/**
	 * Generates loot at the loot room location for the given treasure score
	 * @param loc loot room spawn location
	 * @param treasureScore amount of loot to spawn
	 */
	public static void generateLoot(Location loc, int treasureScore, Player p, boolean trophy, int ascensionLevel, boolean victory) {

		//Load the main reward table with ccs and depths mats and spawn it in
		LootContext context = new LootContext.Builder(loc).build();
		//Money beyond 54 treasure score is reduced to 1 ccs/treasure, first 54 is worth 2ccs each
		int money = 0;
		if (treasureScore <= 54) {
			money = (int) (treasureScore * ((CURRENCY_PER_ASC_LEVEL * ascensionLevel) + 1) * 2);
		} else {
			money = (int) (54 * ((CURRENCY_PER_ASC_LEVEL * ascensionLevel) + 1) * 2);
			money += (int) ((treasureScore - 54) * ((CURRENCY_PER_ASC_LEVEL * ascensionLevel) + 1));

		}
		int hcs = money / 64;
		int ccs = money % 64;

		LootTable ccsTable = Bukkit.getLootTable(CCS_KEY);
		LootTable hcsTable = Bukkit.getLootTable(HCS_KEY);
		LootTable potionTable = Bukkit.getLootTable(POTION_KEY);

		Random r = new Random();
		Collection<ItemStack> ccsLoot = ccsTable.populateLoot(FastUtils.RANDOM, context);
		Collection<ItemStack> hcsLoot = hcsTable.populateLoot(FastUtils.RANDOM, context);
		//Give CCS
		for (int i = 0; i < ccs; i++) {
			if (!ccsLoot.isEmpty()) {
				for (ItemStack item : ccsLoot) {
					Item lootOnGround = loc.getWorld().dropItem(loc, item);
					lootOnGround.setGlowing(true);
				}
			}
		}

		//Give HCS
		for (int i = 0; i < hcs; i++) {
			if (!hcsLoot.isEmpty()) {
				for (ItemStack item : hcsLoot) {
					Item lootOnGround = loc.getWorld().dropItem(loc, item);
					lootOnGround.setGlowing(true);
				}
			}
		}

		//Give Potions / blocks / food
		for (int i = 0; i < treasureScore * ((DUNGEON_PER_ASC_LEVEL * ascensionLevel) + 1); i++) {
			if (r.nextInt(50) == 0) {
				Collection<ItemStack> potionLoot = potionTable.populateLoot(FastUtils.RANDOM, context);

				if (!potionLoot.isEmpty()) {
					for (ItemStack item : potionLoot) {
						loc.getWorld().dropItem(loc, item);
					}
				}
			}

			if (i < 32) {
				ItemStack fillerBlocks = new ItemStack(Material.QUARTZ_BLOCK, 2);
				loc.getWorld().dropItem(loc, fillerBlocks);
			}

			if (i < 64) {
				ItemStack fillerFood = new ItemStack(Material.COOKED_BEEF, 1);
				loc.getWorld().dropItem(loc, fillerFood);
			}
		}

		//Roll for geodes
		LootTable geodeTable = Bukkit.getLootTable(GEODE_KEY);
		Collection<ItemStack> loot = geodeTable.populateLoot(FastUtils.RANDOM, context);
		if (!loot.isEmpty()) {
			for (int i = (int) (treasureScore * ((DUNGEON_PER_ASC_LEVEL * ascensionLevel) + 1)); i >= 12; i -= 12) {
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
			//Get num from 0-11
			int roll = r.nextInt(12);
			if (roll < treasureScore) {
				//Drop an extra geode
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
			if (!ScoreboardUtils.checkTag(p, DAILY_TAG) && victory) {
				//drop two mats for daily bonus and add tag
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
				p.addScoreboardTag(DAILY_TAG);
				DepthsUtils.sendFormattedMessage(p, DepthsContent.CELESTIAL_ZENITH, "You received your daily Zenith clear bonus!");
			}
		}

		//Roll for augments
		LootTable augmentTable = Bukkit.getLootTable(AUGMENT_KEY);
		Collection<ItemStack> augmentLoot = augmentTable.populateLoot(FastUtils.RANDOM, context);
		if (!augmentLoot.isEmpty()) {
			for (int i = (int) (treasureScore * ((CURRENCY_PER_ASC_LEVEL * ascensionLevel) + 1)); i >= 54; i -= 54) {
				for (ItemStack item : augmentLoot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
			//Get num from 0-53
			int roll = r.nextInt(54);
			if (roll < treasureScore) {
				//Drop an extra geode
				for (ItemStack item : augmentLoot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
		}

		//roll halloween loot
		if (HALLOWEEN_KEY != null && p.hasPermission("monumenta.event.creeperween")) {
			LootTable halloweenTable = Bukkit.getLootTable(HALLOWEEN_KEY);
			if (halloweenTable != null) {
				Collection<ItemStack> lootAgain = halloweenTable.populateLoot(FastUtils.RANDOM, context);
				if (!lootAgain.isEmpty()) {
					for (int i = (int) (treasureScore * ((CURRENCY_PER_ASC_LEVEL * ascensionLevel) + 1)); i >= 20; i -= 20) {
						for (ItemStack item : lootAgain) {
							loc.getWorld().dropItem(loc, item);
						}
					}
					//Get num from 0-19
					if (r.nextInt(20) == 0) {
						//Drop an extra candy
						for (ItemStack item : lootAgain) {
							loc.getWorld().dropItem(loc, item);
						}
					}
				}
			}
		}

		//Roll for endless mode loot - subtract 54 from treasure score to only drop in ascensions and then have a random drop chance

		LootTable pomeTable = Bukkit.getLootTable(POME_KEY);
		if (pomeTable != null) {
			for (int i = 0; i < (int) (treasureScore * ((CURRENCY_PER_ASC_LEVEL * ascensionLevel) + 1)) - 54; i++) {
				// 1/10 chance to drop a pome per treasure score excess in ascension
				if (r.nextInt(10) == 0) {
					loot = pomeTable.populateLoot(FastUtils.RANDOM, context);
					if (!loot.isEmpty()) {
						for (ItemStack item : loot) {
							loc.getWorld().dropItem(loc, item);
						}
					}
				}
			}
		}

		//Delve trophy if max points
		if (trophy) {
			LootTable trophyTable = Bukkit.getLootTable(TROPHY_KEY);

			loot = trophyTable.populateLoot(FastUtils.RANDOM, context);
			if (!loot.isEmpty()) {
				for (ItemStack item : loot) {
					Item lootOnGround = loc.getWorld().dropItem(loc, item);
					PotionUtils.applyColoredGlowing("ZenithLootRoom", lootOnGround, NamedTextColor.DARK_PURPLE, 10000);
				}
			}
		}

		//Roll for relics - treasure score / 250 chance (if above 250, guaranteed drop and subtract relic)
		NamespacedKey relicKey = RELIC_KEY;

		// Unlock additional rare tables at certain ascension thresholds
		if (ascensionLevel >= 12) {
			relicKey = NamespacedKeyUtils.fromString("epic:r3/depths2/relic_roll_asc4");
		} else if (ascensionLevel >= 9) {
			relicKey = NamespacedKeyUtils.fromString("epic:r3/depths2/relic_roll_asc3");
		} else if (ascensionLevel >= 6) {
			relicKey = NamespacedKeyUtils.fromString("epic:r3/depths2/relic_roll_asc2");
		} else if (ascensionLevel >= 3) {
			relicKey = NamespacedKeyUtils.fromString("epic:r3/depths2/relic_roll_asc1");
		}

		LootTable relicTable = Bukkit.getLootTable(relicKey);

		if (relicTable == null) {
			return;
		}

		for (int score = (int) (treasureScore * ((DUNGEON_PER_ASC_LEVEL * ascensionLevel) + 1)); score > 0; score -= RELIC_CHANCE) {
			int roll = r.nextInt(RELIC_CHANCE);
			if (roll < score) {
				//Drop random relic
				loot = relicTable.populateLoot(FastUtils.RANDOM, context);
				if (!loot.isEmpty()) {
					for (ItemStack item : loot) {
						Item lootOnGround = loc.getWorld().dropItem(loc, item);
						lootOnGround.setGlowing(true);
						PotionUtils.applyColoredGlowing("ZenithLootRoom", lootOnGround, NamedTextColor.RED, 10000);
						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
					}
				}
			}
		}

		//Generate depths 2 charms - treasure score / ascension chance (if above level odds, guaranteed drop and subtract)

		for (int score = treasureScore; score > 0; score -= ASCENSION_CHARM_ROLLS_SCORE[ascensionLevel]) {
			int roll = r.nextInt(ASCENSION_CHARM_ROLLS_SCORE[ascensionLevel]);
			if (roll < score) {
				//Drop charm
				int power = r.nextInt(5) + 1;
				int level = getCharmRarityByAscensionLevel(r, ascensionLevel);
				ItemStack item = CharmFactory.generateCharm(level, power, 0, null, null, null, null, null);
				Item lootOnGround = loc.getWorld().dropItem(loc, item);
				lootOnGround.setGlowing(true);
				PotionUtils.applyColoredGlowing("ZenithLootRoom", lootOnGround, DepthsUtils.getRarityNamedTextColor(level), 10000);
			}
		}
	}

	private static int getCharmRarityByAscensionLevel(Random r, int ascensionLevel) {
		int roll = r.nextInt(100) + 1;
		for (int i = 0; i < 5; i++) {
			if (roll < addUpChances(i, ASCENSION_CHARM_RARITY_ODDS.get(ascensionLevel))) {
				return i + 1;
			}
		}
		return 1;
	}

	private static int addUpChances(int rarity, int[] chances) {
		int chance = 1;
		for (int i = 0; i <= rarity; i++) {
			chance += chances[i];
		}
		return chance;
	}

}
