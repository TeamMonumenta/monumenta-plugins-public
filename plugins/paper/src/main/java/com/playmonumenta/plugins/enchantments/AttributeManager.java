package com.playmonumenta.plugins.enchantments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.Plugin;

import net.md_5.bungee.api.ChatColor;

public class AttributeManager {

	public class AttributeTrie {

		private class AttributeInfo {
			// Store the mainhand separately for optimization; mainhand changes most often
			public double mMainHandFlat = 0;
			public double mMainHandMultiplier = 0;
			public double mOtherFlat = 0;
			public double mOtherMultiplier = 0;

			public double getValue() {
				return (mMainHandFlat + mOtherFlat) * (1 + mMainHandMultiplier + mOtherMultiplier);
			}
		}

		private class Node {
			public Map<Player, AttributeInfo> mAttributeInfoMappings;
			public Node[] mChildren = new Node[ALPHABET_SIZE];
		}

		private static final int ALPHABET_SIZE = 27;	// Lowercase alphabet and space

		private final Node mRoot = new Node();

		// Initializes trie structure
		public void put(String key) {
			Node current = mRoot;

			for (int i = 0; i < key.length(); i++) {
				// ASCII for space is 32, lowercase starts at 97, divided by 32 (2^5) gives remainder of 1, lines up perfectly
				int index = (key.charAt(i)) & 0x1F;
				if (current.mChildren[index] == null) {
					current.mChildren[index] = new Node();
				}

				current = current.mChildren[index];
			}

			current.mAttributeInfoMappings = new HashMap<Player, AttributeInfo>();
		}

		// Resets attributes for the specified player, should be called before adds
		public void reset(Player player, boolean resetMainHandOnly) {
			for (BaseAttribute attribute : mAttributes) {
				AttributeInfo attributeInfo = get(attribute.getProperty()).mAttributeInfoMappings.get(player);

				if (attributeInfo == null) {
					attributeInfo = new AttributeInfo();
				} else if (resetMainHandOnly) {
					attributeInfo.mMainHandFlat = 0;
					attributeInfo.mMainHandMultiplier = 0;
				} else {
					attributeInfo.mMainHandFlat = 0;
					attributeInfo.mMainHandMultiplier = 0;
					attributeInfo.mOtherFlat = 0;
					attributeInfo.mOtherMultiplier = 0;
				}
			}
		}

		// Only allows adds into matching keys, adds to the existing value
		public void add(String key, Player player, double value, boolean isMultiplier, boolean isMainHand) {
			Node node = get(key);

			if (node != null) {
				AttributeInfo attributeInfo = node.mAttributeInfoMappings.get(player);
				if (attributeInfo == null) {
					attributeInfo = new AttributeInfo();
					node.mAttributeInfoMappings.put(player, attributeInfo);
				}

				if (isMultiplier) {
					if (isMainHand) {
						attributeInfo.mMainHandMultiplier += value;
					} else {
						attributeInfo.mOtherMultiplier += value;
					}
				} else {
					if (isMainHand) {
						attributeInfo.mMainHandFlat += value;
					} else {
						attributeInfo.mOtherFlat += value;
					}
				}
			}
		}

		// Retrieves attribute value in the trie
		public double get(String key, Player player) {
			Node node = get(key);

			if (node != null) {
				AttributeInfo attributeInfo = node.mAttributeInfoMappings.get(player);
				if (attributeInfo != null) {
					return attributeInfo.getValue();
				}
			}

			return 0;
		}

		// Returns the end node, null if key not in the trie
		private Node get(String key) {
			Node current = mRoot;

			for (int i = 0; i < key.length(); i++) {
				if (current == null) {
					return null;
				}

				// ASCII for space is 32, lowercase starts at 97, divided by 32 (2^5) gives remainder of 1, lines up perfectly
				current = current.mChildren[(key.charAt(i)) & 0x1F];
			}

			// If mAttributeInfoMappings is null, then this isn't an end node, so key not found
			return current.mAttributeInfoMappings == null ? null : current;
		}

	}

	private static final String[] ATTRIBUTE_INDICATORS = {
			ChatColor.RESET + "",
			ChatColor.GRAY + "When in main hand:",
			ChatColor.GRAY + "When in off hand:",
			ChatColor.GRAY + "When on head:",
			ChatColor.GRAY + "When on body:",
			ChatColor.GRAY + "When on legs:",
			ChatColor.GRAY + "When on feet:"
	};

	public AttributeTrie mAttributeTrie = new AttributeTrie();
	public List<BaseAttribute> mAttributes = new ArrayList<BaseAttribute>();

	public AttributeManager() {
		mAttributes.add(new AttributeRangedDamage());
		mAttributes.add(new AttributeProjectileSpeed());
		mAttributes.add(new AttributeThrowRate());

		for (BaseAttribute attribute : mAttributes) {
			mAttributeTrie.put(attribute.getProperty());
		}
	}

	public void updateAttributeTrie(Plugin plugin, Player player, boolean updateMainHandOnly) {
		plugin.mAttributeManager.mAttributeTrie.reset(player, updateMainHandOnly);

		PlayerInventory inventory = player.getInventory();
		List<List<String>> lores = new ArrayList<List<String>>();
		ItemStack item;
		item = inventory.getItemInMainHand();
		lores.add(item == null ? null : item.getLore());
		item = inventory.getItemInOffHand();
		lores.add(item == null ? null : item.getLore());
		item = inventory.getHelmet();
		lores.add(item == null ? null : item.getLore());
		item = inventory.getChestplate();
		lores.add(item == null ? null : item.getLore());
		item = inventory.getLeggings();
		lores.add(item == null ? null : item.getLore());
		item = inventory.getBoots();
		lores.add(item == null ? null : item.getLore());

		int iterateUntil = updateMainHandOnly ? 2 : ATTRIBUTE_INDICATORS.length;

		for (int i = 1; i < iterateUntil; i++) {
			if (lores.get(i - 1) == null) {
				continue;
			}

			boolean readAttributes = false;

			for (String loreEntry : lores.get(i - 1)) {
				if (!readAttributes) {
					if (ATTRIBUTE_INDICATORS[i].equals(loreEntry)) {
						readAttributes = true;
					}
				} else {
					if (ATTRIBUTE_INDICATORS[0].equals(loreEntry)) {
						break;
					}

					String loreEntryStripped = "";
					if (loreEntry.startsWith(ChatColor.DARK_GREEN + " ")) {
						loreEntryStripped = loreEntry.substring(3);
					} else if (loreEntry.startsWith(ChatColor.BLUE.toString()) || loreEntry.startsWith(ChatColor.RED.toString())) {
						loreEntryStripped = loreEntry.substring(2);
					}
					String[] loreSegments = loreEntryStripped.split(" ", 2);

					if (loreSegments.length == 2) {
						boolean isMultiplier = false;
						if (loreSegments[0].endsWith("%")) {
							isMultiplier = true;
							loreSegments[0] = loreSegments[0].substring(0, loreSegments[0].length() - 1);
						}

						try {
							// Given all the pre-processing and check of array length, this should be a parsable double, but use a try-catch block just in case
							double value = Double.parseDouble(loreSegments[0]);
							if (isMultiplier) {
								value /= 100;
							}

							plugin.mAttributeManager.mAttributeTrie.add(loreSegments[1], player, value, isMultiplier, i == 1);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
