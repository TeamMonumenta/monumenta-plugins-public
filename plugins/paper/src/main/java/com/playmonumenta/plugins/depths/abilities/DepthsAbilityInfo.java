package com.playmonumenta.plugins.depths.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsAbilityItem;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class DepthsAbilityInfo<T extends DepthsAbility> extends AbilityInfo<T> {

	private final DepthsTrigger mDepthsTrigger;
	private final @Nullable DepthsTree mDepthsTree;
	private boolean mSingleAbilityCharm;
	private boolean mHasLevels;
	private Consumer<Player> mGain;
	private Predicate<Player> mOfferable;
	private Predicate<Integer> mFloors;

	public DepthsAbilityInfo(Class<T> abilityClass, String displayName, BiFunction<Plugin, Player, T> constructor,
	                         @Nullable DepthsTree depthsTree, DepthsTrigger depthsTrigger) {
		super(abilityClass, displayName, constructor);
		mDepthsTree = depthsTree;
		mDepthsTrigger = depthsTrigger;
		mSingleAbilityCharm = true;
		mHasLevels = depthsTree != DepthsTree.CURSE && depthsTrigger != DepthsTrigger.WEAPON_ASPECT && depthsTree != DepthsTree.GIFT;
		mGain = player -> {
		};
		mOfferable = player -> true;
		mFloors = floor -> true;
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
	public DepthsAbilityInfo<T> cooldown(int cooldown, String charmCooldown) {
		super.cooldown(cooldown, charmCooldown);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> cooldown(String charmCooldown, int... cooldowns) {
		super.cooldown(charmCooldown, cooldowns);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> displayItem(Material displayItem) {
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

	public DepthsAbilityInfo<T> gain(Consumer<Player> gain) {
		mGain = gain;
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> descriptions(IntFunction<Description<T>> supplier, int levels) {
		super.descriptions(supplier, levels);
		return this;
	}

	public DepthsAbilityInfo<T> descriptions(BiFunction<Integer, TextColor, Description<T>> supplier) {
		descriptions(i -> supplier.apply(i, DepthsUtils.getRarityColor(i)), DepthsAbility.MAX_RARITY);
		return this;
	}

	public DepthsAbilityInfo<T> descriptions(Supplier<Description<T>> supplier) {
		this.descriptions(unused -> supplier.get(), 1);
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> description(String description) {
		super.description(description);
		return this;
	}

	public DepthsAbilityInfo<T> singleCharm(boolean canBeSingleAbilityCharm) {
		mSingleAbilityCharm = canBeSingleAbilityCharm;
		return this;
	}

	public DepthsAbilityInfo<T> hasLevels(boolean hasLevels) {
		mHasLevels = hasLevels;
		return this;
	}

	public DepthsAbilityInfo<T> offerable(Predicate<Player> offerable) {
		mOfferable = offerable;
		return this;
	}

	public DepthsAbilityInfo<T> floors(Predicate<Integer> floors) {
		mFloors = floors;
		return this;
	}

	@Override
	public DepthsAbilityInfo<T> actionBarColor(TextColor color) {
		super.actionBarColor(color);
		return this;
	}

	@Override
	public int getBaseCooldown(Player player, int score) {
		if (mCooldowns == null) {
			return 0;
		}

		return mCooldowns.get(Math.min(score - 1, mCooldowns.size() - 1));
	}

	public DepthsTrigger getDepthsTrigger() {
		return mDepthsTrigger;
	}

	public @Nullable DepthsTree getDepthsTree() {
		return mDepthsTree;
	}

	public boolean getSingleAbilityCharm() {
		return mSingleAbilityCharm;
	}

	public boolean getHasLevels() {
		return mHasLevels;
	}

	public void onGain(Player player) {
		mGain.accept(player);
	}

	// Whether the player is eligible to have this ability offered
	public boolean canBeOffered(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return false;
		}

		// Make sure the player doesn't have this ability already
		if (dp.hasAbility(getDisplayName())) {
			return false;
		}

		// Weapon aspects are manually offered by the system
		if (mDepthsTrigger == DepthsTrigger.WEAPON_ASPECT) {
			return false;
		}

		// Make sure player doesn't already have an ability with the same trigger
		if (mDepthsTrigger != DepthsTrigger.PASSIVE) {
			for (DepthsAbilityInfo<?> ability : DepthsManager.getAbilities()) {
				// Iterate over abilities and return false if the player has an ability with the same trigger already
				if (ability.getDepthsTrigger() == mDepthsTrigger && dp.hasAbility(ability.getDisplayName())) {
					return false;
				}
			}
		}

		//Skip passive abilities if they have wand aspect charges
		if (dp.mWandAspectCharges > 0 && mDepthsTrigger == DepthsTrigger.PASSIVE && mDepthsTree != DepthsTree.PRISMATIC && mDepthsTree != DepthsTree.CURSE) {
			return false;
		}

		DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp);
		if (party == null) {
			return false;
		}
		return checkConditions(player, party);
	}

	public boolean checkConditions(Player player, DepthsParty party) {
		return mOfferable.test(player) && mFloors.test(party.getFloor());
	}

	public @Nullable DepthsAbilityItem getAbilityItem(int rarity, @Nullable Player player) {
		return getAbilityItem(rarity, player, 0, 0, true);
	}

	public @Nullable DepthsAbilityItem getAbilityItem(int rarity, @Nullable Player player, int oldRarity) {
		return getAbilityItem(rarity, player, oldRarity, 0, false);
	}

	/**
	 * Returns the ability item to display in GUIs given the input rarity
	 *
	 * @param rarity            the rarity to put on the item
	 * @param player            the player who has or can get this ability
	 * @param oldRarity         the rarity the player already has/had in this ability (0 to not include)
	 * @param preIncreaseRarity the rarity before it was increased by abilities such as Enlightenment (0 to not include)
	 * @param useAbility        whether to use current ability data in the description
	 * @return                  the item to display
	 */
	public @Nullable DepthsAbilityItem getAbilityItem(int rarity, @Nullable Player player, int oldRarity, int preIncreaseRarity, boolean useAbility) {
		if (rarity <= 0) {
			//This should never happen
			return null;
		}
		DepthsAbilityItem item = null;

		//Don't crash our abilities because of a null item
		try {
			if (mDepthsTree == null) {
				rarity = 1;
			}
			ItemStack stack = createAbilityItem(rarity, oldRarity, preIncreaseRarity, player, useAbility);
			String name = getDisplayName() == null ? "" : getDisplayName(); // It's never actually null
			item = new DepthsAbilityItem(stack, name, rarity, oldRarity, preIncreaseRarity, useAbility, mDepthsTrigger, mDepthsTree);
		} catch (Exception e) {
			MMLog.warning("Invalid depths ability item: " + getDisplayName());
			e.printStackTrace();
		}
		return item;
	}

	public ItemStack createAbilityItem(int rarity, int oldRarity, int preIncreaseRarity, @Nullable Player player, boolean useAbility) {
		Material mat = getDisplayItem();
		if (mat == null) {
			return GUIUtils.createBasicItem(Material.BARRIER, "You should not be seeing this. Please report this bug.", NamedTextColor.RED);
		}
		String name = getDisplayName();
		ItemStack stack = new ItemStack(mat, 1);
		ItemMeta meta = stack.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.displayName(getColoredName().colorIfAbsent(NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		if (mDepthsTree != null) {
			List<Component> lore = new ArrayList<>();
			if (mHasLevels) {
				lore.add(DepthsUtils.getLoreForItem(mDepthsTree, rarity, oldRarity, preIncreaseRarity));
			} else {
				lore.add(mDepthsTree.getNameComponent());
			}
			meta.lore(lore);
		}
		Component description = getDescription(mHasLevels ? rarity : 1, player, useAbility);
		GUIUtils.splitLoreLine(meta, description, 30, false);
		stack.setItemMeta(meta);
		ItemUtils.setPlainName(stack, name);
		return stack;
	}

	public Component getColoredName() {
		String name = getDisplayName();
		if (name == null) {
			return Component.empty();
		}
		if (mDepthsTree == null) {
			return Component.text(name);
		}
		return mDepthsTree.color(name);
	}

	public Component getNameWithHover(int rarity, int prevRarity, Player player, boolean useAbility) {
		ItemStack item = createAbilityItem(rarity, prevRarity, 0, player, useAbility);
		return getColoredName().hoverEvent(item.asHoverEvent());
	}
}
