package com.playmonumenta.plugins.guis;

import com.comphenix.protocol.ProtocolLibrary;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.custominventories.ClassSelectionCustomInventory;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AbilityTriggersGui extends Gui {

	private static final Component MAIN_PAGE_TITLE = Component.text("Ability Triggers");


	private AbilityInfo<?> mSelectedAbility;
	private AbilityTriggerInfo<?> mSelectedTrigger;
	private AbilityTrigger mNewTrigger;

	public AbilityTriggersGui(Player player) {
		super(player, 6 * 9, MAIN_PAGE_TITLE);
	}

	@Override
	protected void setup() {
		if (mSelectedAbility == null) {
			// back icon
			setItem(0, createBasicItem(Material.ARROW, "Back", NamedTextColor.GRAY, false,
				"Return to the class selection page.", ChatColor.GRAY))
				.onLeftClick(() -> new ClassSelectionCustomInventory(mPlayer).openInventory(mPlayer, mPlugin));

			// help icon
			setItem(4, createBasicItem(Material.OAK_SIGN, "Help", NamedTextColor.WHITE, false,
				"Click on a trigger to change it.\n" +
					"Triggers are shown in the order they are handled. Whenever a key is pressed, the top-left trigger is checked first if it matches. " +
					"If not, the next trigger is checked, and so forth until a trigger matches and casts its ability.\n" +
					"Eagle Eye is an exception: it allows other abilities to trigger after it.", ChatColor.GRAY));

			// trigger icons
			int i = 0;
			for (Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilitiesInTriggerOrder()) {
				AbilityInfo<?> info = ability.getInfo();
				for (AbilityTriggerInfo<?> trigger : ability.getCustomTriggers()) {
					setItem(2 + (i / 7), 1 + (i % 7), createBasicItem(info.getDisplayItem().getType(),
						info.getDisplayName() + " - " + trigger.getDisplayName(), NamedTextColor.GOLD, false,
						(trigger.getTrigger().equals(info.getTrigger(trigger.getId()).getTrigger()) ? ChatColor.GRAY + "Current Trigger (default):" : ChatColor.AQUA + "Custom Trigger:") + ChatColor.RESET + "\n"
							+ trigger.getDescription(), ChatColor.WHITE))
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
				setItem(8, createBasicItem(Material.BARRIER, "Revert all triggers to defaults", NamedTextColor.DARK_RED, false,
					"This resets all triggers of all abilities of all classes back to defaults!\n" +
						"You currently have " + ChatColor.GOLD + numberOfCustomTriggers + ChatColor.RED
						+ " custom trigger" + (numberOfCustomTriggers == 1 ? "" : "s") + " defined.", ChatColor.RED)).onLeftClick(() -> {
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
			setItem(0, createBasicItem(Material.ARROW, "Back",
				NamedTextColor.GRAY, false, "Return to the trigger selection page.", ChatColor.GRAY))
				.onLeftClick(() -> {
					mSelectedAbility = null;
					update();
				});

			// summary icon
			setItem(4, createBasicItem(mSelectedAbility.getDisplayItem().getType(),
				mSelectedAbility.getDisplayName() + " - " + mSelectedAbility.getDisplayName(), NamedTextColor.GOLD, false,
				ChatColor.GRAY + "Trigger summary:\n" + mNewTrigger.getDescription() +
					(mSelectedTrigger.getRestriction() == null ? "" : ChatColor.RED + "- unchangeable: " + ChatColor.WHITE + mSelectedTrigger.getRestriction().getDisplay()), ChatColor.WHITE));

			// options
			setItem(2, 3, createBasicItem(Material.JIGSAW, "Key: " + mNewTrigger.getKey(), NamedTextColor.WHITE, false,
				"Click to cycle through main key.\nNote that this also changes the \"extras\" when changed.", ChatColor.GRAY)).onLeftClick(() -> {
				mNewTrigger.setKey(AbilityTrigger.Key.values()[(mNewTrigger.getKey().ordinal() + 1) % AbilityTrigger.Key.values().length]);
				mNewTrigger.getKeyOptions().clear();
				if (mSelectedTrigger.getRestriction() == null) {
					if (mNewTrigger.getKey() == AbilityTrigger.Key.LEFT_CLICK) {
						mNewTrigger.getKeyOptions().add(AbilityTrigger.KeyOptions.NO_PICKAXE);
					} else if (mNewTrigger.getKey() == AbilityTrigger.Key.RIGHT_CLICK) {
						mNewTrigger.getKeyOptions().add(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS);
					}
				}
				update();
			});
			setItem(2, 5, createBasicItem(Material.SHEARS, "Double Click: " + (mNewTrigger.isDoubleClick() ? "Yes" : "No"), mNewTrigger.isDoubleClick() ? NamedTextColor.GREEN : NamedTextColor.RED, false,
				"Click to toggle requiring a double click", ChatColor.GRAY)).onLeftClick(() -> {
				mNewTrigger.setDoubleClick(!mNewTrigger.isDoubleClick());
				update();
			});
			makeBinaryOptionIcon(3, 3, Material.FEATHER, "Sneaking", mNewTrigger.getSneaking(), mNewTrigger::setSneaking);
			makeBinaryOptionIcon(3, 4, Material.LEATHER_BOOTS, "Sprinting", mNewTrigger.getSprinting(), mNewTrigger::setSprinting);
			makeBinaryOptionIcon(3, 5, Material.GRASS_BLOCK, "On Ground", mNewTrigger.getOnGround(), mNewTrigger::setOnGround);

			setItem(4, 3, createBasicItem(Material.BOW, "Extras", NamedTextColor.WHITE, false,
				ChatColor.GRAY + "Click to cycle through some extras.\n"
					+ ChatColor.GRAY + "When the main key is changed,\n"
					+ ChatColor.GRAY + "these are set to defaults.\n"
					+ ChatColor.WHITE + (mNewTrigger.getKeyOptions().isEmpty() ? "- none" :
						                     "- " + mNewTrigger.getKeyOptions().stream().map(AbilityTrigger.KeyOptions::getDisplay).collect(Collectors.joining("\n- "))), ChatColor.WHITE))
				.onLeftClick(() -> {
					// TODO allow choosing multiple options somehow - probably via new GUI?
					EnumSet<AbilityTrigger.KeyOptions> keyOptions = mNewTrigger.getKeyOptions();
					AbilityTrigger.KeyOptions[] values = AbilityTrigger.KeyOptions.values();
					if (keyOptions.size() != 1) {
						keyOptions.clear();
						keyOptions.add(values[0]);
					} else {
						AbilityTrigger.KeyOptions option = keyOptions.iterator().next();
						keyOptions.clear();
						if (option.ordinal() + 1 < values.length) {
							keyOptions.add(values[option.ordinal() + 1]);
						}
					}
					mNewTrigger.setKey(AbilityTrigger.Key.values()[(mNewTrigger.getKey().ordinal() + 1) % AbilityTrigger.Key.values().length]);
					update();
				});
			String looking = "Looking " + (mNewTrigger.getLookDirections().size() == 3 ? "anywhere"
				                               : mNewTrigger.getLookDirections().stream().map(d -> d.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(" or ")));
			setItem(4, 5, createBasicItem(Material.HEART_OF_THE_SEA, looking, NamedTextColor.WHITE, false,
				"Click to cycle through look directions", ChatColor.GRAY)).onLeftClick(() -> {
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

			// accept/cancel buttons
			if (!mNewTrigger.equals(mSelectedTrigger.getTrigger())) {
				{
					ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
					ItemMeta meta = confirm.getItemMeta();
					meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
					meta.lore(List.of(Component.text("Accept trigger changes.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
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
				setItem(5, 4, createBasicItem(Material.BARRIER, "Revert to default", NamedTextColor.DARK_RED, false,
					"Revert any custom trigger changes", ChatColor.GRAY)).onLeftClick(() -> {
					mNewTrigger = new AbilityTrigger(mSelectedAbility.getTrigger(mSelectedTrigger.getId()).getTrigger());
					mSelectedTrigger.setTrigger(mNewTrigger);
					mPlugin.mAbilityManager.setCustomTrigger(mPlayer, mSelectedAbility, mSelectedTrigger.getId(), null);
					mSelectedAbility = null;
					update();
				});
			}
		}
	}

	private void makeBinaryOptionIcon(int row, int column, Material material, String name, AbilityTrigger.BinaryOption value, Consumer<AbilityTrigger.BinaryOption> setter) {
		String displayName = value == AbilityTrigger.BinaryOption.TRUE ? "Must be " + name
			                     : value == AbilityTrigger.BinaryOption.FALSE ? "Not " + name
				                       : name + " or not " + name;
		NamedTextColor color = value == AbilityTrigger.BinaryOption.TRUE ? NamedTextColor.GREEN
			                       : value == AbilityTrigger.BinaryOption.FALSE ? NamedTextColor.RED
				                         : NamedTextColor.GRAY;
		setItem(row, column, createBasicItem(material, displayName, color, false,
			"Click to cycle through options", ChatColor.GRAY)).onLeftClick(() -> {
			setter.accept(AbilityTrigger.BinaryOption.values()[(value.ordinal() + 1) % AbilityTrigger.BinaryOption.values().length]);
			update();
		});
	}

	public ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
			                 .decoration(TextDecoration.ITALIC, false)
			                 .decoration(TextDecoration.BOLD, nameBold));
		GUIUtils.splitLoreLine(meta, desc, 40, loreColor, true);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

}
