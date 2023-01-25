package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Description of an ability trigger - holds key, if sneaking is required, etc.
 * All values in this class can be changed by the player - if something must be unchangeable, put it into
 * {@link AbilityTriggerInfo}.
 */
public class AbilityTrigger {

	private static final float LOOK_DIRECTION_CUTOFF_ANGLE = 50;

	public enum BinaryOption {
		TRUE, FALSE, EITHER;

		public static BinaryOption ofBoolean(boolean b) {
			return b ? TRUE : FALSE;
		}
	}

	public enum Key {
		LEFT_CLICK("Left Click"),
		RIGHT_CLICK("Right Click"),
		SWAP("Swap Hands"),
		DROP("Drop Item"),
		;

		private final String mDisplay;

		Key(String display) {
			mDisplay = display;
		}

		@Override
		public String toString() {
			return mDisplay;
		}
	}

	public enum KeyOptions {
		NO_POTION("not holding a potion", "may be holding a potion",
			player -> !ItemUtils.isSomePotion(player.getInventory().getItemInMainHand())),
		NO_FOOD("not holding food", "may be holding food",
			player -> !player.getInventory().getItemInMainHand().getType().isEdible()),
		NO_PROJECTILE_WEAPON("not holding a projectile weapon", "may be holding a projectile weapon",
			player -> !ItemUtils.isShootableItem(player.getInventory().getItemInMainHand())),
		NO_SHIELD("not holding a shield", "may be holding a shield",
			player -> player.getInventory().getItemInMainHand().getType() != Material.SHIELD),
		NO_BLOCKS("not holding blocks", "may be holding blocks",
			player -> !player.getInventory().getItemInMainHand().getType().isBlock()),
		NO_MISC("not holding a compass, a multitool, or a riptide trident while swimming", "may be holding a compass, a multitool, or a riptide trident while swimming",
			player -> !(player.getInventory().getItemInMainHand().getType() == Material.COMPASS
				|| Plugin.getInstance().mItemStatManager.getEnchantmentLevel(player, ItemStatUtils.EnchantmentType.MULTITOOL) > 0
				|| ((LocationUtils.isLocationInWater(player.getLocation()) || player.isInRain()) && ItemStatUtils.hasEnchantment(player.getInventory().getItemInMainHand(), ItemStatUtils.EnchantmentType.RIPTIDE)))
			),
		NO_PICKAXE("not holding a pickaxe", "may be holding a pickaxe",
			player -> !ItemUtils.isPickaxe(player.getInventory().getItemInMainHand())),
		SNEAK_WITH_SHIELD("sneaking if holding a shield", "no sneak requirement if holding a shield",
			player -> player.isSneaking() || !(player.getInventory().getItemInMainHand().getType() == Material.SHIELD || player.getInventory().getItemInOffHand().getType() == Material.SHIELD));

		public static final KeyOptions[] NO_USABLE_ITEMS = {
			NO_POTION,
			NO_FOOD,
			NO_PROJECTILE_WEAPON,
			NO_SHIELD,
			NO_BLOCKS,
			NO_MISC
		};

		private final String mEnabledDisplay;
		private final String mDisabledDisplay;

		private final Predicate<Player> mPredicate;

		KeyOptions(String enabledDisplay, String disabledDisplay, Predicate<Player> predicate) {
			mEnabledDisplay = enabledDisplay;
			mDisabledDisplay = disabledDisplay;
			mPredicate = predicate;
		}

		public String getDisplay(boolean enabled) {
			return enabled ? mEnabledDisplay : mDisabledDisplay;
		}

		@Override
		public String toString() {
			return mEnabledDisplay;
		}
	}

	public enum LookDirection {
		DOWN, LEVEL, UP
	}

	private static final AtomicInteger mNextMetadataId = new AtomicInteger();
	private final int mMetedataId = mNextMetadataId.getAndIncrement();

	private Key mKey;

	private boolean mEnabled = true;

	private final EnumSet<KeyOptions> mKeyOptions = EnumSet.noneOf(KeyOptions.class);

	private BinaryOption mSneaking = BinaryOption.EITHER;
	private BinaryOption mSprinting = BinaryOption.EITHER;
	private BinaryOption mOnGround = BinaryOption.EITHER;

	private final EnumSet<LookDirection> mLookDirections = EnumSet.allOf(LookDirection.class);

	private boolean mDoubleClick = false;


	// Note: if you add a new field, make sure to update the copy constructor, fromJson, toJson, equals, and hashCode

