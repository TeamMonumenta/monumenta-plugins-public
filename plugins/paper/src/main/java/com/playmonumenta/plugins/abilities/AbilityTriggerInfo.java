package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents one triggerable action of an {@link Ability} - holds the action, the trigger to cast it,
 * and an optional cast restriction that cannot be changed by the player (e.g. mage spells require holding a wand).
 * <p>
 * None of the values in this class can be changed by the player. If something should be modifiable, put it into {@link AbilityTrigger}.
 *
 * @param <T> type of the ability this trigger is for
 */
public class AbilityTriggerInfo<T extends Ability> {

	public static final TriggerRestriction HOLDING_PROJECTILE_WEAPON_RESTRICTION =
		new TriggerRestriction("holding a projectile weapon", player -> ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()));
	public static final TriggerRestriction NOT_HOLDING_PROJECTILE_WEAPON_RESTRICTION =
		new TriggerRestriction("not holding a projectile weapon", player -> !ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()));

	public static final TriggerRestriction HOLDING_MAGIC_WAND_RESTRICTION =
		new TriggerRestriction("holding a wand", player -> Plugin.getInstance().mItemStatManager.getPlayerItemStats(player).getItemStats().get(ItemStatUtils.EnchantmentType.MAGIC_WAND) > 0);
	public static final TriggerRestriction HOLDING_SCYTHE_RESTRICTION =
		new TriggerRestriction("holding a scythe", player -> ItemUtils.isHoe(player.getInventory().getItemInMainHand()));
	public static final TriggerRestriction HOLDING_TWO_SWORDS_RESTRICTION =
		new TriggerRestriction("holding two swords", player -> InventoryUtils.rogueTriggerCheck(Plugin.getInstance(), player));

	private final String mId;

	private final String mDisplayName;
	private final @Nullable String mDescription;

	private final Consumer<T> mAction;

	private AbilityTrigger mTrigger;

	private final @Nullable TriggerRestriction mRestriction;

	public static class TriggerRestriction {
		private final String mDisplay;
		private final Predicate<Player> mPredicate;

		public TriggerRestriction(String display, Predicate<Player> predicate) {
			mDisplay = display;
			mPredicate = predicate;
		}

		public String getDisplay() {
			return mDisplay;
		}

		public Predicate<Player> getPredicate() {
			return mPredicate;
		}
	}

	public AbilityTriggerInfo(String id, String displayName, Consumer<T> action, AbilityTrigger trigger) {
		this(id, displayName, null, action, trigger, null);
	}

	public AbilityTriggerInfo(String id, String displayName, Consumer<T> action, AbilityTrigger trigger, @Nullable TriggerRestriction restriction) {
		this(id, displayName, null, action, trigger, restriction);
	}

	public AbilityTriggerInfo(String id, String displayName, @Nullable String description, Consumer<T> action, AbilityTrigger trigger, @Nullable TriggerRestriction restriction) {
		mId = id;
		mDisplayName = displayName;
		mDescription = description;
		mAction = action;
		mTrigger = trigger;
		mRestriction = restriction;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public @Nullable String getDescription() {
		return mDescription;
	}

	public Consumer<T> getAction() {
		return mAction;
	}

	public String getId() {
		return mId;
	}

	public void setTrigger(AbilityTrigger trigger) {
		this.mTrigger = trigger;
	}

	public AbilityTrigger getTrigger() {
		return mTrigger;
	}

	public @Nullable TriggerRestriction getRestriction() {
		return mRestriction;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("displayName", mDisplayName);
		json.add("trigger", mTrigger.toJson());
		if (mRestriction != null) {
			json.addProperty("restriction", mRestriction.mDisplay);
		}
		return json;
	}

	public boolean check(Player player, AbilityTrigger.Key key) {
		return mTrigger.check(player, key) && (mRestriction == null || mRestriction.getPredicate().test(player));
	}

	public AbilityTriggerInfo<T> withCustomTrigger(AbilityInfo<?> ability, Player player) {
		AbilityTrigger customTrigger = Plugin.getInstance().mAbilityManager.getCustomTrigger(player, ability, mId);
		return new AbilityTriggerInfo<>(mId, mDisplayName, mDescription, mAction, customTrigger != null ? customTrigger : mTrigger, mRestriction);
	}

	public List<Component> getTriggerDescription() {
		List<Component> desc = mTrigger.getDescription();
		if (mRestriction != null && mTrigger.isEnabled()) {
			desc.add(Component.text("- unchangeable: ", NamedTextColor.RED)
				         .append(Component.text(mRestriction.getDisplay(), NamedTextColor.WHITE)));
		}
		return desc;
	}

}
