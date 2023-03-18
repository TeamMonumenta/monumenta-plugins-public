package com.playmonumenta.plugins.guis;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.managers.LoadoutManager;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class LoadoutManagerGui extends Gui {

	private static final int LOADOUTS_START = 9;
	private static final int MAX_LOADOUTS = 5 * 9;
	private final Player mTarget;
	LoadoutManager.LoadoutData mLoadoutData;

	private @Nullable LoadoutManager.Loadout mRearrangingLoadout;
	private @Nullable LoadoutManager.Loadout mSelectedLoadout;
	private @Nullable LoadoutManager.Loadout mDeletedLoadout;

	public LoadoutManagerGui(Player player) {
		this(player, player);
	}

	public LoadoutManagerGui(Player sender, Player target) {
		super(sender, 6 * 9, Component.text(LoadoutManager.LOADOUT_MANAGER_NAME, NamedTextColor.WHITE, TextDecoration.BOLD));
		mTarget = target;
		mLoadoutData = Plugin.getInstance().mLoadoutManager.getData(target);
		setFiller(Material.GRAY_STAINED_GLASS_PANE);
	}

	@Override
	protected void setup() {
		LoadoutManager.Loadout selectedLoadout = mSelectedLoadout;
		if (selectedLoadout == null) {

			// info icon
			setItem(4, GUIUtils.createBasicItem(Material.DARK_OAK_SIGN, 1,
				Component.text(LoadoutManager.LOADOUT_MANAGER_NAME + " Info", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
				List.of(
					Component.text("Manage your loadouts here.", NamedTextColor.GRAY),
					Component.text("- Click on an empty spot below", NamedTextColor.GRAY),
					Component.text("to create a new loadout.", NamedTextColor.GRAY),
					Component.text("- ", NamedTextColor.GRAY).append(Component.text("Left-click", NamedTextColor.WHITE)).append(Component.text(" a loadout to edit it.", NamedTextColor.GRAY)),
					Component.text("- ", NamedTextColor.GRAY).append(Component.text("Right-click", NamedTextColor.WHITE)).append(Component.text(" a loadout to swap to it.", NamedTextColor.GRAY)),
					Component.text("- ", NamedTextColor.GRAY).append(Component.text("Shift + Right-click", NamedTextColor.WHITE)).append(Component.text(" to only swap equipment and vanity.", NamedTextColor.GRAY)),
					Component.text("- To rearrange loadouts, press ", NamedTextColor.GRAY)
						.append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE)),
					Component.text("on a loadout, then click on any", NamedTextColor.GRAY),
					Component.text("other tile to move it there.", NamedTextColor.GRAY),
					Component.empty(),
					Component.text("Equipping a loadout takes items from your inventory,", NamedTextColor.GRAY),
					Component.text("including items in " + LoadoutManager.STORAGE_SHULKER_NAME + "s,", NamedTextColor.GRAY),
					Component.text("and swaps them with your equipped items.", NamedTextColor.GRAY),
					Component.text("If you have a Remnant of the Rose or are near", NamedTextColor.GRAY),
					Component.text("an Ender Chest, it also takes items from your", NamedTextColor.GRAY),
					Component.text("Ender Chest or " + LoadoutManager.STORAGE_SHULKER_NAME + "s therein.", NamedTextColor.GRAY)
				), true));

			// loadout slots icon: show number purchased, and click to purchase another slot
			setItem(6, GUIUtils.createBasicItem(Material.ARMOR_STAND, "Available Loadout Slots: " + (mLoadoutData.mMaxLoadouts - mLoadoutData.mLoadouts.size()) + "/" + mLoadoutData.mMaxLoadouts,
				NamedTextColor.GOLD, true, """
					Click to buy an additional slot for the cost of:
					- 1 Loadout Lockbox
					- 1 Tesseract of the Elements
					- and 1 C.H.A.R.M. 2000
					Note that only empty, uninfused items in your inventory are taken as payment."""))
				.onLeftClick(() -> {
					if (mLoadoutData.mMaxLoadouts >= MAX_LOADOUTS) {
						mPlayer.sendMessage(Component.text("You have already reached the maximum number of loadout slots!", NamedTextColor.RED));
						mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
						return;
					}
					ItemStack cost1 = Arrays.stream(mPlayer.getInventory().getStorageContents()).filter(
						item -> item != null
							        && ItemUtils.isShulkerBox(item.getType())
							        && "Loadout Lockbox".equals(ItemUtils.getPlainNameIfExists(item))
							        && isUnmodifiedShulker(item)
					).findFirst().orElse(null);
					if (cost1 == null) {
						mPlayer.sendMessage(Component.text("You need an empty, unmodified Loadout Lockbox to buy a loadout slot!", NamedTextColor.RED));
						mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
						return;
					}
					ItemStack cost2 = Arrays.stream(mPlayer.getInventory().getStorageContents()).filter(
						item -> item != null
							        && ItemUtils.isShulkerBox(item.getType())
							        && "C.H.A.R.M. 2000".equals(ItemUtils.getPlainNameIfExists(item))
							        && isUnmodifiedShulker(item)
					).findFirst().orElse(null);
					if (cost2 == null) {
						mPlayer.sendMessage(Component.text("You need an empty, unmodified C.H.A.R.M. 2000 to buy a loadout slot!", NamedTextColor.RED));
						mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
						return;
					}
					ItemStack cost3 = Arrays.stream(mPlayer.getInventory().getStorageContents()).filter(
						item -> item != null
							        && item.getType() == Material.YELLOW_STAINED_GLASS
							        && "Tesseract of the Elements".equals(ItemUtils.getPlainNameIfExists(item))
							        && ItemStatUtils.getPlayerModified(new NBTItem(item)) == null
					).findFirst().orElse(null);
					if (cost3 == null) {
						mPlayer.sendMessage(Component.text("You need an empty, unmodified Tesseract of the Elements to buy a loadout slot!", NamedTextColor.RED));
						mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
						return;
					}
					cost1.subtract();
					cost2.subtract();
					cost3.subtract();
					mLoadoutData.mMaxLoadouts++;
					mPlayer.sendMessage(Component.text("Successfully bought a loadout slot! Your new loadout limit is ", NamedTextColor.AQUA)
						                    .append(Component.text(mLoadoutData.mMaxLoadouts, NamedTextColor.GOLD, TextDecoration.BOLD))
						                    .append(Component.text(" (out of a possible maximum of " + MAX_LOADOUTS + ").", NamedTextColor.AQUA)));
					mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1, 2);
					update();
				});

			// stash equipment button
			setItem(0, 7, GUIUtils.createBasicItem(Material.CHEST, "Stash Equipment", NamedTextColor.WHITE, true,
				"Click to store your current equipment and charms in any free " + LoadoutManager.STORAGE_SHULKER_NAME + "s.\n" +
					"Use for example to get a clean slate to start a new loadout.\n" +
					"Shift click to only store equipment and not charms."))
				.onClick(event -> {
					if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
						LoadoutManager.Loadout loadout = new LoadoutManager.Loadout(-1, "temp");
						loadout.mIncludeClass = false;
						loadout.mIncludeVanity = false;
						loadout.mIncludeCharms = event.getClick() == ClickType.LEFT;
						loadout.mClearEmpty = true;
						Plugin.getInstance().mLoadoutManager.swapTo(mPlayer, loadout, true);
						update();
					}
				});

			// undo deletion icon
			if (mDeletedLoadout != null) {
				setItem(0, 8, GUIUtils.createBasicItem(Material.BARRIER, "Undo Deletion", NamedTextColor.YELLOW, true,
					"Restores the most recently deleted loadout. Can only be done as long as this screen is still open and you don't create a new loadout."))
					.onLeftClick(() -> {
						if (mDeletedLoadout != null) {
							mLoadoutData.mLoadouts.add(mDeletedLoadout);
							mDeletedLoadout = null;
							update();
						}
					});
			}

			// loadout list
			index_loop:
			for (int i = 0; i < MAX_LOADOUTS; i++) {
				for (LoadoutManager.Loadout loadout : mLoadoutData.mLoadouts) {
					if (loadout.mIndex == i) {
						ItemStack icon = new ItemStack(loadout.mDisplayItem.mType);
						ItemUtils.modifyMeta(icon, meta -> {
							meta.displayName(Component.text(loadout.mName, NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
							List<Component> lore = new ArrayList<>();
							if (loadout.mIsQuickSwap) {
								lore.add(Component.text("Quick-Swap Loadout", NamedTextColor.GOLD, TextDecoration.ITALIC));
							}
							if (loadout.mIncludeClass) {
								PlayerClass playerClass = new MonumentaClasses().getClassById(loadout.mClass.mClassId);
								lore.add(Component.text(playerClass == null ? "Classless" : playerClass.mClassName + " (" + AbilityUtils.getSpec(loadout.mClass.mSpecId) + ")",
										playerClass == null ? NamedTextColor.YELLOW : playerClass.mClassColor, TextDecoration.BOLD)
									         .decoration(TextDecoration.ITALIC, false));
							} else {
								lore.add(Component.text("No class", NamedTextColor.GRAY, TextDecoration.ITALIC));
							}
							if (loadout.mIncludeEquipment && !loadout.mEquipment.isEmpty()) {
								lore.add(Component.text("Has equipment", NamedTextColor.WHITE)
									         .decoration(TextDecoration.ITALIC, false));
							} else {
								lore.add(Component.text("No equipment", NamedTextColor.GRAY, TextDecoration.ITALIC));
							}
							if (loadout.mIncludeVanity) {
								lore.add(Component.text("Has vanity", NamedTextColor.WHITE)
									         .decoration(TextDecoration.ITALIC, false));
							} else {
								lore.add(Component.text("No vanity", NamedTextColor.GRAY, TextDecoration.ITALIC));
							}
							if (loadout.mIncludeCharms) {
								lore.add(Component.text("Has charms", NamedTextColor.WHITE)
									         .decoration(TextDecoration.ITALIC, false));
							} else {
								lore.add(Component.text("No charms", NamedTextColor.GRAY, TextDecoration.ITALIC));
							}
							meta.lore(lore);
							meta.addItemFlags(ItemFlag.values());
						});
						ItemUtils.setPlainName(icon, loadout.mDisplayItem.mName);
						setItem(LOADOUTS_START + i, new GuiItem(icon, false))
							.onLeftClick(() -> {
								mSelectedLoadout = loadout;
								update();
							})
							.onRightClick(() -> {
								if (mTarget != mPlayer) {
									return;
								}
								close();
								Plugin.getInstance().mLoadoutManager.swapTo(mTarget, loadout, true);
							})
							.onClick(event -> {
								if (event.getClick() == ClickType.SWAP_OFFHAND) {
									mRearrangingLoadout = loadout;
								} else if (event.getClick() == ClickType.SHIFT_RIGHT) {
									if (mTarget != mPlayer) {
										return;
									}
									close();
									Plugin.getInstance().mLoadoutManager.swapTo(mTarget, loadout, false);
								}
							});
						continue index_loop;
					}
				}
				int finalI = i;
				setItem(LOADOUTS_START + i, GUIUtils.createBasicItem(Material.BLACK_STAINED_GLASS_PANE, "", NamedTextColor.BLACK, ""))
					.onLeftClick(() -> {
						if (mLoadoutData.mLoadouts.size() >= mLoadoutData.mMaxLoadouts) {
							mPlayer.sendMessage(Component.text("You have reached your maximum number of loadouts! Purchase additional slots to add more loadouts.", NamedTextColor.RED));
							mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
							return;
						}
						close();
						SignUtils.newMenu(List.of("", "", "~~~~~~~~~~~", "Loadout Name"))
							.reopenIfFail(false)
							.response((player, lines) -> {
								String name = (lines[0].trim() + " " + lines[1].trim()).trim();
								if (name.isEmpty()) {
									open();
									return false;
								}
								LoadoutManager.Loadout loadout = new LoadoutManager.Loadout(finalI, name);
								loadout.setFromPlayer(mTarget);
								loadout.mDisplayItem = ItemUtils.getIdentifier(player.getInventory().getItem(0), false);
								if (loadout.mDisplayItem.mType == null || loadout.mDisplayItem.mType.isAir()) {
									loadout.mDisplayItem = new ItemUtils.ItemIdentifier(Material.ARMOR_STAND, null);
								}
								loadout.mIncludeVanity = Plugin.getInstance().mVanityManager.getData(player).mLockboxSwapEnabled;
								mLoadoutData.mLoadouts.add(loadout);
								mSelectedLoadout = loadout;
								mDeletedLoadout = null;
								open();
								return true;
							})
							.open(mPlayer);
					});
			}
		} else {
			// ---------- top row: meta ----------
			// back button
			setItem(0, 0, GUIUtils.createBasicItem(Material.ARROW, "Back to Overview", NamedTextColor.WHITE, true, "Go back to the main page"))
				.onLeftClick(() -> {
					mSelectedLoadout = null;
					update();
				});

			// info icon; is also button to change name
			ItemStack loadoutIcon = GUIUtils.createBasicItem(selectedLoadout.mDisplayItem.mType, selectedLoadout.mName, NamedTextColor.GOLD, true,
				"Click to change the name of this loadout.\n" +
					"Shift left click an item in your inventory to use it as display icon for this loadout.");
			ItemUtils.setPlainName(loadoutIcon, selectedLoadout.mDisplayItem.mName);
			setItem(0, 4, new GuiItem(loadoutIcon, false))
				.onLeftClick(() -> {
					close();
					String name = selectedLoadout.mName;
					int spaceIndex1 = name.indexOf(' ', name.length() / 2);
					int spaceIndex2 = name.lastIndexOf(' ', name.length() / 2);
					int spaceIndex = spaceIndex2 < 0 || (spaceIndex1 >= 0 && Math.abs(spaceIndex1 - name.length() / 2) < Math.abs(spaceIndex2 - name.length() / 2)) ? spaceIndex1 : spaceIndex2;
					if (spaceIndex < 0) {
						spaceIndex = name.length();
					}
					SignUtils.newMenu(List.of(name.substring(0, spaceIndex).trim(), name.substring(spaceIndex).trim(), "~~~~~~~~~~~", "Loadout Name"))
						.response((player, lines) -> {
							String newName = (lines[0].trim() + " " + lines[1].trim()).trim();
							if (!newName.isEmpty()) {
								selectedLoadout.mName = newName;
							}
							open();
							return true;
						})
						.open(mPlayer);
				});

			// set as quickswap loadout button
			setItem(0, 6, GUIUtils.createBasicItem(selectedLoadout.mIsQuickSwap ? Material.GOLDEN_SWORD : Material.STONE_SWORD, 1,
				Component.text(selectedLoadout.mIsQuickSwap ? "Is Quick-Swap Loadout" : "Set to Quick-Swap Loadout",
					selectedLoadout.mIsQuickSwap ? NamedTextColor.GOLD : NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
				List.of(
					Component.text("The Quick-Swap Loadout can", NamedTextColor.GRAY),
					Component.text("be quickly switched to by", NamedTextColor.GRAY),
					Component.text("pressing ", NamedTextColor.GRAY).append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE)).append(Component.text(" on the", NamedTextColor.GRAY)),
					Component.text(LoadoutManager.LOADOUT_MANAGER_NAME + " item in an inventory.", NamedTextColor.GRAY),
					Component.text("If this loadout is already equipped,", NamedTextColor.GRAY),
					Component.text("using the same button will instead", NamedTextColor.GRAY),
					Component.text("restore the previous loadout.", NamedTextColor.GRAY)
				), true))
				.onLeftClick(() -> {
					if (!selectedLoadout.mIsQuickSwap) {
						for (LoadoutManager.Loadout loadout : mLoadoutData.mLoadouts) {
							loadout.mIsQuickSwap = false;
						}
						selectedLoadout.mIsQuickSwap = true;
					} else {
						selectedLoadout.mIsQuickSwap = false;
					}
					update();
				});

			// delete loadout button
			setItem(0, 8, GUIUtils.createBasicItem(Material.BARRIER, "Delete Loadout", NamedTextColor.RED, true,
				"Deletes this loadout."))
				.onLeftClick(() -> {
					mLoadoutData.mLoadouts.remove(selectedLoadout);
					mDeletedLoadout = selectedLoadout;
					mSelectedLoadout = null;
					update();
				});


			// ---------- second and third rows: equipment ----------
			// button to include/exclude equipment
			// button to set to current
			// show equipment
			setItem(1, 0, GUIUtils.createBasicItem(selectedLoadout.mIncludeEquipment ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, 1,
				Component.text("Equipment " + (selectedLoadout.mIncludeEquipment ? "Included" : "Excluded"),
					selectedLoadout.mIncludeEquipment ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
				List.of(
					Component.text("Left-click to toggle whether", NamedTextColor.GRAY),
					Component.text("this loadout swaps your equipment.", NamedTextColor.GRAY),
					Component.text("Right-click to toggle whether", NamedTextColor.GRAY),
					Component.text("empty slots in the loadout are", NamedTextColor.GRAY),
					Component.text("kept as-is or cleared on swapping.", NamedTextColor.GRAY),
					Component.text("Current mode: ", NamedTextColor.GRAY).append(Component.text(selectedLoadout.mClearEmpty ? "clear empty slots" : "keep items in empty slots", NamedTextColor.WHITE))
				), true))
				.onLeftClick(() -> {
					selectedLoadout.mIncludeEquipment = !selectedLoadout.mIncludeEquipment;
					update();
				})
				.onRightClick(() -> {
					selectedLoadout.mClearEmpty = !selectedLoadout.mClearEmpty;
					update();
				});
			if (selectedLoadout.mIncludeEquipment) {
				setItem(1, 1, getPlayerHead(GUIUtils.createBasicItem(Material.PLAYER_HEAD, "Set to current", NamedTextColor.WHITE, true,
					"Click to set this loadout's stored equipment to your current equipment."), mTarget))
					.onLeftClick(() -> {
						selectedLoadout.setEquipmentFromPlayer(mTarget);
						update();
					});

				for (LoadoutManager.LoadoutItem loadoutItem : selectedLoadout.mEquipment) {
					int targetSlot = loadoutItem.mSlot < 9 ? 18 + loadoutItem.mSlot : loadoutItem.mSlot == 40 ? 15 : 11 + 39 - loadoutItem.mSlot;
					ItemStack icon = new ItemStack(loadoutItem.mIdentifier.mType);
					ItemUtils.modifyMeta(icon, meta -> {
						meta.displayName((loadoutItem.mIdentifier.mName != null ? Component.text(loadoutItem.mIdentifier.mName, NamedTextColor.WHITE, TextDecoration.BOLD)
							                  : Component.translatable(loadoutItem.mIdentifier.mType.translationKey(), NamedTextColor.WHITE, TextDecoration.BOLD)).decoration(TextDecoration.ITALIC, false));
						List<Component> lore = new ArrayList<>();
						lore.add(Component.text("Preferred Infusion: ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
							         .append(loadoutItem.mInfusionType != null ? Component.text(loadoutItem.mInfusionType.getName(), InfusionUtils.InfusionSelection.getByType(loadoutItem.mInfusionType).getColor()) : Component.text("any")));
						if (loadoutItem.mInfusionType != null) {
							lore.add(Component.text("(right click to clear)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
						}
						meta.lore(lore);
						meta.addItemFlags(ItemFlag.values());
					});
					setItem(targetSlot, icon)
						.onRightClick(() -> {
							loadoutItem.mInfusionType = null;
							update();
						});
				}

			}


			// ---------- forth row: vanity ----------
			// button to include/exclude vanity
			// button to set to current
			// show vanity
			setItem(3, 0, GUIUtils.createBasicItem(selectedLoadout.mIncludeVanity ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
				"Vanity " + (selectedLoadout.mIncludeVanity ? "Included" : "Excluded"),
				selectedLoadout.mIncludeVanity ? NamedTextColor.GREEN : NamedTextColor.RED, true,
				"Click to toggle whether this loadout swaps your vanity and parrots."))
				.onLeftClick(() -> {
					selectedLoadout.mIncludeVanity = !selectedLoadout.mIncludeVanity;
					update();
				});
			if (selectedLoadout.mIncludeVanity) {
				setItem(3, 1, getPlayerHead(GUIUtils.createBasicItem(Material.PLAYER_HEAD, "Set to current", NamedTextColor.WHITE, true,
					"Click to set this loadout's stored vanity and parrots to your current setup."), mTarget))
					.onLeftClick(() -> {
						selectedLoadout.setVanityFromPlayer(mTarget);
						update();
					});

				for (Map.Entry<EquipmentSlot, ItemStack> entry : selectedLoadout.mVanity.entrySet()) {
					ItemStack icon = ItemUtils.clone(entry.getValue());
					ItemUtils.modifyMeta(icon, meta -> {
						String slot = switch (entry.getKey()) {
							case HEAD -> "Head";
							case CHEST -> "Chest";
							case LEGS -> "Legs";
							case FEET -> "Feet";
							default -> "Offhand";
						};
						meta.lore(List.of(Component.text(slot + " Vanity", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
						if (meta.displayName() != null) {
							meta.displayName(Component.text(MessagingUtils.plainText(meta.displayName()), NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
						}
					});
					setItem(3, 7 - entry.getKey().ordinal(), icon);
				}

				ParrotManager.ParrotVariant leftParrot = ParrotManager.ParrotVariant.getVariantByNumber(selectedLoadout.mLeftParrot);
				if (leftParrot != null) {
					setItem(3, 7, GUIUtils.createBasicItem(leftParrot.getDisplayitem(), leftParrot.getName(), NamedTextColor.WHITE, true, "Left shoulder parrot"));
				}
				ParrotManager.ParrotVariant rightParrot = ParrotManager.ParrotVariant.getVariantByNumber(selectedLoadout.mRightParrot);
				if (rightParrot != null) {
					setItem(3, 8, GUIUtils.createBasicItem(rightParrot.getDisplayitem(), rightParrot.getName(), NamedTextColor.WHITE, true, "Right shoulder parrot"));
				}
			}


			// ---------- fifth row: charms ----------
			// button to include/exclude charms
			// button to set to current
			// show charms
			setItem(4, 0, GUIUtils.createBasicItem(selectedLoadout.mIncludeCharms ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
				"Charms " + (selectedLoadout.mIncludeCharms ? "Included" : "Excluded"),
				selectedLoadout.mIncludeCharms ? NamedTextColor.GREEN : NamedTextColor.RED, true,
				"Click to toggle whether this loadout swaps your charms."))
				.onLeftClick(() -> {
					selectedLoadout.mIncludeCharms = !selectedLoadout.mIncludeCharms;
					update();
				});
			if (selectedLoadout.mIncludeCharms) {
				setItem(4, 1, getPlayerHead(GUIUtils.createBasicItem(Material.PLAYER_HEAD, "Set to current", NamedTextColor.WHITE, true,
					"Click to set this loadout's stored charms to your current setup."), mTarget))
					.onLeftClick(() -> {
						selectedLoadout.setCharmsFromPlayer(mTarget);
						update();
					});

				int i = 0;
				for (ItemUtils.ItemIdentifier charm : selectedLoadout.mCharms) {
					setItem(4, 2 + i, GUIUtils.createBasicItem(charm.mType, 1, charm.getDisplayName().color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD), List.of(), true));
					i++;
				}
			}

			// ---------- last row: class ----------
			// button to include/exclude class
			// button to set to current
			// show class
			setItem(5, 0, GUIUtils.createBasicItem(selectedLoadout.mIncludeClass ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
				"Class " + (selectedLoadout.mIncludeClass ? "Included" : "Excluded"),
				selectedLoadout.mIncludeClass ? NamedTextColor.GREEN : NamedTextColor.RED, true,
				"Click to toggle whether this loadout swaps your class and abilities."))
				.onLeftClick(() -> {
					selectedLoadout.mIncludeClass = !selectedLoadout.mIncludeClass;
					update();
				});
			if (selectedLoadout.mIncludeClass) {
				setItem(5, 1, getPlayerHead(GUIUtils.createBasicItem(Material.PLAYER_HEAD, "Set to current", NamedTextColor.WHITE, true,
					"Click to set this loadout's stored class and abilities to your current class setup."), mTarget))
					.onLeftClick(() -> {
						selectedLoadout.setClassFromPlayer(mTarget);
						update();
					});

				PlayerClass playerClass = new MonumentaClasses().getClassById(selectedLoadout.mClass.mClassId);
				PlayerSpec playerSpec = playerClass == null ? null : playerClass.getSpecById(selectedLoadout.mClass.mSpecId);
				StringBuilder lore = new StringBuilder();
				if (playerClass != null) {
					(playerSpec != null ? Stream.concat(playerClass.mAbilities.stream(), playerSpec.mAbilities.stream()) : playerClass.mAbilities.stream()).forEach(abilityInfo -> {
						Integer score = selectedLoadout.mClass.mAbilityScores.get(abilityInfo.getScoreboard());
						if (score != null && score > 0) {
							lore.append("\n").append(abilityInfo.getDisplayName()).append(": ").append(score > 2 ? score - 2 + "*" : score);
						}
					});
				}
				setItem(5, 2, GUIUtils.createBasicItem(playerClass == null ? Material.WOODEN_SWORD : playerClass.mDisplayItem.getType(),
					(playerClass == null ? "No Class" : playerClass.mClassName + " (" + (playerSpec == null ? "No Specialization" : playerSpec.mSpecName) + ")"),
					playerClass == null ? NamedTextColor.WHITE : playerClass.mClassColor, true,
					lore.toString().trim()));
			}

		}
	}

	@Override
	protected boolean onGuiClick(InventoryClickEvent event) {
		if (mRearrangingLoadout != null) {
			int index = event.getSlot() - LOADOUTS_START;
			if (0 <= index && index < MAX_LOADOUTS) {
				if (index == mRearrangingLoadout.mIndex) {
					mRearrangingLoadout = null;
					return false;
				}
				for (LoadoutManager.Loadout loadout : mLoadoutData.mLoadouts) {
					if (loadout.mIndex == index) {
						loadout.mIndex = mRearrangingLoadout.mIndex;
						break;
					}
				}
				mRearrangingLoadout.mIndex = index;
				mRearrangingLoadout = null;
				update();
			}
			return false;
		}
		return true;
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		ItemStack currentItem = event.getCurrentItem();
		if (mSelectedLoadout != null && event.getClick() == ClickType.SHIFT_LEFT && currentItem != null && !currentItem.getType().isAir()) {
			mSelectedLoadout.mDisplayItem = ItemUtils.getIdentifier(currentItem, false);
			update();
		}
	}

	private static ItemStack getPlayerHead(ItemStack item, Player player) {
		if (item.getItemMeta() instanceof SkullMeta skullMeta) {
			skullMeta.setOwningPlayer(player);
			item.setItemMeta(skullMeta);
		}
		return item;
	}

	private static final ImmutableSet<String> ALLOWED_PLAYER_MODIFIED_KEYS = ImmutableSet.of(
		ItemStatUtils.CUSTOM_SKIN_KEY, ItemStatUtils.PLAYER_CUSTOM_NAME_KEY, ItemStatUtils.VANITY_ITEMS_KEY);

	private static boolean isUnmodifiedShulker(ItemStack item) {
		// Must be empty
		if (item.getItemMeta() instanceof BlockStateMeta meta
			    && meta.getBlockState() instanceof ShulkerBox shulkerBox
			    && !Arrays.stream(shulkerBox.getInventory().getContents()).allMatch(ItemUtils::isNullOrAir)) {
			return false;
		}
		// Must not have any player modifications except for name, skin, or stored vanity
		NBTCompound playerModified = ItemStatUtils.getPlayerModified(new NBTItem(item));
		if (playerModified == null) {
			return true;
		}
		return ALLOWED_PLAYER_MODIFIED_KEYS.containsAll(playerModified.getKeys());
	}

}