	public AbilityTrigger(Key key) {
		mKey = key;
	}

	public AbilityTrigger(AbilityTrigger original) {
		mKey = original.mKey;
		mEnabled = original.mEnabled;
		mKeyOptions.clear();
		mKeyOptions.addAll(original.mKeyOptions);
		mSneaking = original.mSneaking;
		mSprinting = original.mSprinting;
		mOnGround = original.mOnGround;
		mLookDirections.clear();
		mLookDirections.addAll(original.mLookDirections);
		mDoubleClick = original.mDoubleClick;
	}

	// builder methods

	public AbilityTrigger key(Key key) {
		mKey = key;
		return this;
	}

	public AbilityTrigger keyOptions(KeyOptions... options) {
		mKeyOptions.addAll(List.of(options));
		return this;
	}

	public AbilityTrigger sneaking(boolean sneaking) {
		mSneaking = sneaking ? BinaryOption.TRUE : BinaryOption.FALSE;
		return this;
	}

	public AbilityTrigger sprinting(boolean sprinting) {
		mSprinting = sprinting ? BinaryOption.TRUE : BinaryOption.FALSE;
		return this;
	}

	public AbilityTrigger onGround(boolean onGround) {
		mOnGround = onGround ? BinaryOption.TRUE : BinaryOption.FALSE;
		return this;
	}

	public AbilityTrigger lookDirections(LookDirection... lookDirections) {
		mLookDirections.clear();
		mLookDirections.addAll(List.of(lookDirections));
		return this;
	}

	public AbilityTrigger doubleClick() {
		mDoubleClick = true;
		return this;
	}

	// getters and setters

	public Key getKey() {
		return mKey;
	}

