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
			public double mArrowFlat = 0;
			public double mArrowMultiplier = 0;

			public double getValue() {
				return (mMainHandFlat + mOtherFlat + mArrowFlat) * (1 + mMainHandMultiplier + mOtherMultiplier + mArrowMultiplier);
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
		//If isMainHand, reset arrow attribute anyway, in case player changed to non-arrow projectile launcher
		public void reset(Player player, boolean resetMainHandOnly) {
			for (BaseAttribute attribute : mAttributes) {
				AttributeInfo attributeInfo = get(attribute.getProperty()).mAttributeInfoMappings.get(player);

				if (attributeInfo == null) {
					attributeInfo = new AttributeInfo();
				} else if (resetMainHandOnly) {
					attributeInfo.mMainHandFlat = 0;
					attributeInfo.mMainHandMultiplier = 0;
					attributeInfo.mArrowFlat = 0;
					attributeInfo.mArrowMultiplier = 0;
				} else {
					attributeInfo.mMainHandFlat = 0;
					attributeInfo.mMainHandMultiplier = 0;
					attributeInfo.mOtherFlat = 0;
					attributeInfo.mOtherMultiplier = 0;
					attributeInfo.mArrowFlat = 0;
					attributeInfo.mArrowMultiplier = 0;
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

		public void addArrow(String key, Player player, double value, boolean isMultiplier) {
			Node node = get(key);

			if (node != null) {
				AttributeInfo attributeInfo = node.mAttributeInfoMappings.get(player);
				if (attributeInfo == null) {
					attributeInfo = new AttributeInfo();
					node.mAttributeInfoMappings.put(player, attributeInfo);
				}

				if (isMultiplier) {
					attributeInfo.mArrowMultiplier += value;
				} else {
					attributeInfo.mArrowFlat += value;
				}
			}
		}

		public void resetArrow(Player player) {
			for (BaseAttribute attribute : mAttributes) {
				AttributeInfo attributeInfo = get(attribute.getProperty()).mAttributeInfoMappings.get(player);

				if (attributeInfo == null) {
					attributeInfo = new AttributeInfo();
				} else {
					attributeInfo.mArrowFlat = 0;
					attributeInfo.mArrowMultiplier = 0;
				}
			}
		}

		// Retrieves attribute value in the trie
		//By default, does not include arrow attributes
		public double get(String key, Player player) {
			Node node = get(key);

			if (node != null) {
				AttributeInfo attributeInfo = node.mAttributeInfoMappings.get(player);
				if (attributeInfo != null) {
					//Include attributes of arrow when shot
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
			ChatColor.GRAY + "When in Main Hand:",
			ChatColor.GRAY + "When in Off Hand:",
			ChatColor.GRAY + "When on Head:",
			ChatColor.GRAY + "When on Body:",
			ChatColor.GRAY + "When on Legs:",
			ChatColor.GRAY + "When on Feet:"
	};

	private static final String ATTRIBUTE_ARROW = ChatColor.GRAY + "When Shot:";

	public AttributeTrie mAttributeTrie = new AttributeTrie();
	public List<BaseAttribute> mAttributes = new ArrayList<BaseAttribute>();

	public AttributeManager() {
		mAttributes.add(new AttributeProjectileDamage());
		mAttributes.add(new AttributeProjectileSpeed());
		mAttributes.add(new AttributeThrowRate());
		mAttributes.add(new AttributeThornsDamage());
		mAttributes.add(new AttributeAbilityPower());

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

					if (loreSegments.length == 2 && loreSegments[0].length() > 0) {
						boolean isMultiplier = false;
						if (loreSegments[0].endsWith("%")) {
							isMultiplier = true;
							loreSegments[0] = loreSegments[0].substring(0, loreSegments[0].length() - 1);
						}

						boolean foundDecimal = false;
						int j;
						for (j = 0; j < loreSegments[0].length(); j++) {
							char c = loreSegments[0].charAt(j);
							if (!Character.isDigit(c) && !(j == 0 && (c == '+' || c == '-'))) {
								if (c == '.' && loreSegments[0].length() >= 2) {
									if (foundDecimal) {
										break;
									} else {
										foundDecimal = true;
									}
								} else {
									break;
								}
							}
						}

						// Reached the end of iteration means very-likely to be parsable
						if (j == loreSegments[0].length()) {
							try {
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

	public void updateAttributeArrowTrie(Plugin plugin, Player player, ItemStack item) {
		boolean readAttributes = false;

		for (String loreEntry : item.getLore()) {
			if (!readAttributes) {
				if (ATTRIBUTE_ARROW.equals(loreEntry)) {
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

				if (loreSegments.length == 2 && loreSegments[0].length() > 0) {
					boolean isMultiplier = false;
					if (loreSegments[0].endsWith("%")) {
						isMultiplier = true;
						loreSegments[0] = loreSegments[0].substring(0, loreSegments[0].length() - 1);
					}

					boolean foundDecimal = false;
					int j;
					for (j = 0; j < loreSegments[0].length(); j++) {
						char c = loreSegments[0].charAt(j);
						if (!Character.isDigit(c) && !(j == 0 && (c == '+' || c == '-'))) {
							if (c == '.' && loreSegments[0].length() >= 2) {
								if (foundDecimal) {
									break;
								} else {
									foundDecimal = true;
								}
							} else {
								break;
							}
						}
					}

					// Reached the end of iteration means very-likely to be parsable
					if (j == loreSegments[0].length()) {
						try {
							double value = Double.parseDouble(loreSegments[0]);
							if (isMultiplier) {
								value /= 100;
							}

							plugin.mAttributeManager.mAttributeTrie.addArrow(loreSegments[1], player, value, isMultiplier);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
