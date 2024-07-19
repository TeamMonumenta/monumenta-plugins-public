package com.playmonumenta.plugins.depths.charmfactory;

import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableItemNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class CharmFactory {

	public static final int[] CHARM_BUDGET_PER_POWER = {2, 4, 7, 11, 16};
	public static final double[] CHARM_BUDGET_PER_LEVEL = {2, 3, 4, 5, 6};
	public static final String CHARM_UUID_KEY = "DEPTHS_CHARM_UUID";
	public static final String CHARM_RARITY_KEY = "DEPTHS_CHARM_RARITY";
	public static final String CHARM_EFFECTS_KEY = "DEPTHS_CHARM_EFFECT";
	public static final String CHARM_ACTIONS_KEY = "DEPTHS_CHARM_ACTIONS";
	public static final String CHARM_ROLLS_KEY = "DEPTHS_CHARM_ROLLS";
	public static final String CHARM_WILDCARD_TREE_CAP_KEY = "DEPTHS_CHARM_WILDCARD_TREE_CAP";
	public static final double TREE_BUDGET_MODIFIER = 1.35;
	public static final double WILDCARD_BUDGET_MODIFIER = 1.8;
	public static final int[] WILDCARD_TREE_CAP_CHANCES = {0, 33, 33, 17, 8, 5, 4};

	public static final Map<String, String> charmConversionMap = Map.ofEntries(
		Map.entry("Precision Strike Range", "Precision Strike Range Requirement"),
		Map.entry("Fireball Range", "Fireball Velocity"),
		Map.entry("Ring of Flames Cooldown", "Igneous Rune Cooldown"),
		Map.entry("Ring of Flames Damage", "Igneous Rune Damage"),
		Map.entry("Ring of Flames Fire Duration", "Igneous Rune Fire Duration"),
		Map.entry("Ring of Flames Bleed Amplifier", "Igneous Rune Buff Amplifier"),
		Map.entry("Ring of Flames Duration", "Igneous Rune Buff Duration"),
		Map.entry("Lightning Bottle Kills Per Bottle", "Lightning Bottle Kill Threshold"),
		Map.entry("Slipstream Cooldown", "Aeroblast Cooldown"),
		Map.entry("Slipstream Jump Boost Amplifier", "Aeroblast Damage"),
		Map.entry("Slipstream Knockback", "Aeroblast Knockback"),
		Map.entry("Slipstream Duration", "Aeroblast Speed Duration"),
		Map.entry("Slipstream Speed Amplifier", "Aeroblast Speed Amplifier"),
		Map.entry("Slipstream Radius", "Aeroblast Size"),

		Map.entry("Divine Beam Cooldown Reduction", ""),

		Map.entry("Totem of Salvation Cooldown", "Spark of Inspiration Cooldown"),
		Map.entry("Totem of Salvation Radius", "Spark of Inspiration Cast Range"),
		Map.entry("Totem of Salvation Healing", "Spark of Inspiration Cooldown Reduction Rate"),
		Map.entry("Totem of Salvation Max Absorption", "Spark of Inspiration Strength Amplifier"),
		Map.entry("Totem of Salvation Duration", "Spark of Inspiration Buff Duration"),
		Map.entry("Totem of Salvation Absorption Duration", "Spark of Inspiration Resistance Duration"),

		Map.entry("Stone Skin Cooldown", "Iron Grip Cooldown"),
		Map.entry("Stone Skin Resistance Amplifier", "Iron Grip Resistance Amplifier"),
		Map.entry("Stone Skin Knockback Resistance", "Iron Grip Damage"),
		Map.entry("Stone Skin Duration", "Iron Grip Resistance Duration"),

		Map.entry("Howling Winds Cooldown", "Thundercloud Form Cooldown"),
		Map.entry("Howling Winds Radius", "Thundercloud Form Radius"),
		Map.entry("Howling Winds Duration", "Thundercloud Form Flight Duration"),
		Map.entry("Howling Winds Velocity", "Thundercloud Form Flight Speed"),
		Map.entry("Howling Winds Cast Range", ""),
		Map.entry("Howling Winds Vulnerability Amplifier", "Thundercloud Form Damage"),

		Map.entry("Metalmancy Cooldown", "Gravity Bomb Cooldown"),
		Map.entry("Metalmancy Damage", "Gravity Bomb Damage"),
		Map.entry("Metalmancy Duration", "Gravity Bomb Radius"),

		Map.entry("Projectile Mastery Damage Multiplier", "Sharpshooter Damage Multiplier"),

		Map.entry("Volcanic Combos Hit Requirement", "Volcanic Combos Cooldown"),
		Map.entry("Frigid Combos Hit Requirement", "Frigid Combos Cooldown")
	);

	public static final Map<String, CharmEffectActions> charmLevelCapMap = Map.ofEntries(
		Map.entry("Bottled Sunlight Bottle Velocity", CharmEffectActions.EPIC),
		Map.entry("Ward of Light Cone Angle", CharmEffectActions.EPIC),
		Map.entry("Fireball Velocity", CharmEffectActions.EPIC),
		Map.entry("Fireball Fire Duration", CharmEffectActions.RARE),
		Map.entry("Flame Spirit Fire Duration", CharmEffectActions.RARE),
		Map.entry("Flamestrike Cone Angle", CharmEffectActions.EPIC),
		Map.entry("Flamestrike Fire Duration", CharmEffectActions.RARE),
		Map.entry("Flamestrike Knockback", CharmEffectActions.EPIC),
		Map.entry("Igneous Rune Fire Duration", CharmEffectActions.RARE),
		Map.entry("Pyroblast Fire Duration", CharmEffectActions.RARE),
		Map.entry("Volcanic Combos Fire Duration", CharmEffectActions.RARE),
		Map.entry("Volcanic Meteor Fire Duration", CharmEffectActions.RARE),
		Map.entry("Cryobox Ice Duration", CharmEffectActions.RARE),
		Map.entry("Frost Nova Ice Duration", CharmEffectActions.RARE),
		Map.entry("Ice Barrier Cast Range", CharmEffectActions.EPIC),
		Map.entry("Ice Barrier Max Length", CharmEffectActions.EPIC),
		Map.entry("Ice Barrier Ice Duration", CharmEffectActions.RARE),
		Map.entry("Ice Lance Ice Duration", CharmEffectActions.RARE),
		Map.entry("Piercing Cold Ice Duration", CharmEffectActions.RARE),
		Map.entry("Chaos Dagger Velocity", CharmEffectActions.EPIC),
		Map.entry("Focused Combos Bleed Amplifier", CharmEffectActions.EPIC),
		Map.entry("Focused Combos Bleed Duration", CharmEffectActions.RARE),
		Map.entry("Scrapshot Recoil Velocity", CharmEffectActions.EPIC),
		Map.entry("Scrapshot Shrapnel Cone Angle", CharmEffectActions.EPIC),
		Map.entry("Steel Stallion Horse Speed", CharmEffectActions.EPIC),
		Map.entry("Steel Stallion Jump Strength", CharmEffectActions.EPIC),
		Map.entry("Wind Walk Velocity", CharmEffectActions.EPIC)
	);

	public static @Nullable ItemStack updateCharm(ItemStack item) {
		ItemUpdateHelper.generateItemStats(item);
		return item;
	}

	public static ItemStack generateCharm(int level, int power, long seed, @Nullable List<String> effectOrder, @Nullable List<String> actionOrder, @Nullable List<Double> rollsOrder, @Nullable Component fixedName, @Nullable ItemStack oldItem) {
		// Keeps track of which effect names are already in the charm
		List<String> activeEffects = new ArrayList<>();
		// Keep track of text components to put them in sorted order later
		List<Component> charmTextLines = new ArrayList<>();

		String chosenAbility = null;
		DepthsTree chosenTree = null;
		boolean isTreeLocked = false;
		int budget = 0;
		boolean hasNegative = false;

		//Initialize the random generator- in the case this is reloading a generated charm, use the UUID seed in the nbt data

		if (seed == 0) {
			//Generate unique long value for the charm
			//Stackoverflow said this was good enough..
			seed = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
		}

		Random r = new Random(seed);
		ItemStack item = new ItemStack(Material.STONE);
		if (oldItem != null) {
			item.setType(oldItem.getType());
			/*
			NBT.modify(item, nbt -> {
				ReadableNBT infusions = NBT.get(oldItem, rnbt -> {
					return ItemStatUtils.getInfusions(rnbt);
				});
				if (infusions != null) {
					ReadWriteNBT newInfusions = ItemStatUtils.addPlayerModified(nbt).getOrCreateCompound(InfusionType.KEY);
					newInfusions.mergeCompound(infusions);
				}
			});
			*/
		}

		// Add seed
		long finalSeed = seed;
		NBT.modify(item, nbt -> {
			ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(nbt);
			playerModified.setLong(CHARM_UUID_KEY, finalSeed);
			playerModified.setInteger(CHARM_RARITY_KEY, level);
		});
		ItemStatUtils.setCharmPower(item, power);
		ItemStatUtils.editItemInfo(item, Region.RING, Tier.ZENITH_CHARM, Masterwork.NONE, Location.ZENITH);


		// Split between types of charms
		int charmType = r.nextInt(10);
		if (power > 3) {
			charmType = Math.max(charmType + 2, 4);
		}
		// First- single ability charm

		if (charmType < 4) { //40% chance
			// Single ability charm
			budget = (int) Math.floor(CHARM_BUDGET_PER_POWER[power - 1] * CHARM_BUDGET_PER_LEVEL[level - 1]);

			// Generate starter ability at level of charm
			CharmEffects effect = applyRandomCharmEffect(null, null, level, item, r, activeEffects, false, true, effectOrder, rollsOrder, charmTextLines);
			if (effect != null) {
				chosenTree = effect.mTree;
				chosenAbility = effect.mAbility;
			}
		} else if (charmType < 9) {
			//Tree locked charm- 50% chance
			budget = (int) Math.floor(CHARM_BUDGET_PER_POWER[power - 1] * CHARM_BUDGET_PER_LEVEL[level - 1] * TREE_BUDGET_MODIFIER);
			// Generate starter ability at level of charm
			CharmEffects effect = applyRandomCharmEffect(null, null, level, item, r, activeEffects, false, false, effectOrder, rollsOrder, charmTextLines);
			if (effect != null) {
				chosenTree = effect.mTree;
				isTreeLocked = true;
			}
		} else {
			//10% chance of wildcard charm- this one doesn't care about locked tree or ability
			budget = (int) Math.floor(CHARM_BUDGET_PER_POWER[power - 1] * CHARM_BUDGET_PER_LEVEL[level - 1] * WILDCARD_BUDGET_MODIFIER);
			// Generate starter ability at level of charm
			CharmEffects effect = applyRandomCharmEffect(null, null, level, item, r, activeEffects, false, false, effectOrder, rollsOrder, charmTextLines);
			if (effect != null) {
				chosenTree = effect.mTree;
			}
			if (getWildcardTreeCap(item) == 0) {
				Random r2 = new Random();
				r2.setSeed(seed);
				int roll = r2.nextInt(0, 100);
				int cap = 1;
				int total = 0;
				while (total <= roll && cap < WILDCARD_TREE_CAP_CHANCES.length) {
					total += WILDCARD_TREE_CAP_CHANCES[cap - 1];
					cap++;
				}
				int finalCap = cap;
				NBT.modify(item, nbt -> {
					ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(nbt);
					playerModified.setInteger(CHARM_WILDCARD_TREE_CAP_KEY, finalCap);
				});
				MMLog.finest("tree cap: " + cap);
			}

		}
		ItemMeta itemMeta = item.getItemMeta();

		//Edit name based on chosen tree
		if (fixedName != null) {
			itemMeta.displayName(fixedName);
			item.setItemMeta(itemMeta);
			randomCharmName(r, chosenTree, new ItemStack(Material.STONE)); //Still run randomizer for seed purposes
		} else {
			String generatedName = randomCharmName(r, chosenTree, item);
			itemMeta.displayName(Component.text(generatedName, DepthsUtils.getRarityTextColor(level)).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.UNDERLINED, false).decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(itemMeta);
			ItemUtils.setPlainName(item, generatedName);
		}

		//Now apply additional effects depending on remaining budget

		while (budget > 0) {

			MMLog.fine("budget is " + budget);

			boolean success = false;
			List<CharmEffectActions> potentialActions = Arrays.asList(CharmEffectActions.values());
			Collections.shuffle(potentialActions, r);

			// frozen: if loading an existing charm
			if (actionOrder != null && actionOrder.size() >= activeEffects.size()) {
				String actionName = actionOrder.get(activeEffects.size() - 1);
				CharmEffectActions action = CharmEffectActions.getEffect(actionName);
				if (action != null) {
					DepthsTree lockedTree = isTreeLocked ? chosenTree : null;
					CharmEffects effect = applyRandomCharmEffect(chosenAbility, lockedTree, action.mRarity, item, r, activeEffects, action.mIsNegative, false, effectOrder, rollsOrder, charmTextLines);
					if (effect == null) {
						MMLog.fine("action failed- " + action.mAction);
					} else {
						budget += action.mBudget;
						success = true;
						NBT.modify(item, nbt -> {
							ItemStatUtils.addPlayerModified(nbt).setString(CHARM_ACTIONS_KEY + (activeEffects.size() - 1), action.mAction);
						});
						MMLog.fine("added nbt- " + action.mAction + " " + (activeEffects.size() - 1));

						if (action.mIsNegative) {
							hasNegative = true;
						}
						MMLog.fine("action success- " + action.mAction);
					}
				}
			}

			// frozen: if creating a new charm
			if (!success) {
				//Iterate through the potential actions until we find a match
				for (CharmEffectActions action : potentialActions) {
					//Skip action if it's rarity is above the budget for this charm, common and uncommon are always fine
					if (action.mRarity > level && action.mRarity > 2) {
						MMLog.fine("action skipped 1- " + action.mAction);

						continue;
					}
					// Skip action if it's negative and we aren't doing an ability only charm
					if ((action.mIsNegative && chosenAbility == null) || (action.mIsNegative && hasNegative)) {
						MMLog.fine("action skipped 2- " + action.mAction);

						continue;
					}
					//Skip action if we do not have the budget for it
					if (budget + action.mBudget < 0) {
						MMLog.fine("action skipped 3- " + action.mAction);

						continue;
					}
					MMLog.fine("attempting action- " + action.mAction);

					// Finally, attempt the action. If successful, subtract budget, otherwise try next action
					DepthsTree lockedTree = isTreeLocked ? chosenTree : null;
					CharmEffects effect = applyRandomCharmEffect(chosenAbility, lockedTree, action.mRarity, item, r, activeEffects, action.mIsNegative, false, effectOrder, rollsOrder, charmTextLines);
					if (effect == null) {
						MMLog.fine("action failed- " + action.mAction);
						continue;
					} else {
						budget += action.mBudget;
						success = true;
						NBT.modify(item, nbt -> {
							ItemStatUtils.addPlayerModified(nbt).setString(CHARM_ACTIONS_KEY + (activeEffects.size() - 1), action.mAction);
						});
						MMLog.fine("added nbt- " + action.mAction + " " + (activeEffects.size() - 1));

						if (action.mIsNegative) {
							hasNegative = true;
						}
						MMLog.fine("action success- " + action.mAction);

						break;
					}

				}
			}

			//End loop if we couldn't give a new effect
			if (!success) {
				break;
			}
		}

		// frozen: if we still have budget, attempt to upgrade stats at random to use that budget
		if (budget > 0) {
			for (int i = 0; i < 100; i++) {
				// choose a random action
				int index = FastUtils.randomIntInRange(1, activeEffects.size() - 1);
				CharmEffectActions action = CharmEffectActions.getEffect(NBT.get(item, nbt -> {
					ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
					if (playerModified == null) {
						return null;
					}
					return playerModified.getString(CharmFactory.CHARM_ACTIONS_KEY + index);
				}));
				CharmEffectActions upgraded = CharmEffectActions.upgradeAction(action);
				if (action == null || upgraded == null) {
					continue;
				}
				MMLog.fine("upgrade: action " + action.mAction);
				MMLog.fine("upgrade: upgraded " + upgraded.mAction);
				// check if we have enough budget
				int budgetDifference = upgraded.mBudget - action.mBudget;
				if (budget + budgetDifference < 0) {
					continue;
				}
				budget += budgetDifference;

				CharmEffects effect = CharmEffects.getEffect(activeEffects.get(index));
				if (effect != null && effect.isValidAtLevel(upgraded.mRarity)) { // check if valid at the new rarity
					MMLog.fine("upgrade: effect " + effect.mAbility);
					// update the action NBT to the new action
					NBT.modify(item, nbt -> {
						ItemStatUtils.addPlayerModified(nbt).setString(CHARM_ACTIONS_KEY + index, upgraded.mAction);
					});

					// update the text to the new stat
					double roll = NBT.get(item, nbt -> {
						ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
						if (playerModified == null) {
							return null;
						}
						return playerModified.getDouble(CharmFactory.CHARM_ROLLS_KEY + (index + 1));
					});
					MMLog.fine("upgrade: roll " + roll);
					Component newText = generateCharmText(effect, effect.mRarityValues[upgraded.mRarity - 1], roll, upgraded.mIsNegative, upgraded.mRarity);
					charmTextLines.set(index, newText);
				}
			}
		}

		// Finally, update the item nbt with its list of effects
		for (int count = 1; count <= activeEffects.size(); count++) {
			String key = CHARM_EFFECTS_KEY + count;
			String effect = activeEffects.get(count - 1);
			NBT.modify(item, nbt -> {
				ItemStatUtils.addPlayerModified(nbt).setString(key, effect);
			});
			MMLog.fine("added nbt- " + effect + " " + count);
		}

		// this gets our active effects as strings, turns them into CharmEffects and sorts them via order of declaration
		List<String> sortedEffects = activeEffects.stream()
			.map(CharmEffects::getEffect)
			.filter(Objects::nonNull)
			.sorted(CharmEffects::compareTo)
			.map(CharmEffects::getEffectName).toList();

		List<Integer> indexes = new ArrayList<>();
		for (String effect : sortedEffects) {
			indexes.add(activeEffects.indexOf(effect));
		}

		for (int i : indexes) {
			ItemStatUtils.addCharmEffect(item, sortedEffects.size(), charmTextLines.get(i));
		}

		ItemUpdateHelper.generateItemStats(item);
		return item;
	}

	public static @Nullable CharmEffects applyRandomCharmEffect(@Nullable String ability, @Nullable DepthsTree tree, int level, ItemStack charm, Random r, List<String> effectHistory, boolean isNegative, boolean isFirstSingleAbilityCharm, @Nullable List<String> charmEffectOrder, @Nullable List<Double> charmRollsOrder, List<Component> charmTextOrder) {

		CharmEffects chosenEffect = null;
		Double pastRoll = Double.MIN_VALUE;

		// First off, directly pick the old effect if it exists
		if (charmEffectOrder != null && charmEffectOrder.size() > effectHistory.size()) {
			String effectName = charmEffectOrder.get(effectHistory.size());
			chosenEffect = CharmEffects.getEffect(effectName);
		}
		if (charmEffectOrder != null && charmRollsOrder != null && charmRollsOrder.size() > effectHistory.size()) {
			pastRoll = charmRollsOrder.get(effectHistory.size());
		}

		//Shuffle list, seeded with the randomizer
		List<CharmEffects> charmEffects = Arrays.asList(CharmEffects.values());
		Collections.shuffle(charmEffects, r);

		if (chosenEffect == null) {
			// We didn't have an old charm to update, so we pull a random effect

			if (ability != null) {
				//We have to pick a specific ability

				for (CharmEffects ce : charmEffects) {
					if (isNegative && ce.mIsOnlyPositive) {
						continue;
					}
					if (ce.mAbility.equals(ability) && !effectHistory.contains(ce.mEffectName) && ce.isValidAtLevel(level)) {
						chosenEffect = ce;
						break;
					}
				}
			} else if (tree != null) {
				//We have to pick within the specified tree
				for (CharmEffects ce : charmEffects) {
					if (ce.mTree == tree && !effectHistory.contains(ce.mEffectName) && ce.isValidAtLevel(level) && !isNegative) {
						chosenEffect = ce;
						break;
					}
				}
			} else {
				int treeCap = getWildcardTreeCap(charm);

				//Could be anything- check tree count
				Set<DepthsTree> activeTrees = new HashSet<>();
				for (String s : effectHistory) {
					CharmEffects effect = CharmEffects.getEffect(s);
					if (effect != null) {
						activeTrees.add(effect.mTree);
					}
				}
				for (CharmEffects ce : charmEffects) {
					if (!effectHistory.contains(ce.mEffectName) && ce.isValidAtLevel(level) && !isNegative) {
						//Skip if it's the first effect of a single ability charm and the ability doesn't support it
						if (isFirstSingleAbilityCharm && !ce.mInfo.getSingleAbilityCharm()) {
							continue;
						}
						// Skip this effect if we don't currently have its tree and are capped on trees
						if (treeCap > 0 && activeTrees.size() >= treeCap && !activeTrees.contains(ce.mTree)) {
							continue;
						}
						chosenEffect = ce;
						break;
					}
				}
			}
		}


		if (chosenEffect == null) {
			return null;
		} else if (!chosenEffect.isValidAtLevel(level)) {
			return null;
		}

		//We picked our effect

		//Calculate the actual value!
		double value = chosenEffect.mRarityValues[level - 1];
		// Call even if not used to maintain seed parity
		double random = r.nextDouble();

		if (pastRoll == Double.MIN_VALUE) {
			pastRoll = random;
		}
		double nbtDouble = pastRoll;

		NBT.modify(charm, nbt -> {
			ItemStatUtils.addPlayerModified(nbt).setDouble(CHARM_ROLLS_KEY + (effectHistory.size() + 1), nbtDouble);
		});
		MMLog.fine("added nbt- " + nbtDouble + " " + effectHistory.size());

		Component text = generateCharmText(chosenEffect, value, nbtDouble, isNegative, level);

		// Add to history
		effectHistory.add(chosenEffect.mEffectName);
		charmTextOrder.add(text);
		MMLog.fine("chosen effect- " + chosenEffect.mEffectName + " " + effectHistory.size());

		return chosenEffect;
	}

	public static Component generateCharmText(CharmEffects chosenEffect, double value, double roll, boolean isNegative, int level) {
		//Turn roll into stat
		if (chosenEffect.mVariance != 0) {
			boolean roundToInt = value >= 5;
			value = value + chosenEffect.mVariance * (2 * roll - 1);
			if (roundToInt) {
				value = Math.round(value);
			} else {
				// At most 2 decimal points
				value = Math.round(value * 100) / 100.0;
			}
		}

		if (isNegative) {
			value *= -1;
		}

		//Generate lore on item
		String lore = (value > 0 ? "+" : "") + StringUtils.formatDecimal(value) + (chosenEffect.mIsPercent ? "%" : "") + " " + chosenEffect.mEffectName;
		Component text = null;
		if (isNegative) {
			text = Component.text(lore, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
		} else {
			text = Component.text(lore, DepthsUtils.getRarityTextColor(level)).decoration(TextDecoration.ITALIC, false);
		}

		MMLog.fine("generated charm text " + lore);

		return text;
	}

	/**
	 * Generates a random name to apply to a charm item
	 *
	 * @param r     seeded randomizer to ensure same name when regenerating charms
	 * @param tree  the depths tree that the first ability is based in, making it eligible for unique names
	 * @param charm the charm item to change the base value of
	 * @return randomized charm name from three sample selections
	 */
	public static String randomCharmName(Random r, @Nullable DepthsTree tree, ItemStack charm) {
		String a;
		String b;
		String c;

		List<CharmAdjectives> nameA = new ArrayList<>(Arrays.asList(CharmAdjectives.values()));
		List<CharmNounItems> nameB = new ArrayList<>(Arrays.asList(CharmNounItems.values()));
		List<CharmNounConcepts> nameC = new ArrayList<>(Arrays.asList(CharmNounConcepts.values()));

		if (tree != null) {
			nameA.removeIf(x -> x.mTree != null && x.mTree != tree);
			nameB.removeIf(x -> x.mTree != null && x.mTree != tree);
			nameC.removeIf(x -> x.mTree != null && x.mTree != tree);
		} else {
			nameA.removeIf(x -> x.mTree != null);
			nameB.removeIf(x -> x.mTree != null);
			nameC.removeIf(x -> x.mTree != null);
		}

		CharmAdjectives adj = nameA.get(r.nextInt(nameA.size()));
		a = adj.mName;

		CharmNounItems ni = nameB.get(r.nextInt(nameB.size()));
		b = ni.mName;
		charm.setType(ni.mBaseItem);

		CharmNounConcepts nc = nameC.get(r.nextInt(nameC.size()));
		c = nc.mName;

		return a + " " + b + " of " + c;
	}

	public static int getZenithCharmRarity(ItemStack item) {
		return NBT.get(item, nbt -> {
			return getZenithCharmRarity(nbt);
		});
	}

	public static int getZenithCharmRarity(ReadableItemNBT nbt) {
		ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
		if (playerModified == null) {
			// Not a Zenith charm/no rarity
			return 0;
		}
		return playerModified.getInteger(CharmFactory.CHARM_RARITY_KEY);
	}

	public static @Nullable Component getZenithCharmRarityComponent(ReadableItemNBT nbt) {
		ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
		if (playerModified == null) {
			return null;
		}
		int rarity = playerModified.getInteger(CharmFactory.CHARM_RARITY_KEY);
		if (rarity <= 0 || rarity > 6) {
			return null;
		}
		return DepthsUtils.getRarityComponent(rarity);
	}

	public static int getWildcardTreeCap(ItemStack item) {
		return NBT.get(item, nbt -> {
			return getWildcardTreeCap(nbt);
		});
	}

	public static int getWildcardTreeCap(ReadableItemNBT nbt) {
		ReadableNBT playerModified = ItemStatUtils.getPlayerModified(nbt);
		if (playerModified == null || !playerModified.hasTag(CharmFactory.CHARM_WILDCARD_TREE_CAP_KEY)) {
			// Not a Zenith charm/no rarity
			return 0;
		}
		return playerModified.getInteger(CharmFactory.CHARM_WILDCARD_TREE_CAP_KEY);
	}

}
