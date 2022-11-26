package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DepthsAbilityInfo<T extends DepthsAbility> extends AbilityInfo<T> {

	private final DepthsTrigger mDepthsTrigger;
	private final @Nullable DepthsTree mDepthsTree;

	public DepthsAbilityInfo(Class<T> abilityClass, String displayName, BiFunction<Plugin, Player, T> constructor,
	                         @Nullable DepthsTree depthsTree, DepthsTrigger depthsTrigger) {
		super(abilityClass, displayName, constructor);
		mDepthsTree = depthsTree;
		mDepthsTrigger = depthsTrigger;
		canUse(player -> DepthsManager.getInstance().getPlayerLevelInAbility(displayName, player) > 0);
	}

	@Override
	public DepthsAbilityInfo<T> linkedSpell(ClassAbility linkedSpell) {
		super.linkedSpell(linkedSpell);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> scoreboardId(String scoreboardId) {
		super.scoreboardId(scoreboardId);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> shorthandName(String shorthandName) {
		super.shorthandName(shorthandName);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> cooldown(int cooldown) {
		super.cooldown(cooldown, cooldown, cooldown, cooldown, cooldown, cooldown);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> cooldown(int... cooldowns) {
		super.cooldown(cooldowns);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> displayItem(ItemStack displayItem) {
		super.displayItem(displayItem);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> addTrigger(AbilityTriggerInfo<T> trigger) {
		super.addTrigger(trigger);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> priorityAmount(double priorityAmount) {
		super.priorityAmount(priorityAmount);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> canUse(Predicate<Player> canUse) {
		super.canUse(canUse);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> remove(Consumer<Player> remove) {
		super.remove(remove);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> descriptions(IntFunction<String> supplier, int levels) {
		super.descriptions(supplier, levels);
		return this;
	}

	public DepthsAbilityInfo<T> description(String description) {
		super.descriptions(i -> description, 1);
		return this;
	}

	public DepthsTrigger getDepthsTrigger() {
		return mDepthsTrigger;
	}

	public DepthsTree getDepthsTree() {
		return mDepthsTree;
	}

	//Whether the player is eligible to have this ability offered
	public boolean canBeOffered(Player player) {

		// Make sure the player doesn't have this ability already
		if (DepthsManager.getInstance().getPlayerLevelInAbility(getDisplayName(), player) > 0) {
			return false;
		}

		// Weapon aspects are manually offered by the system
		if (mDepthsTrigger == DepthsTrigger.WEAPON_ASPECT) {
			return false;
		}

		// Make sure player doesn't already have an ability with the same trigger
		if (mDepthsTrigger != DepthsTrigger.PASSIVE && DepthsManager.getInstance().isInSystem(player)) {
			for (DepthsAbilityInfo<?> ability : DepthsManager.getAbilities()) {
				// Iterate over abilities and return false if the player has an ability with the same trigger already
				if (DepthsManager.getInstance().getPlayerLevelInAbility(ability.getDisplayName(), player) > 0 && ability.getDepthsTrigger() == mDepthsTrigger) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns the ability item to display in GUIs given the input rarity
	 *
	 * @param rarity the rarity to put on the item
	 * @return the item to display
	 */
	public @Nullable DepthsAbilityItem getAbilityItem(int rarity) {
		DepthsAbilityItem item = null;

		//Don't crash our abilities because of a null item
		try {
			item = new DepthsAbilityItem();
			item.mRarity = rarity;
			item.mAbility = getDisplayName();
			item.mTrigger = mDepthsTrigger;
			ItemStack stack = getDisplayItem();
			ItemMeta meta = stack.getItemMeta();
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			TextColor color = mDepthsTree == null ? NamedTextColor.WHITE : mDepthsTree.getColor();
			meta.displayName(Component.text("" + ChatColor.BOLD + getDisplayName(), color).decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			if (rarity > 0 && mDepthsTree != null) {
				lore.add(DepthsUtils.getLoreForItem(mDepthsTree, rarity));
			}
			meta.lore(lore);
			GUIUtils.splitLoreLine(meta, getDescription(rarity), 30, ChatColor.WHITE, false);
			stack.setItemMeta(meta);
			ItemUtils.setPlainName(stack, getDisplayName());
			item.mItem = stack;
		} catch (Exception e) {
			Plugin.getInstance().getLogger().info("Invalid depths ability item: " + getDisplayName());
			e.printStackTrace();
		}
		return item;
	}

}