	public void setKey(Key key) {
		this.mKey = key;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public void setEnabled(boolean enabled) {
		this.mEnabled = enabled;
	}

	public EnumSet<KeyOptions> getKeyOptions() {
		return mKeyOptions;
	}

	public BinaryOption getSneaking() {
		return mSneaking;
	}

	public void setSneaking(BinaryOption sneaking) {
		this.mSneaking = sneaking;
	}

	public BinaryOption getSprinting() {
		return mSprinting;
	}

	public void setSprinting(BinaryOption sprinting) {
		this.mSprinting = sprinting;
	}

	public BinaryOption getOnGround() {
		return mOnGround;
	}

	public void setOnGround(BinaryOption onGround) {
		this.mOnGround = onGround;
	}

	public EnumSet<LookDirection> getLookDirections() {
		return mLookDirections;
	}

	public boolean isDoubleClick() {
		return mDoubleClick;
	}

	public void setDoubleClick(boolean doubleClick) {
		this.mDoubleClick = doubleClick;
	}

	// other methods

	public static @Nullable AbilityTrigger fromJson(JsonObject json) {
		try {
			AbilityTrigger trigger = new AbilityTrigger(Key.valueOf(json.get("key").getAsString()));
			if (json.has("enabled")) { // old triggers didn't have this
				trigger.mEnabled = json.get("enabled").getAsBoolean();
			}
			for (JsonElement keyOption : json.get("keyOptions").getAsJsonArray()) {
				if ("NO_USABLE_ITEMS".equals(keyOption.getAsString())) { // update old triggers
					trigger.mKeyOptions.addAll(List.of(KeyOptions.NO_USABLE_ITEMS));
				} else {
					trigger.mKeyOptions.add(KeyOptions.valueOf(keyOption.getAsString()));
				}
			}
			trigger.mSneaking = BinaryOption.valueOf(json.get("sneaking").getAsString());
			trigger.mSprinting = BinaryOption.valueOf(json.get("sprinting").getAsString());
			trigger.mOnGround = BinaryOption.valueOf(json.get("onGround").getAsString());
			trigger.mLookDirections.clear();
			for (JsonElement lookDirection : json.get("lookDirections").getAsJsonArray()) {
				trigger.mLookDirections.add(LookDirection.valueOf(lookDirection.getAsString()));
			}
			trigger.mDoubleClick = json.get("doubleClick").getAsBoolean();
			return trigger;
		} catch (NullPointerException | IllegalArgumentException | ClassCastException | IllegalStateException e) {
			// Missing or invalid value: ignore the custom trigger and reset to default.
			// Log with info level just in case triggers get unexpectedly reset when they shouldn't to get at least some info.
			MMLog.info("Error when reading AbilityTrigger JSON: " + e);
			return null;
		}
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("key", mKey.name());
		json.addProperty("enabled", mEnabled);
		JsonArray keyOptions = new JsonArray();
		for (KeyOptions keyOption : mKeyOptions) {
			keyOptions.add(keyOption.name());
		}
		json.add("keyOptions", keyOptions);
		json.addProperty("sneaking", mSneaking.name());
		json.addProperty("sprinting", mSprinting.name());
		json.addProperty("onGround", mOnGround.name());
		JsonArray lookDirection = new JsonArray();
		for (LookDirection direction : mLookDirections) {
			lookDirection.add(direction.name());
		}
		json.add("lookDirections", lookDirection);
		json.addProperty("doubleClick", mDoubleClick);
		return json;
	}

	public boolean check(Player player, Key key) {
		if (!mEnabled) {
			return false;
		}
		if (key != mKey) {
			return false;
		}
		for (KeyOptions keyOption : mKeyOptions) {
			if (!keyOption.mPredicate.test(player)) {
				return false;
			}
		}
		if (mSneaking != BinaryOption.EITHER && player.isSneaking() != (mSneaking == BinaryOption.TRUE)) {
			return false;
		}
		if (mSprinting != BinaryOption.EITHER && player.isSprinting() != (mSprinting == BinaryOption.TRUE)) {
			return false;
		}
		if (mOnGround != BinaryOption.EITHER && player.isOnGround() == (mOnGround == BinaryOption.FALSE)) {
			return false;
		}
		LookDirection lookDirection = player.getLocation().getPitch() < -LOOK_DIRECTION_CUTOFF_ANGLE ? LookDirection.UP
			                              : player.getLocation().getPitch() > LOOK_DIRECTION_CUTOFF_ANGLE ? LookDirection.DOWN
				                                : LookDirection.LEVEL;
		if (!mLookDirections.contains(lookDirection)) {
			return false;
		}
		if (mDoubleClick) {
			String metadataKey = "DoubleClickCheck_" + mMetedataId;
			int currentTick = Bukkit.getServer().getCurrentTick();
			int lastClick = MetadataUtils.getMetadata(player, metadataKey, 0);
			if (currentTick - lastClick <= 5) {
				MetadataUtils.removeMetadata(player, metadataKey);
				return true;
			} else {
				MetadataUtils.setMetadata(player, metadataKey, currentTick);
				return false;
			}
		}
		// note: double click check must be last, so add new options above that, not here
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AbilityTrigger that)) {
			return false;
		}
		return mEnabled == that.mEnabled && mDoubleClick == that.mDoubleClick && mKey == that.mKey && mKeyOptions.equals(that.mKeyOptions) && mSneaking == that.mSneaking
			       && mSprinting == that.mSprinting && mOnGround == that.mOnGround && mLookDirections.equals(that.mLookDirections);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mEnabled, mKey, mKeyOptions, mSneaking, mSprinting, mOnGround, mLookDirections, mDoubleClick);
	}

	public List<Component> getDescription() {
		if (!mEnabled) {
			return List.of(Component.text("Trigger is disabled!", NamedTextColor.RED));
		}
		List<Component> desc = new ArrayList<>();
		desc.add(Component.text(mKey.mDisplay, NamedTextColor.GOLD));
		if (mDoubleClick) {
			desc.add(Component.text("- double click"));
		}
		if (mSneaking != BinaryOption.EITHER) {
			desc.add(Component.text("- " + (mSneaking == BinaryOption.FALSE ? "not " : "") + "sneaking"));
		}
		if (mSprinting != BinaryOption.EITHER) {
			desc.add(Component.text("- " + (mSprinting == BinaryOption.FALSE ? "not " : "") + "sprinting"));
		}
		if (mOnGround != BinaryOption.EITHER) {
			desc.add(Component.text("- " + (mOnGround == BinaryOption.FALSE ? "not " : "") + "on ground"));
		}
		if (mLookDirections.size() < 3) {
			desc.add(Component.text("- looking " + mLookDirections.stream().map(d -> d.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(" or "))));
		}
		EnumSet<KeyOptions> keyOptions = EnumSet.copyOf(mKeyOptions);
		if (keyOptions.containsAll(List.of(KeyOptions.NO_USABLE_ITEMS))) {
			desc.add(Component.text("- not holding a usable item"));
			List.of(KeyOptions.NO_USABLE_ITEMS).forEach(keyOptions::remove);
		}
		for (KeyOptions keyOption : keyOptions) {
			desc.add(Component.text("- " + keyOption.mEnabledDisplay));
		}
		return desc;
	}

}
