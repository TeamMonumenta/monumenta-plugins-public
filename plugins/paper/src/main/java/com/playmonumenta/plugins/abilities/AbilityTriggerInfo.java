package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

	@FunctionalInterface
	public interface TriggerAction<T extends Ability> {
		boolean run(T ability);
	}

	public static final TriggerRestriction HOLDING_PROJECTILE_WEAPON_RESTRICTION =
		new TriggerRestriction("holding a projectile weapon", player -> ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()) && !Grappling.playerHoldingHook(player));
	public static final TriggerRestriction NOT_HOLDING_PROJECTILE_WEAPON_RESTRICTION =
		new TriggerRestriction("not holding a projectile weapon", player -> !ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()));

	public static final TriggerRestriction HOLDING_MAGIC_WAND_RESTRICTION =
		new TriggerRestriction("holding a wand", player -> Plugin.getInstance().mItemStatManager.getEnchantmentLevel(player, EnchantmentType.MAGIC_WAND) > 0);
	public static final TriggerRestriction HOLDING_SCYTHE_RESTRICTION =
		new TriggerRestriction("holding a scythe", player -> ItemUtils.isHoe(player.getInventory().getItemInMainHand()));
	public static final TriggerRestriction HOLDING_TWO_SWORDS_RESTRICTION =
		new TriggerRestriction("holding two swords", player -> InventoryUtils.rogueTriggerCheck(Plugin.getInstance(), player));

	private final String mId;

	private final String mDisplayName;
	private final @Nullable String mDescription;

	private final TriggerAction<T> mAction;

	private AbilityTrigger mTrigger;

	private final @Nullable TriggerRestriction mRestriction;

	private final @Nullable Predicate<Player> mPrerequisite;

	public static class TriggerRestriction {
		private final String mDisplay;
		private final Predicate<Player> mPredicate;
		private final boolean mIncludeInDescription;

		public TriggerRestriction(String display, Predicate<Player> predicate) {
			this(display, predicate, true);
		}

		public TriggerRestriction(String display, Predicate<Player> predicate, boolean includeInDescription) {
			mDisplay = display;
			mPredicate = predicate;
			mIncludeInDescription = includeInDescription;
		}

		public String getDisplay() {
			return mDisplay;
		}

		public Predicate<Player> getPredicate() {
			return mPredicate;
		}

		public boolean includeInDescription() {
			return mIncludeInDescription;
		}

		public boolean test(Player player) {
			return mPredicate.test(player);
		}
	}

	public AbilityTriggerInfo(String id, String displayName, TriggerAction<T> action, AbilityTrigger trigger) {
		this(id, displayName, null, action, trigger, null);
	}

	public AbilityTriggerInfo(String id, String displayName, TriggerAction<T> action, AbilityTrigger trigger, @Nullable TriggerRestriction restriction) {
		this(id, displayName, null, action, trigger, restriction);
	}

	public AbilityTriggerInfo(String id, String displayName, @Nullable String description, TriggerAction<T> action, AbilityTrigger trigger, @Nullable TriggerRestriction restriction) {
		this(id, displayName, description, action, trigger, restriction, null);
	}

	public AbilityTriggerInfo(String id, String displayName, TriggerAction<T> action, DepthsTrigger depthsTrigger) {
		this(id, displayName, null, action, depthsTrigger.mTrigger, DepthsTrigger.DEPTHS_TRIGGER_RESTRICTION);
	}

	public AbilityTriggerInfo(String id, String displayName, @Nullable String description, TriggerAction<T> action, AbilityTrigger trigger, @Nullable TriggerRestriction restriction, @Nullable Predicate<Player> prerequisite) {
		mId = id;
		mDisplayName = displayName;
		mDescription = description;
		mAction = action;
		mTrigger = trigger;
		mRestriction = restriction;
		mPrerequisite = prerequisite;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public @Nullable String getDescription() {
		return mDescription;
	}

	public TriggerAction<T> getAction() {
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

	public boolean meetsPrerequsite(Player player) {
		return mPrerequisite == null || mPrerequisite.test(player);
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
		return mTrigger.check(player, key) && (mRestriction == null || mRestriction.getPredicate().test(player)) && meetsPrerequsite(player);
	}

	public AbilityTriggerInfo<T> withCustomTrigger(AbilityInfo<?> ability, Player player) {
		AbilityTrigger customTrigger = Plugin.getInstance().mAbilityManager.getCustomTrigger(player, ability, mId);
		return new AbilityTriggerInfo<>(mId, mDisplayName, mDescription, mAction, customTrigger != null ? customTrigger : mTrigger, mRestriction, mPrerequisite);
	}

	public List<Component> getTriggerDescription() {
		List<Component> desc = mTrigger.getDescription();
		if (mRestriction != null && mTrigger.isEnabled()) {
			desc.add(Component.text("- unchangeable: ", NamedTextColor.RED)
				.append(Component.text(mRestriction.getDisplay(), NamedTextColor.WHITE)));
		}
		return desc;
	}

	public Component getAsNaturalLanguage(@Nullable String extraCondition) {
		AbilityTrigger.Key key = mTrigger.getKey();
		Component keyComp = switch (key) {
			case RIGHT_CLICK -> Component.text("Right click");
			case LEFT_CLICK -> Component.text("Left click");
			case SWAP -> Component.text("Press ").append(Component.keybind(Constants.Keybind.SWAP_OFFHAND));
			case DROP -> Component.text("Press ").append(Component.keybind(Constants.Keybind.DROP));
		};

		StringBuilder builder = new StringBuilder();
		if (mTrigger.isDoubleClick()) {
			builder.append(" twice");
		}

		List<String> conditions = new ArrayList<>();
		if (mRestriction != null && mRestriction.includeInDescription()) {
			conditions.add(mRestriction.getDisplay());
		}
		// This is the only key option we actually want to list. Others would mostly clutter the descriptions
		if (mTrigger.getKeyOptions().contains(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)) {
			conditions.add(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON.getDisplay(true));
		}

		AbilityTrigger.BinaryOption sneaking = mTrigger.getSneaking();
		AbilityTrigger.BinaryOption sprinting = mTrigger.getSprinting();
		// Don't show "not sneaking" if we require sprinting; don't show "not sprinting" if we require sneaking
		if (!(sneaking == AbilityTrigger.BinaryOption.FALSE && sprinting == AbilityTrigger.BinaryOption.TRUE)) {
			addCondition(conditions, sneaking, "sneaking");
		}
		if (!(sprinting == AbilityTrigger.BinaryOption.FALSE && sneaking == AbilityTrigger.BinaryOption.TRUE)) {
			addCondition(conditions, sprinting, "sprinting");
		}
		addCondition(conditions, mTrigger.getOnGround(), "on the ground");

		List<AbilityTrigger.LookDirection> dirs = new ArrayList<>(mTrigger.getLookDirections());
		if (dirs.size() < 3 && !dirs.isEmpty()) {
			String desc = "";
			if (dirs.size() == 2) {
				desc += "not ";
				dirs = new ArrayList<>(EnumSet.complementOf(mTrigger.getLookDirections()));
			}
			// We now always have exactly one element left
			desc += "looking " + dirs.get(0).mName;
			conditions.add(desc);
		}

		if (extraCondition != null) {
			conditions.add(extraCondition);
		}

		if (!conditions.isEmpty()) {
			builder.append(" while ");
			if (conditions.size() <= 2) {
				builder.append(conditions.get(0));
				if (conditions.size() == 2) {
					builder.append(" and ").append(conditions.get(1));
				}
			} else {
				for (int i = 0; i < conditions.size(); i++) {
					if (i != 0) {
						builder.append(", ");
					}
					if (i == conditions.size() - 1) {
						builder.append("and ");
					}
					builder.append(conditions.get(i));
				}
			}
		}

		if (mTrigger.getKeyOptions().contains(AbilityTrigger.KeyOptions.SNEAK_WITH_SHIELD) && mTrigger.getSneaking() == AbilityTrigger.BinaryOption.EITHER) {
			builder.append(" (sneaking if holding shield)");
		}

		return keyComp.append(Component.text(builder.toString()));
	}

	// Helper method for description
	private void addCondition(List<String> conditions, AbilityTrigger.BinaryOption option, String trueString) {
		if (option == AbilityTrigger.BinaryOption.TRUE) {
			conditions.add(trueString);
		} else if (option == AbilityTrigger.BinaryOption.FALSE) {
			conditions.add("not " + trueString);
		}
	}

}
