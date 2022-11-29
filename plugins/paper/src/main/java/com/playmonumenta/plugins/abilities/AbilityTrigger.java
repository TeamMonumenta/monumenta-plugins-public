package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Description of an ability trigger - holds key, if sneaking is required, etc.
 * All values in this class can be changed by the player - if something must be unchangeable, put it into
 * {@link AbilityTriggerInfo}.
 */
public class AbilityTrigger {

	private static final float LOOK_DIRECTION_CUTOFF_ANGLE = 50;

	public enum BinaryOption {
		TRUE, FALSE, EITHER;
	}

	public enum Key {
		LEFT_CLICK("Left Click"), RIGHT_CLICK("Right Click"), SWAP("Swap Hands"),
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
		NO_USABLE_ITEMS("not holding a potion, projectile weapon, shield, block, etc. in the main hand", player -> {
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			return !(ItemUtils.isShootableItem(mainhand, false)
				         || ItemUtils.isSomePotion(mainhand)
				         || mainhand.getType().isBlock()
				         || mainhand.getType().isEdible()
				         || mainhand.getType() == Material.COMPASS
				         || mainhand.getType() == Material.SHIELD
				         || Plugin.getInstance().mItemStatManager.getEnchantmentLevel(player, ItemStatUtils.EnchantmentType.MULTITOOL) > 0);
		}),
		NO_PICKAXE("not holding a pickaxe", player -> !ItemUtils.isPickaxe(player.getInventory().getItemInMainHand())),
		SNEAK_WITH_SHIELD("sneaking if holding a shield", player -> player.isSneaking() || !(player.getInventory().getItemInMainHand().getType() == Material.SHIELD || player.getInventory().getItemInOffHand().getType() == Material.SHIELD));

		private final String mDisplay;

		private final Predicate<Player> mPredicate;

		KeyOptions(String display, Predicate<Player> predicate) {
			mDisplay = display;
			mPredicate = predicate;
		}

		public String getDisplay() {
			return mDisplay;
		}

		@Override
		public String toString() {
			return mDisplay;
		}
	}

	public enum LookDirection {
		DOWN, LEVEL, UP
	}

	private static final AtomicInteger mNextMetadataId = new AtomicInteger();
	private final int mMetedataId = mNextMetadataId.getAndIncrement();

	private Key mKey;
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

	public static AbilityTrigger fromJson(JsonObject json) {
		try {
			AbilityTrigger trigger = new AbilityTrigger(Key.valueOf(json.get("key").getAsString()));
			for (JsonElement keyOption : json.get("keyOptions").getAsJsonArray()) {
				trigger.mKeyOptions.add(KeyOptions.valueOf(keyOption.getAsString()));
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
		return mDoubleClick == that.mDoubleClick && mKey == that.mKey && mKeyOptions.equals(that.mKeyOptions) && mSneaking == that.mSneaking
			       && mSprinting == that.mSprinting && mOnGround == that.mOnGround && mLookDirections.equals(that.mLookDirections);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mKey, mKeyOptions, mSneaking, mSprinting, mOnGround, mLookDirections, mDoubleClick);
	}

	public String getDescription() {
		StringBuilder description = new StringBuilder(ChatColor.GOLD + mKey.mDisplay + ChatColor.RESET + "\n");
		if (mDoubleClick) {
			description.append("- double click\n");
		}
		if (mSneaking != BinaryOption.EITHER) {
			description.append("- ").append(mSneaking == BinaryOption.FALSE ? "not " : "").append("sneaking\n");
		}
		if (mSprinting != BinaryOption.EITHER) {
			description.append("- ").append(mSprinting == BinaryOption.FALSE ? "not " : "").append("sprinting\n");
		}
		if (mOnGround != BinaryOption.EITHER) {
			description.append("- ").append(mOnGround == BinaryOption.FALSE ? "not " : "").append("on ground\n");
		}
		if (mLookDirections.size() < 3) {
			description.append("- looking ").append(mLookDirections.stream().map(d -> d.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(" or "))).append("\n");
		}
		for (KeyOptions keyOption : mKeyOptions) {
			description.append("- ").append(keyOption.mDisplay).append("\n");
		}
		return description.toString();
	}

}
