package com.playmonumenta.plugins.guis;

import com.comphenix.protocol.ProtocolLibrary;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.custominventories.ClassSelectionCustomInventory;
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


	private @Nullable AbilityInfo<?> mSelectedAbility;
	private @Nullable AbilityTriggerInfo<?> mSelectedTrigger;
	private @Nullable AbilityTrigger mNewTrigger;

	public AbilityTriggersGui(Player player) {
		super(player, 6 * 9, MAIN_PAGE_TITLE);
	}

	@Override
	protected void setup() {
		if (mSelectedAbility == null) {
			// back icon
			setItem(0, GUIUtils.createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY, false,
					"Return to the class selection page.", NamedTextColor.GRAY, 40))
					.onLeftClick(() -> new ClassSelectionCustomInventory(mPlayer).openInventory(mPlayer, mPlugin));

			// help icon
			setItem(4, GUIUtils.createBasicItem(Material.OAK_SIGN, "Help", NamedTextColor.WHITE, false,
					"Click on a trigger to change it.\n" +
							"Triggers are shown in the order they are handled. Whenever a key is pressed, the top-left trigger is checked first if it matches. " +
							"If not, the next trigger is checked, and so forth until a trigger matches and casts its ability.\n" +
							"Eagle Eye is an exception: it allows other abilities to trigger after it.", NamedTextColor.GRAY, 40));

			// trigger icons
			int i = 0;
			for (Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilitiesInTriggerOrder()) {
				AbilityInfo<?> info = ability.getInfo();
				for (AbilityTriggerInfo<?> trigger : ability.getCustomTriggers()) {
					List<Component> lore = new ArrayList<>();
					lore.add(trigger.getTrigger().equals(info.getTrigger(trigger.getId()).getTrigger()) ?
						Component.text("Current Trigger (default):", NamedTextColor.GRAY) :
						Component.text("Custom Trigger:", NamedTextColor.AQUA));
					lore.addAll(trigger.getDescription());
					ItemStack item = GUIUtils.createBasicItem(info.getDisplayItem().getType(), 1,
						info.getDisplayName() + " - " + trigger.getDisplayName(), NamedTextColor.GOLD, false,
						lore, true);

					setItem(2 + (i / 7), 1 + (i % 7), item)
							.onLeftClick(() -> {
								mSelectedAbility = info;
								mSelectedTrigger = trigger;
								mNewTrigger = new AbilityTrigger(trigger.getTrigger());
								update();
							});
					i++;
				}
			}

			// "revert all" button - top right to hopefully prevent accidental presses
			int numberOfCustomTriggers = mPlugin.mAbilityManager.getNumberOfCustomTriggers(mPlayer);
			if (numberOfCustomTriggers > 0) {
				setItem(8, GUIUtils.createBasicItem(Material.BARRIER, 1, "Revert all triggers to defaults", NamedTextColor.DARK_RED, false,
						Component.text("This resets all triggers of all abilities of all classes back to defaults!\nYou currently have ", NamedTextColor.RED)
							.append(Component.text(numberOfCustomTriggers, NamedTextColor.GOLD))
							.append(Component.text(" custom trigger" + (numberOfCustomTriggers == 1 ? "" : "s") + " defined.", NamedTextColor.RED)), 40, true)).onLeftClick(() -> {
					mPlugin.mAbilityManager.clearCustomTriggers(mPlayer);
					for (Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilitiesInTriggerOrder()) {
						for (AbilityTriggerInfo<?> trigger : ability.getCustomTriggers()) {
							trigger.setTrigger(new AbilityTrigger(ability.getInfo().getTrigger(trigger.getId()).getTrigger()));
						}
					}
					update();
				});
			}
		} else {
			// back icon
			setItem(0, 0, GUIUtils.createBasicItem(Material.ARROW, "Back",
					NamedTextColor.GRAY, false, "Return to the trigger selection page.", NamedTextColor.GRAY, 40))
					.onLeftClick(() -> {
						mSelectedAbility = null;
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
			setItem(0, 4, GUIUtils.createBasicItem(mSelectedAbility.getDisplayItem().getType(), 1,
					mSelectedAbility.getDisplayName() + " - " + mSelectedAbility.getDisplayName(), NamedTextColor.GOLD, false,
					summary, true));

			// options
			makeOptionIcons(1, 0, GUIUtils.createBasicItem(Material.BARRIER, mNewTrigger.isEnabled() ? "Trigger enabled" : "Trigger disabled", mNewTrigger.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED, false,
					"Click to " + (mNewTrigger.isEnabled() ? "disable" : "enable") + " the trigger", NamedTextColor.GRAY, 40), mNewTrigger.isEnabled() ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, () -> {
				mNewTrigger.setEnabled(!mNewTrigger.isEnabled());
				update();
			});
			makeOptionIcons(1, 1, GUIUtils.createBasicItem(Material.JIGSAW, "Key: " + mNewTrigger.getKey(), NamedTextColor.WHITE, false,
					"Click to cycle through main key.\nNote that this also changes the \"extras\" when changed.", NamedTextColor.GRAY, 40), switch (mNewTrigger.getKey()) {
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
			makeOptionIcons(1, 3, GUIUtils.createBasicItem(Material.SHEARS, "Double click: " + (mNewTrigger.isDoubleClick() ? "yes" : "no"), mNewTrigger.isDoubleClick() ? NamedTextColor.GREEN : NamedTextColor.GRAY, false,
					"Click to toggle requiring a double click", NamedTextColor.GRAY, 40), mNewTrigger.isDoubleClick() ? Material.GREEN_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, () -> {
				mNewTrigger.setDoubleClick(!mNewTrigger.isDoubleClick());
				update();
			});
			makeBinaryOptionIcon(1, 4, Material.FEATHER, "sneaking", mNewTrigger.getSneaking(), mNewTrigger::setSneaking);
			makeBinaryOptionIcon(1, 5, Material.LEATHER_BOOTS, "sprinting", mNewTrigger.getSprinting(), mNewTrigger::setSprinting);
			makeBinaryOptionIcon(1, 6, Material.GRASS_BLOCK, "on ground", mNewTrigger.getOnGround(), mNewTrigger::setOnGround);

			String looking = "Looking " + (mNewTrigger.getLookDirections().size() == 3 ? "anywhere"
					: mNewTrigger.getLookDirections().stream().map(d -> d.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(" or ")));
			makeOptionIcons(1, 7, GUIUtils.createBasicItem(Material.HEART_OF_THE_SEA, looking, NamedTextColor.WHITE, false,
					"Click to cycle through look directions", NamedTextColor.GRAY, 40), mNewTrigger.getLookDirections().size() == 3 ? Material.GRAY_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, () -> {
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

			// extras aka key options
			setItem(3, 0, GUIUtils.createBasicItem(Material.CHAIN_COMMAND_BLOCK, "Extras", NamedTextColor.WHITE, false,
					"Extra options for held items.\n"
							+ "When the main key is changed these are set to defaults, unless the trigger has an unchangeable item restriction.", NamedTextColor.GRAY, 40));
			int col = 1;
			for (AbilityTrigger.KeyOptions keyOption : AbilityTrigger.KeyOptions.values()) {
				Material mat = switch (keyOption) {
					case NO_POTION -> Material.POTION;
					case NO_FOOD -> Material.COOKED_BEEF;
					case NO_PROJECTILE_WEAPON -> Material.CROSSBOW;
					case NO_SHIELD -> Material.SHIELD;
					case NO_BLOCKS -> Material.COBBLESTONE;
					case NO_MISC -> Material.COMPASS;
					case NO_PICKAXE -> Material.IRON_PICKAXE;
					case SNEAK_WITH_SHIELD -> Material.SHIELD;
				};
				boolean enabled = mNewTrigger.getKeyOptions().contains(keyOption);
				makeOptionIcons(3, col++, GUIUtils.createBasicItem(mat, capitalize(keyOption.getDisplay(enabled)), enabled ? NamedTextColor.RED : NamedTextColor.GRAY, false,
						"Click to toggle", NamedTextColor.GRAY, 40), enabled ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE, () -> {
					EnumSet<AbilityTrigger.KeyOptions> keyOptions = mNewTrigger.getKeyOptions();
					if (!keyOptions.remove(keyOption)) {
						keyOptions.add(keyOption);
					}
					update();
				});
			}

			// accept/cancel buttons
			if (!mNewTrigger.equals(mSelectedTrigger.getTrigger())) {
				{
					ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
					ItemMeta meta = confirm.getItemMeta();
					meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
					meta.lore(List.of(Component.text("Accept trigger changes", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
					confirm.setItemMeta(meta);
					ItemUtils.setPlainTag(confirm);
					setItem(5, 2, confirm).onLeftClick(() -> {
						mSelectedTrigger.setTrigger(mNewTrigger);
						mPlugin.mAbilityManager.setCustomTrigger(mPlayer, mSelectedAbility, mSelectedTrigger.getId(), mNewTrigger);
						ProtocolLibrary.getProtocolManager().updateEntity(mPlayer, ProtocolLibrary.getProtocolManager().getEntityTrackers(mPlayer));
						mSelectedAbility = null;
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
					setItem(5, 6, cancel).onLeftClick(() -> {
						mNewTrigger = new AbilityTrigger(mSelectedTrigger.getTrigger());
						mSelectedAbility = null;
						update();
					});
				}
			}

			// revert button
			if (!mSelectedTrigger.getTrigger().equals(mSelectedAbility.getTrigger(mSelectedTrigger.getId()).getTrigger())) {
				setItem(5, 4, GUIUtils.createBasicItem(Material.BARRIER, "Revert to default", NamedTextColor.DARK_RED, false,
						"Revert any custom trigger changes", NamedTextColor.GRAY, 40)).onLeftClick(() -> {
					mNewTrigger = new AbilityTrigger(mSelectedAbility.getTrigger(mSelectedTrigger.getId()).getTrigger());
					mSelectedTrigger.setTrigger(mNewTrigger);
					mPlugin.mAbilityManager.setCustomTrigger(mPlayer, mSelectedAbility, mSelectedTrigger.getId(), null);
					mSelectedAbility = null;
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
		setItem(row + 1, column, indicator).onLeftClick(onClick);
	}

	private void makeBinaryOptionIcon(int row, int column, Material material, String name, AbilityTrigger.BinaryOption value, Consumer<AbilityTrigger.BinaryOption> setter) {
		String displayName = value == AbilityTrigger.BinaryOption.TRUE ? "Must be " + name
				: value == AbilityTrigger.BinaryOption.FALSE ? "Not " + name
				: capitalize(name) + " or not " + name;
		NamedTextColor color = value == AbilityTrigger.BinaryOption.TRUE ? NamedTextColor.GREEN
				: value == AbilityTrigger.BinaryOption.FALSE ? NamedTextColor.RED
				: NamedTextColor.GRAY;
		makeOptionIcons(row, column, GUIUtils.createBasicItem(material, displayName, color, false,
				"Click to cycle through options", NamedTextColor.GRAY, 40), value, () -> {
			setter.accept(AbilityTrigger.BinaryOption.values()[(value.ordinal() + 1) % AbilityTrigger.BinaryOption.values().length]);
			update();
		});
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

}
