package com.playmonumenta.plugins.cooking;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class CookingTableInventory {

	private final Plugin mPlugin;
	private final Player mPlayer;
	private final Inventory mInventory;

	CookingTableInventory(Plugin plugin, Player player) {
		mPlugin = plugin;
		mPlayer = player;
		int mTableTier = CookingUtils.getTableTierFromPlayer(player);
		mInventory = Bukkit.createInventory(mPlayer, 54, "Cooking Table Tier " + mTableTier);
		mInventory.setContents(CookingConsts.BASE_TABLE_CONTENTS_TIERED[mTableTier]);

	}

	void openTable() {
		mPlayer.openInventory(mInventory);
	}

	void closeTable() {
		for (Integer i : this.getUsedSlots()) {
			ItemStack item = mInventory.getItem(i);
			mPlayer.getInventory().addItem(item);
		}
	}

	public Inventory getInventory() {
		return mInventory;
	}

	private ItemStack hasValidRecipe() {
		ArrayList<String> errors = new ArrayList<>();
		String json;
		CookingItemObject obj;
		// check if there is a base item
		ItemStack testItem = mInventory.getItem(11);
		if (testItem == null) {
			errors.add(ChatColor.RED + "Missing Base cooking item in Base cooking item Slot");
		} else {
			json = CookingUtils.extractItemDataFromFirstLoreLine(testItem);
			obj = CookingUtils.cookingItemObjectFromJson(json);
			if (obj == null) {
				errors.add(ChatColor.RED + "Item placed in the Base ingredient slot is not a cooking item");
			} else if (obj.getType() != CookingItemType.BASE) {
				errors.add(ChatColor.RED + "Item placed in the Base ingredient slot is a " + obj.getType());
			}
		}

		// check the tool slot
		testItem = mInventory.getItem(14);
		if (testItem != null) {
			json = CookingUtils.extractItemDataFromFirstLoreLine(testItem);
			obj = CookingUtils.cookingItemObjectFromJson(json);
			if (obj == null) {
				errors.add(ChatColor.RED + "Item placed in the Tool slot is not a cooking item");
			} else if (obj.getType() != CookingItemType.TOOL) {
				errors.add(ChatColor.RED + "Item placed in the Tool slot is a " + obj.getType());
			}
		}

		// check ingredient slots
		Map<String, Integer> metIngredients = new HashMap<>();
		for (int i = 0; i < 8; i++) {
			testItem = mInventory.getItem(29 + i % 4 + (i / 4) * 9);
			if (testItem != null && !testItem.equals(CookingConsts.BASE_TABLE_CONTENTS_NYU)) {
				json = CookingUtils.extractItemDataFromFirstLoreLine(testItem);
				obj = CookingUtils.cookingItemObjectFromJson(json);
				if (obj == null) {
					errors.add(ChatColor.RED + "Item placed in the Secondary ingredient slot " + (i + 1) + " is not a cooking item");
				} else if (obj.getType() != CookingItemType.SECONDARY) {
					errors.add(ChatColor.RED + "Item placed in the Secondary ingredient slot " + (i + 1) + " is a " + obj.getType());
				} else if (metIngredients.containsKey(obj.getName())) {
					errors.add(ChatColor.RED + "Duplicate Secondary ingredient placed in slot " + (i + 1));
				} else {
					metIngredients.put(obj.getName(), 1);
				}
			}
		}

		if (errors.size() > 0) {
			ItemStack out = new ItemStack(Material.BARRIER);
			ItemMeta meta = out.getItemMeta();
			meta.setDisplayName(CookingConsts.ERROR_ITEM_NAME);
			meta.setLore(errors);
			out.setItemMeta(meta);
			return out;
		}
		return null;
	}

	// updates the output preview by taking account of placed items in the table
	void updateTable() {
		ItemStack outItem = this.hasValidRecipe();
		if (outItem != null) {
			// not a valid recipe
			this.setItemStackInSlot(outItem, 25);
			return;
		}
		this.calculate();
	}

	private void calculate() {
		// init
		CookingItemObject out = new CookingItemObject();
		out.setType(CookingItemType.MEAL);
		out.setRegion(ItemUtils.ItemRegion.MONUMENTA);
		out.setTier(ItemUtils.ItemTier.DISH);
		CookingItemObject[] itemsUsed = this.parseUsedSlots();
		out.setMaterial(CookingUtils.cookedMaterialVersionOf(itemsUsed[0].getMaterial()));
		// effects
		Map<CookingEffectsEnum, Integer> effects = this.calculateEffects(itemsUsed);
		Integer i1 = effects.remove(CookingEffectsEnum.ITEM_AMOUNT_ADD);
		Integer i2 = effects.remove(CookingEffectsEnum.ITEM_AMOUNT_MULT);
		int itemAmount = i1 != null ? i1 : 0;
		int itemAmountmult = i2 != null ? i2 : 0;
		itemAmount *= (int)(1 + (itemAmountmult / (float)100));
		out.setConsumeEffects(effects);
		// name
		if (itemsUsed.length > 1) {
			out.setName("&r" + this.calculateName(itemsUsed) + "Dish");
		} else {
			out.setName("&rCooked " + ChatColor.stripColor(itemsUsed[0].getName()));
		}
		// lore
		out.setLore(this.calculateLore(itemsUsed));
		// end
		ItemStack itemStack = out.toCookingItemStack();
		itemStack.setAmount(itemAmount + 1);
		this.setItemStackInSlot(itemStack, 25);
	}

	private String calculateLore(CookingItemObject[] items) {
		StringBuilder out = new StringBuilder();
		out.append("Ingredients Used:\n");
		for (int i = 0; i < items.length; i++) {
			String name = ChatColor.stripColor(items[i].getName().replace("&", "§"));
			if (i != items.length - 1) {
				out.append(name).append(",");
			}
			if (i % 2 == 0) {
				out.append(" ");
			} else {
				out.append("\n");
			}
		}
		return out.toString();
	}

	private ArrayList<String> getNameModifiersFromUsedItems(CookingItemObject[] items) {
		ArrayList<String> out = new ArrayList<>();
		for (CookingItemObject i : items) {
			Collections.addAll(out, i.getNameModifiers());
		}
		return out;
	}

	private static boolean entryIsMatching(String[] inputs, ArrayList<String> modifiers) {
		i : for (String in : inputs) {
			for (String mod : modifiers) {
				if (mod.equals(in)) {
					modifiers.remove(mod);
					continue i;
				}
			}
			return false;
		}
		return true;
	}

	private static void removeFromList(String[] strs, ArrayList<String> lst) {
		for (String s : strs) {
			lst.remove(s);
		}
	}

	private String calculateName(CookingItemObject[] items) {
		ArrayList<String> nameModifiers = getNameModifiersFromUsedItems(items);
		ArrayList<String> finalNameModifiers = new ArrayList<>();
		CookingModifierCombos.ComboEntry[] list = CookingModifierCombos.list;
		for (int i = 0; i < list.length; i++) {
			CookingModifierCombos.ComboEntry entry = list[i];
			ArrayList<String> allModifiers = new ArrayList<>();
			allModifiers.addAll(finalNameModifiers);
			allModifiers.addAll(nameModifiers);
			if (entryIsMatching(entry.mInputs, allModifiers)) {
				removeFromList(entry.mInputs, finalNameModifiers);
				removeFromList(entry.mInputs, nameModifiers);
				finalNameModifiers.add(entry.mOutput);
				i = 0;
			}
		}
		// compile all final modifiers to a single string
		if (finalNameModifiers.size() == 0) {
			return "";
		} else {
			StringBuilder out = new StringBuilder();
			for (String finalNameModifier : finalNameModifiers) {
				out.append(finalNameModifier);
				out.append(" ");
			}
			return out.toString();
		}
	}

	private Map<CookingEffectsEnum, Integer> calculateEffects(CookingItemObject[] items) {
		Map<CookingEffectsEnum, Integer> out = this.harvestEasyEffects(items);
		Map<CookingEffectsEnum, Map<Integer, Integer>> harvested = this.harvestPotionEffects(items);
		// for each effect
		for (Map.Entry<CookingEffectsEnum, Map<Integer, Integer>> entry : harvested.entrySet()) {
			CookingEffectsEnum effect = entry.getKey();
			Map<Integer, Integer> potencyDurationMap = entry.getValue();
			Integer potency = 0;
			Integer duration;
			// get the highest potency, and its duration
			for (Integer p : potencyDurationMap.keySet()) {
				if (p > potency) {
					potency = p;
				}
			}
			duration = potencyDurationMap.get(potency);
			out.put(effect, potency);
			out.put(CookingEffectsEnum.valueOf(effect.toString().replace("POTENCY", "DURATION")), duration);
		}
		return this.applySimpleEffects(out);
	}

	private Map<CookingEffectsEnum, Integer> applySimpleEffects(Map<CookingEffectsEnum, Integer> out) {
		applySimpleEffect(out, CookingEffectsEnum.POTENCY_ADD, CookingEffectsEnum.Kind.POTENCY, "+");
		applySimpleEffect(out, CookingEffectsEnum.DURATION_ADD, CookingEffectsEnum.Kind.DURATION, "+");
		applySimpleEffect(out, CookingEffectsEnum.DURATION_MULT, CookingEffectsEnum.Kind.DURATION, "*");
		applySimpleEffect(out, CookingEffectsEnum.FOOD_VALUES_ADD, CookingEffectsEnum.Kind.FOOD, "+");
		applySimpleEffect(out, CookingEffectsEnum.FOOD_VALUES_MULT, CookingEffectsEnum.Kind.FOOD, "*");
		return out;
	}

	private static void applySimpleEffect(Map<CookingEffectsEnum, Integer> out, CookingEffectsEnum effect,
										  CookingEffectsEnum.Kind kindToApply, String method) {
		Integer val = out.remove(effect);
		if (val != null && val != 0) {
			for (CookingEffectsEnum e : out.keySet()) {
				if (e.getKind() == kindToApply) {
					if (method.equals("+")) {
						out.put(e, out.get(e) + val);
					} else if (method.equals("*")) {
						out.put(e, Math.round(out.get(e) * (1 + (val / (float) 100))));
					}
				}
			}
		}
	}

	private Map<CookingEffectsEnum, Integer> harvestEasyEffects(CookingItemObject[] items) {
		Map<CookingEffectsEnum, Integer> out = new TreeMap<>();
		for (CookingItemObject i : items) {
			// consume effects
			Map<CookingEffectsEnum, Integer> consumeEffects = i.getConsumeEffects();
			for (Map.Entry<CookingEffectsEnum, Integer> entry : consumeEffects.entrySet()) {
				if (entry.getKey().getKind() == CookingEffectsEnum.Kind.FOOD
					|| entry.getKey().getKind() == CookingEffectsEnum.Kind.OTHER) {
					CookingEffectsEnum effect = entry.getKey();
					Integer value = entry.getValue();
					out.put(effect, out.getOrDefault(effect, 0) + value);
				}
			}
			// cooking effects
			Map<CookingEffectsEnum, Integer> cookingEffects = i.getCookingEffects();
			for (Map.Entry<CookingEffectsEnum, Integer> entry : cookingEffects.entrySet()) {
				if (entry.getKey().getKind() == CookingEffectsEnum.Kind.FOOD
					|| entry.getKey().getKind() == CookingEffectsEnum.Kind.OTHER) {
					CookingEffectsEnum effect = entry.getKey();
					Integer value = entry.getValue();
					out.put(effect, out.getOrDefault(effect, 0) + value);
				}
			}
		}
		return out;
	}

	private Map<CookingEffectsEnum, Map<Integer, Integer>> harvestPotionEffects(CookingItemObject[] itemsUsed) {
		Map<CookingEffectsEnum, Map<Integer, Integer>> out = new HashMap<>();
		for (CookingItemObject i : itemsUsed) {
			// consume effects
			Map<CookingEffectsEnum, Integer> consumeEffects = i.getConsumeEffects();
			for (Map.Entry<CookingEffectsEnum, Integer> entry : consumeEffects.entrySet()) {
				if (entry.getKey().getKind() != CookingEffectsEnum.Kind.POTENCY) {
					continue;
				}
				CookingEffectsEnum effect = entry.getKey();
				Integer potency = entry.getValue();
				Integer duration = consumeEffects.get(CookingEffectsEnum.valueOf(effect.toString().replace("POTENCY", "DURATION")));
				Map<Integer, Integer> tmp = out.getOrDefault(effect, new TreeMap<>());
				tmp.put(potency, duration + tmp.getOrDefault(potency, 0));
				out.put(effect, tmp);
			}
			// cooking effects
			Map<CookingEffectsEnum, Integer> cookingEffects = i.getCookingEffects();
			for (Map.Entry<CookingEffectsEnum, Integer> entry : cookingEffects.entrySet()) {
				if (entry.getKey().getKind() != CookingEffectsEnum.Kind.POTENCY) {
					continue;
				}
				CookingEffectsEnum effect = entry.getKey();
				Integer potency = entry.getValue();
				Integer duration = cookingEffects.get(CookingEffectsEnum.valueOf(effect.toString().replace("POTENCY", "DURATION")));
				Map<Integer, Integer> tmp = out.getOrDefault(effect, new TreeMap<>());
				tmp.put(potency, duration + tmp.getOrDefault(potency, 0));
				out.put(effect, tmp);
			}
		}
		return out;
	}

	private Integer[] getUsedSlots() {
		int[] allPossibleSlots = {11, 14, 29, 30, 31, 32, 38, 39, 40, 41};
		ArrayList<Integer> out = new ArrayList<>();
		for (int possibleSlot : allPossibleSlots) {
			ItemStack item = mInventory.getItem(possibleSlot);
			String json = CookingUtils.extractItemDataFromFirstLoreLine(item);
			if (json == null) {
				continue;
			}
			out.add(possibleSlot);
		}
		return out.toArray(new Integer[0]);
	}

	private CookingItemObject[] parseUsedSlots() {
		ArrayList<CookingItemObject> out = new ArrayList<>();
		// visit all used slots
		for (int slot : this.getUsedSlots()) {
			ItemStack item = mInventory.getItem(slot);
			String json = CookingUtils.extractItemDataFromFirstLoreLine(item);
			if (json == null) {
				continue;
			}
			out.add(CookingUtils.cookingItemObjectFromJson(json));
		}
		return out.toArray(new CookingItemObject[0]);
	}

	void setCookingItemInSlot(CookingItemObject item, int slot) {
		this.setItemStackInSlot(item.toCookingItemStack(), slot);
	}

	private void setItemStackInSlot(ItemStack item, int slot) {
		mInventory.setItem(slot, item);
		this.visualUpdate();
	}


	private void visualUpdate() {
		mPlugin.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, mPlayer::updateInventory, 0L);
	}

	void cookAll() {
		ItemStack outItem = mInventory.getItem(25);
		// determine maximum amount of times the item can be cooked
		int maxPossibleCooks = outItem.getType().getMaxStackSize() / outItem.getAmount();
		for (Integer slot : this.getUsedSlots()) {
			if (slot != 14) {
				ItemStack item = mInventory.getItem(slot);
				int amount = item.getAmount();
				if (amount < maxPossibleCooks) {
					maxPossibleCooks = amount;
				}
			}
		}
		// actually cook
		for (Integer slot : this.getUsedSlots()) {
			if (slot != 14) {
				ItemStack item = mInventory.getItem(slot);
				item.setAmount(item.getAmount() - maxPossibleCooks);
				mInventory.setItem(slot, item);
			}
		}
		outItem.setAmount(outItem.getAmount() * maxPossibleCooks);
		mPlayer.getInventory().addItem(outItem);
	}

	void cookOne() {
		for (Integer slot : this.getUsedSlots()) {
			if (slot != 14) {
				ItemStack item = mInventory.getItem(slot);
				item.setAmount(item.getAmount() - 1);
				mInventory.setItem(slot, item);
			}
		}
		ItemStack outItem = mInventory.getItem(25);
		mPlayer.getInventory().addItem(outItem);
	}
}
