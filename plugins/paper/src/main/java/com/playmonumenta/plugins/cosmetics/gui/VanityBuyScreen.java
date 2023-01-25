package com.playmonumenta.plugins.cosmetics.gui;

import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

class VanityBuyScreen extends Gui {
	private final ItemStack mItem;
	private final VanityGUI mVanityGUI;

	private static final NamespacedKey TWISTED_STRAND_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/delves/items/twisted_strand");
	private static @Nullable ItemStack mTwistedStrand;

	public VanityBuyScreen(Player player, ItemStack item, VanityGUI vanityGUI) {
		super(player, 4 * 9, Component.text("Unlock Vanity Item?", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		mItem = item;
		mVanityGUI = vanityGUI;
		setFiller(CosmeticsGUI.FILLER);
	}

	@Override
	protected void setup() {
		{
			ItemStack vanityItem = ItemUtils.clone(mItem);
			ItemMeta meta = vanityItem.getItemMeta();
			String slot = switch (ItemUtils.getEquipmentSlot(mItem)) {
				case HEAD -> "Head";
				case CHEST -> "Chest";
				case LEGS -> "Legs";
				case FEET -> "Feet";
				default -> "Offhand";
			};
			meta.lore(List.of(Component.text("Base material: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
				                  .append(Component.translatable(vanityItem.getType().translationKey())),
				Component.text("Slot: " + slot, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			meta.addItemFlags(ItemFlag.values());
			vanityItem.setItemMeta(meta);
			ItemUtils.setPlainTag(vanityItem);
			setItem(1, 4, vanityItem);
		}

		{
			ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
			ItemMeta meta = confirm.getItemMeta();
			meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Pay " + VanityGUI.STRAND_COST_PER_VANITY_UNLOCK + " Twisted Strands to unlock", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("this item for vanity use.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("All items of the same type as this item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("(base material + name) will permanently be", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("unlocked for you for use as vanity.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			confirm.setItemMeta(meta);
			ItemUtils.setPlainTag(confirm);
			setItem(2, 2, confirm).onLeftClick(this::confirm);
		}
		{
			ItemStack cancel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
			ItemMeta meta = cancel.getItemMeta();
			meta.displayName(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			meta.lore(List.of(Component.text("Do not unlock this item.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			cancel.setItemMeta(meta);
			ItemUtils.setPlainTag(cancel);
			setItem(2, 6, cancel).onLeftClick(this::close);
		}
	}

	private void confirm() {
		if (mTwistedStrand == null) {
			mTwistedStrand = InventoryUtils.getItemFromLootTable(mPlayer, TWISTED_STRAND_LOOT_TABLE);
		}
		if (!mPlayer.getInventory().containsAtLeast(mTwistedStrand, VanityGUI.STRAND_COST_PER_VANITY_UNLOCK)) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
			mPlayer.sendMessage(Component.text("You don't have enough Twisted Strands to pay for this!", NamedTextColor.RED));
			close();
			return;
		}
		mTwistedStrand.setAmount(VanityGUI.STRAND_COST_PER_VANITY_UNLOCK);
		mPlayer.getInventory().removeItem(mTwistedStrand);
		mPlugin.mVanityManager.unlockVanity(mPlayer, mItem);
		EquipmentSlot slot = ItemUtils.getEquipmentSlot(mItem);
		if (slot == EquipmentSlot.HAND) {
			slot = EquipmentSlot.OFF_HAND;
		}
		mPlugin.mVanityManager.getData(mPlayer).equip(slot, mItem);
		close();
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		Bukkit.getScheduler().runTask(mPlugin, () -> new VanityGUI(mVanityGUI, mPlayer).open());
	}

}
