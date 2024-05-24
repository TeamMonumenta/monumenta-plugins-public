package com.playmonumenta.plugins.guis;

import com.comphenix.protocol.ProtocolLibrary;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.custominventories.ClassSelectionCustomInventory;
import com.playmonumenta.plugins.depths.guis.DepthsSummaryGUI;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("NullAway")
public class AbilityTriggersGui extends Gui {

	private static final Component MAIN_PAGE_TITLE = Component.text("Ability Triggers");
	private static final int GUI_IDENTIFIER_LOC_L = 45;
	private static final int GUI_IDENTIFIER_LOC_R = 53;
	private @Nullable AbilityInfo<?> mSelectedAbility;
	private @Nullable AbilityTriggerInfo<?> mSelectedTrigger;
	private @Nullable AbilityTrigger mNewTrigger;
	private int mKeyOptionsStartIndex = 0;
	// true for class GUI, false for depths summary GUI
	private final boolean mPreviousGUI;

	public AbilityTriggersGui(Player player, boolean previousGUI) {
		super(player, 6 * 9, MAIN_PAGE_TITLE);
		mPreviousGUI = previousGUI;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setup() {
		ItemStack tempItem;
		String guiTag;
		if (mSelectedAbility == null) {
			// back icon
			if (mPreviousGUI) {
				tempItem = GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY, false,
					"Return to the class selection page.", NamedTextColor.GRAY, 40);
				GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_main_back");
				setItem(0, tempItem)
					.onLeftClick(() -> new ClassSelectionCustomInventory(mPlayer).openInventory(mPlayer, mPlugin));
			} else {
				tempItem = GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY, false,
					"Return to the ability summary page.", NamedTextColor.GRAY, 40);
				GUIUtils.setGuiNbtTag(tempItem, "texture", "depth_trigger_main_back");
				setItem(0, tempItem)
					.onLeftClick(() -> new DepthsSummaryGUI(mPlayer).openInventory(mPlayer, mPlugin));
			}

			// help icon
			tempItem = GUIUtils.createBasicItem(Material.OAK_SIGN, "Help", NamedTextColor.WHITE, false,
				"""
					Click on a trigger to change it.
					Triggers are shown in the order they are handled. Whenever a key is pressed, the top-left trigger is checked first if it matches. If not, the next trigger is checked, and so forth until a trigger matches and casts its ability.
					Eagle Eye is an exception: it allows other abilities to trigger after it.
					Right-click a trigger to immediately perform the trigger's action (e.g. toggle some state).""", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "depth_main_help");
			setItem(4, tempItem);

			// trigger icons
			int i = 0;
			for (Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilitiesInTriggerOrder()) {
				AbilityInfo<?> info = ability.getInfo();
				for (AbilityTriggerInfo<?> trigger : ability.getCustomTriggers()) {
					if (!trigger.meetsPrerequsite(mPlayer)) {
						continue;
					}
					List<Component> lore = new ArrayList<>();
					if (trigger.getDescription() != null) {
						lore.add(Component.text(trigger.getDescription(), NamedTextColor.GRAY));
					}
					lore.add(trigger.getTrigger().equals(info.getTrigger(trigger.getId()).getTrigger()) ?
						         Component.text("Current Trigger (default):", NamedTextColor.GRAY) :
						         Component.text("Custom Trigger:", NamedTextColor.AQUA));
					lore.addAll(trigger.getTriggerDescription());
					ItemStack item = GUIUtils.createBasicItem(info.getDisplayItem(), 1,
						info.getDisplayName() + " - " + trigger.getDisplayName(), NamedTextColor.GOLD, false,
						lore, true);

					setItem(2 + (i / 7), 1 + (i % 7), item)
						.onLeftClick(() -> {
							mSelectedAbility = info;
							mSelectedTrigger = trigger;
							mNewTrigger = new AbilityTrigger(trigger.getTrigger());
							update();
						})
						.onRightClick(() -> {
							if (trigger.getRestriction() == null || trigger.getRestriction().getPredicate().test(mPlayer)) {
								// cast is fine here, as the trigger is for the ability we got it from
								((AbilityTriggerInfo) trigger).getAction().test(ability);
							}
						});
					i++;
				}
			}

			// gui identifier - filler with tag for rp gui support (bottom left corner)
			if (mPreviousGUI) {
				setItem(GUI_IDENTIFIER_LOC_L, GUIUtils.createGuiIdentifierItem("gui_class_4_l"));
				setItem(GUI_IDENTIFIER_LOC_R, GUIUtils.createGuiIdentifierItem("gui_class_4_r"));
			} else {
				setItem(GUI_IDENTIFIER_LOC_L, GUIUtils.createGuiIdentifierItem("gui_depth_4_l"));
				setItem(GUI_IDENTIFIER_LOC_R, GUIUtils.createGuiIdentifierItem("gui_depth_4_r"));
			}


			// "revert all" button - top right to hopefully prevent accidental presses
			int numberOfCustomTriggers = mPlugin.mAbilityManager.getNumberOfCustomTriggers(mPlayer);
			if (numberOfCustomTriggers > 0) {
				tempItem = GUIUtils.createBasicItem(Material.BARRIER, 1, "Revert all triggers to defaults", NamedTextColor.DARK_RED, false,
					Component.text("This resets all triggers of all abilities of all classes back to defaults!\nYou currently have ", NamedTextColor.RED)
						.append(Component.text(numberOfCustomTriggers, NamedTextColor.GOLD))
						.append(Component.text(" custom trigger" + (numberOfCustomTriggers == 1 ? "" : "s") + " defined.", NamedTextColor.RED)), 40, true);
				GUIUtils.setGuiNbtTag(tempItem, "texture", "depth_main_reset_trigger");
				setItem(8, tempItem)
					.onLeftClick(() -> {
						mPlugin.mAbilityManager.clearCustomTriggers(mPlayer);
						for (Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilitiesInTriggerOrder()) {
							for (AbilityTriggerInfo<?> trigger : ability.getCustomTriggers()) {
								trigger.setTrigger(new AbilityTrigger(Objects.requireNonNull(ability.getInfo().getTrigger(trigger.getId())).getTrigger()));
							}
						}
						update();
					});
			}
		} else {
			// back icon
			tempItem = GUIUtils.createBasicItem(Material.ARROW, "Back",
				NamedTextColor.GRAY, false, "Return to the trigger selection page.", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_back");
			setItem(0, 0, tempItem)
					.onLeftClick(() -> {
						mSelectedAbility = null;
						mKeyOptionsStartIndex = 0;
						update();
					});

			Objects.requireNonNull(mNewTrigger);
			Objects.requireNonNull(mSelectedTrigger);

			List<Component> summary = new ArrayList<>();
			summary.add(Component.text("Trigger summary:", NamedTextColor.GRAY));
			summary.addAll(mNewTrigger.getDescription());
			if (mSelectedTrigger.getRestriction() != null && mNewTrigger.isEnabled()) {
				summary.add(Component.text("- unchangeable: ", NamedTextColor.RED)
					.append(Component.text(mSelectedTrigger.getRestriction().getDisplay(), NamedTextColor.WHITE)));
			}
			// summary icon
			setItem(0, 4, GUIUtils.createBasicItem(mSelectedAbility.getDisplayItem(), 1,
				mSelectedAbility.getDisplayName() + " - " + mSelectedTrigger.getDisplayName(), NamedTextColor.GOLD, false,
				summary, true));

			// options
			tempItem = GUIUtils.createBasicItem(Material.BARRIER, mNewTrigger.isEnabled() ? "Trigger enabled" : "Trigger disabled", mNewTrigger.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED, false,
				"Click to " + (mNewTrigger.isEnabled() ? "disable" : "enable") + " the trigger", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_" + (mNewTrigger.isEnabled() ? "enabled" : "disabled"));
			makeOptionIcons(1, 0, tempItem, mNewTrigger.isEnabled() ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, () -> {
				mNewTrigger.setEnabled(!mNewTrigger.isEnabled());
				update();
			});
			tempItem = GUIUtils.createBasicItem(Material.JIGSAW, "Key: " + mNewTrigger.getKey(), NamedTextColor.WHITE, false,
				"Click to cycle through main key.\nNote that this also changes the \"extras\" when changed.", NamedTextColor.GRAY, 40);

			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_" + (
				switch (mNewTrigger.getKey()) {
				case LEFT_CLICK -> "left";
				case RIGHT_CLICK -> "right";
				case SWAP -> "swap";
				case DROP -> "drop";
				default -> "";
			}));
			makeOptionIcons(1, 1, tempItem, switch (mNewTrigger.getKey()) {
				case LEFT_CLICK -> Material.IRON_SWORD;
				case RIGHT_CLICK -> Material.BOW;
				case SWAP -> Material.TORCH;
				case DROP -> Material.DROPPER;
			}, () -> {
				mNewTrigger.setKey(AbilityTrigger.Key.values()[(mNewTrigger.getKey().ordinal() + 1) % AbilityTrigger.Key.values().length]);
				mNewTrigger.getKeyOptions().clear();
				if (mSelectedTrigger.getRestriction() == null) {
					if (mNewTrigger.getKey() == AbilityTrigger.Key.LEFT_CLICK) {
						mNewTrigger.getKeyOptions().add(AbilityTrigger.KeyOptions.NO_PICKAXE);
					} else if (mNewTrigger.getKey() == AbilityTrigger.Key.RIGHT_CLICK) {
						mNewTrigger.getKeyOptions().addAll(List.of(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS));
					}
				}
				update();
			});
			tempItem = GUIUtils.createBasicItem(Material.SHEARS, "Double click: " + (mNewTrigger.isDoubleClick() ? "yes" : "no"), mNewTrigger.isDoubleClick() ? NamedTextColor.GREEN : NamedTextColor.GRAY, false,
				"Click to toggle requiring a double click", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_dclick_" + (mNewTrigger.isDoubleClick() ? "true" : "false"));
			makeOptionIcons(1, 3, tempItem, mNewTrigger.isDoubleClick() ? Material.GREEN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, () -> {
				mNewTrigger.setDoubleClick(!mNewTrigger.isDoubleClick());
				update();
			});
			makeBinaryOptionIcon(1, 4, Material.FEATHER, "sneaking", mNewTrigger.getSneaking(), mNewTrigger::setSneaking);
			makeBinaryOptionIcon(1, 5, Material.IRON_BOOTS, "sprinting", mNewTrigger.getSprinting(), mNewTrigger::setSprinting);
			makeBinaryOptionIcon(1, 6, Material.ROOTED_DIRT, "on ground", mNewTrigger.getOnGround(), mNewTrigger::setOnGround);

			String looking = "Looking " + (mNewTrigger.getLookDirections().size() == 3 ? "anywhere"
					: mNewTrigger.getLookDirections().stream().map(d -> d.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(" or ")));
			guiTag = "trigger_detail_look_" + (
				switch (looking) {
				case "Looking anywhere" -> "all";
				case "Looking up" -> "up";
				case "Looking level" -> "mid";
				case "Looking down" -> "down";
				case "Looking level or up" -> "up_mid";
				case "Looking down or up" -> "up_down";
				case "Looking down or level" -> "mid_down";
				default -> "unsupported";
			});
			tempItem = GUIUtils.createBasicItem(Material.HEART_OF_THE_SEA, looking, NamedTextColor.GRAY, false,
				"Click to cycle through look directions", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", guiTag);
			makeOptionIcons(1, 7, tempItem, mNewTrigger.getLookDirections().size() == 3 ? Material.GRAY_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, () -> {
				EnumSet<AbilityTrigger.LookDirection> lookDirections = mNewTrigger.getLookDirections();
				AbilityTrigger.LookDirection[] values = AbilityTrigger.LookDirection.values();
				if (lookDirections.size() == 3) {
					lookDirections.clear();
					lookDirections.add(values[0]);
				} else if (lookDirections.size() == 1) {
					AbilityTrigger.LookDirection direction = lookDirections.iterator().next();
					lookDirections.clear();
					if (direction.ordinal() + 1 < values.length) {
						lookDirections.add(values[direction.ordinal() + 1]);
					} else {
						lookDirections.add(values[0]);
						lookDirections.add(values[1]);
					}
				} else if (lookDirections.size() == 2) {
					AbilityTrigger.LookDirection missingDirection = Arrays.stream(values).filter(d -> !lookDirections.contains(d)).findFirst().orElse(values[0]);
					lookDirections.clear();
					lookDirections.addAll(List.of(values));
					if (missingDirection.ordinal() > 0) {
						lookDirections.remove(values[missingDirection.ordinal() - 1]);
					}
				}
				update();
			});

			tempItem = GUIUtils.createBasicItem(Material.POINTED_DRIPSTONE, "Allow fall-through: " + (mNewTrigger.isFallThrough() ? "yes" : "no"), mNewTrigger.isFallThrough() ? NamedTextColor.GREEN : NamedTextColor.GRAY, false,
				"Click to toggle whether another ability with an overlapping trigger will be triggered if this ability fails or is on cooldown.", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_fall_through_" + (mNewTrigger.isFallThrough() ? "true" : "false"));
			makeOptionIcons(1, 8, tempItem, mNewTrigger.isFallThrough() ? Material.GREEN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, () -> {
				mNewTrigger.setFallThrough(!mNewTrigger.isFallThrough());
				update();
			});

			// extras aka key options
			tempItem = GUIUtils.createBasicItem(Material.CHAIN_COMMAND_BLOCK, "Extras", NamedTextColor.WHITE, false,
				"Extra options for held items.\n"
					+ "When the main key is changed these are set to defaults, unless the trigger has an unchangeable item restriction.", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_extras");
			setItem(3, 0, tempItem);

			int colStart = 1;
			int colEnd = 8;
			mKeyOptionsStartIndex = Math.max(0, Math.min(AbilityTrigger.KeyOptions.values().length - 7, mKeyOptionsStartIndex));
			if (mKeyOptionsStartIndex == 1) {
				mKeyOptionsStartIndex = 0;
			} else if (mKeyOptionsStartIndex > 1) {
				colStart = 2;
				tempItem = GUIUtils.createBasicItem(Material.ARROW, "Scroll back for more options", NamedTextColor.GRAY);
				GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_scroll_back");
				setItem(3, 1, tempItem).onLeftClick(() -> {
					mKeyOptionsStartIndex -= 6;
					update();
				});
			}
			if (mKeyOptionsStartIndex + 7 < AbilityTrigger.KeyOptions.values().length) {
				colEnd = 7;
				tempItem = GUIUtils.createBasicItem(Material.ARROW, "Scroll forward for more options", NamedTextColor.GRAY);
				GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_scroll_forward");
				setItem(3, 8, tempItem).onLeftClick(() -> {
					mKeyOptionsStartIndex += mKeyOptionsStartIndex == 0 ? 7 : 6;
					update();
				});
			}

			for (int col = colStart; col <= colEnd; col++) {
				AbilityTrigger.KeyOptions keyOption = AbilityTrigger.KeyOptions.values()[mKeyOptionsStartIndex + col - colStart];
				boolean enabled = mNewTrigger.getKeyOptions().contains(keyOption);
				tempItem = GUIUtils.createBasicItem(keyOption.getMaterial(), capitalize(keyOption.getDisplay(enabled)), enabled ? NamedTextColor.RED : NamedTextColor.GRAY, false,
					"Click to toggle", NamedTextColor.GRAY, 40);
				GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_" + keyOption.getGuiTag(enabled));
				makeOptionIcons(3, col, tempItem, enabled ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, () -> {
					EnumSet<AbilityTrigger.KeyOptions> keyOptions = mNewTrigger.getKeyOptions();
					if (!keyOptions.remove(keyOption)) {
						keyOptions.add(keyOption);

						if (keyOption == AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON) {
							keyOptions.remove(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON);
						} else if (keyOption == AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON) {
							keyOptions.remove(AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON);
						}
					}
					update();
				});
			}

			// gui identifier - filler with tag for rp gui support (bottom left)
			setItem(GUI_IDENTIFIER_LOC_L, GUIUtils.createGuiIdentifierItem("gui_class_5_l"));
			setItem(GUI_IDENTIFIER_LOC_R, GUIUtils.createGuiIdentifierItem("gui_class_5_r"));

			// accept/cancel buttons
			if (!mNewTrigger.equals(mSelectedTrigger.getTrigger())) {
				{
					ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
					ItemMeta meta = confirm.getItemMeta();
					meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
					meta.lore(List.of(Component.text("Accept trigger changes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
					confirm.setItemMeta(meta);
					ItemUtils.setPlainTag(confirm);
					GUIUtils.setGuiNbtTag(confirm, "texture", "trigger_detail_confirm");
					setItem(5, 2, confirm).onLeftClick(() -> {
						mSelectedTrigger.setTrigger(mNewTrigger);
						mPlugin.mAbilityManager.setCustomTrigger(mPlayer, mSelectedAbility, mSelectedTrigger.getId(), mNewTrigger);
						ProtocolLibrary.getProtocolManager().updateEntity(mPlayer, ProtocolLibrary.getProtocolManager().getEntityTrackers(mPlayer));
						mSelectedAbility = null;
						mKeyOptionsStartIndex = 0;
						update();
					});
				}
				{
					ItemStack cancel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
					ItemMeta meta = cancel.getItemMeta();
					meta.displayName(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
					meta.lore(List.of(Component.text("Discard current trigger changes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
					cancel.setItemMeta(meta);
					ItemUtils.setPlainTag(cancel);
					GUIUtils.setGuiNbtTag(cancel, "texture", "trigger_detail_cancel");
					setItem(5, 6, cancel).onLeftClick(() -> {
						mNewTrigger = new AbilityTrigger(mSelectedTrigger.getTrigger());
						mSelectedAbility = null;
						mKeyOptionsStartIndex = 0;
						update();
					});
				}
			}

			// revert button
			tempItem = GUIUtils.createBasicItem(Material.BARRIER, "Revert to default", NamedTextColor.DARK_RED, false,
				"Revert any custom trigger changes", NamedTextColor.GRAY, 40);
			GUIUtils.setGuiNbtTag(tempItem, "texture", "trigger_detail_revert");
			if (!mSelectedTrigger.getTrigger().equals(mSelectedAbility.getTrigger(mSelectedTrigger.getId()).getTrigger())) {
				setItem(5, 4, tempItem).onLeftClick(() -> {
					mNewTrigger = new AbilityTrigger(mSelectedAbility.getTrigger(mSelectedTrigger.getId()).getTrigger());
					mSelectedTrigger.setTrigger(mNewTrigger);
					mPlugin.mAbilityManager.setCustomTrigger(mPlayer, mSelectedAbility, mSelectedTrigger.getId(), null);
					mSelectedAbility = null;
					mKeyOptionsStartIndex = 0;
					update();
				});
			}
		}
	}


	private void makeOptionIcons(int row, int column, ItemStack display, AbilityTrigger.BinaryOption value, Runnable onClick) {
		Material indicatorMaterial = value == AbilityTrigger.BinaryOption.TRUE ? Material.GREEN_STAINED_GLASS_PANE
				: value == AbilityTrigger.BinaryOption.FALSE ? Material.RED_STAINED_GLASS_PANE
				: Material.GRAY_STAINED_GLASS_PANE;
		makeOptionIcons(row, column, display, indicatorMaterial, onClick);
	}

	private void makeOptionIcons(int row, int column, ItemStack display, Material indicatorMaterial, Runnable onClick) {
		setItem(row, column, display).onLeftClick(onClick);
		ItemStack indicator = display.clone();
		indicator.setType(indicatorMaterial);
		GUIUtils.setGuiNbtTag(indicator, "texture", "trigger_detail_empty");
		setItem(row + 1, column, indicator).onLeftClick(onClick);
	}

	private void makeBinaryOptionIcon(int row, int column, Material material, String name, AbilityTrigger.BinaryOption value, Consumer<AbilityTrigger.BinaryOption> setter) {
		String displayName = value == AbilityTrigger.BinaryOption.TRUE ? "Must be " + name
				: value == AbilityTrigger.BinaryOption.FALSE ? "Not " + name
				: capitalize(name) + " or not " + name;
		NamedTextColor color = value == AbilityTrigger.BinaryOption.TRUE ? NamedTextColor.GREEN
				: value == AbilityTrigger.BinaryOption.FALSE ? NamedTextColor.RED
				: NamedTextColor.GRAY;

		String guiTag = switch (name) {
			case "sneaking" -> "sneak";
			case "sprinting" -> "sprint";
			case "on ground" -> "ground";
			default -> "unsupported";
		};
		guiTag = "trigger_detail_" + guiTag + ((value == AbilityTrigger.BinaryOption.TRUE) ? "_true"
			: (value == AbilityTrigger.BinaryOption.FALSE) ? "_false"
			: "_both");
		ItemStack tempItem = GUIUtils.createBasicItem(material, displayName, color, false,
			"Click to cycle through options", NamedTextColor.GRAY, 40);
		GUIUtils.setGuiNbtTag(tempItem, "texture", guiTag);

		makeOptionIcons(row, column, tempItem, value, () -> {
			setter.accept(AbilityTrigger.BinaryOption.values()[(value.ordinal() + 1) % AbilityTrigger.BinaryOption.values().length]);
			update();
		});
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

}
