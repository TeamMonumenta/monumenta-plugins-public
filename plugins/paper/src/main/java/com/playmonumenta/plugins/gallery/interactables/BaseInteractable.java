package com.playmonumenta.plugins.gallery.interactables;

import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.gallery.GalleryUtils;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *           Base class of Interactable Object.
 *           The player can interact with this object by press the swap key
 *           while having the Gallery Brochure on main hand,
 *           then if canInteractWithObject(...) return true interactWithObject(...) will run.
 *
 */
public class BaseInteractable {
	public static final String TAG_STRING = "GalleryInteractable";

	protected final @NotNull String mName;
	protected final @NotNull Entity mArmorStand;
	protected final @Nullable String mCommand;

	protected @Nullable String mShowingText;

	protected boolean mIsInteractable;
	protected boolean mIsShowingMessage;

	public BaseInteractable(@NotNull String name, @NotNull Entity entity, @Nullable String showingText, @Nullable String command) {
		mName = name;
		mArmorStand = entity;
		mShowingText = showingText;
		mCommand = command;
		mIsInteractable = true;
		mIsShowingMessage = false;
	}

	public String getName() {
		return mName;
	}

	public void setInteractable(Boolean bool) {
		mIsInteractable = bool;
		save();
	}

	public void setShowingText(String text) {
		mShowingText = text;
		save();
	}

	public boolean isInteractable() {
		return mIsInteractable;
	}

	public @NotNull Location getLocation() {
		return mArmorStand.getLocation();
	}

	public @NotNull Entity getEntity() {
		return mArmorStand;
	}

	public boolean shouldShowMessage(GalleryPlayer player) {
		return mIsInteractable && mShowingText != null && player.isOnline() && player.getPlayer().getLocation().distance(this.getLocation()) < 7;
	}

	public void showMessage() {
		if (mIsShowingMessage) {
			return;
		}
		mIsShowingMessage = true;
		mArmorStand.setCustomNameVisible(mShowingText != null);
		mArmorStand.customName(Component.text(mShowingText != null ? mShowingText : ""));
	}

	public void removeMessage() {
		if (!mIsShowingMessage) {
			return;
		}
		mIsShowingMessage = false;
		mArmorStand.customName(Component.empty());
		mArmorStand.setCustomNameVisible(false);
	}

	public boolean canInteractWithObject(GalleryGame game, GalleryPlayer player) {
		return isInteractable() && player.getPlayer() != null && player.getPlayer().getLocation().distance(this.getLocation()) < 3;
	}

	public boolean interactWithObject(GalleryGame game, GalleryPlayer player) {
		if (mCommand != null) {
			GalleryUtils.runCommandAsEntity(mArmorStand, mCommand);
		}
		return true;
	}

	//Save this Interactable into this entity
	public void save() {
		new HashSet<>(mArmorStand.getScoreboardTags()).forEach(s -> {
			if (s.startsWith(TAG_STRING + "-")) {
				mArmorStand.removeScoreboardTag(s);
			}
		});
		//TAG_STRING-name-showing text-True/False-function to run
		mArmorStand.addScoreboardTag(TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING + "-" + mName + "-" + (mShowingText != null ? mShowingText : "null") + "-" + (mIsInteractable ? "True" : "False") + "-" + (mCommand != null ? mCommand : "null"));
	}

	public void remove() {
		mArmorStand.remove();
	}

	//----------------Static functions--------------------

	public static BaseInteractable fromEntity(Entity entity) {
		Set<String> tags = new HashSet<>(entity.getScoreboardTags());
		try {
			if (tags.contains(BasePricedInteractable.TAG_STRING)) {
				return BasePricedInteractable.fromEntity(entity);
			}

			if (tags.contains(MysteryBoxInteractable.TAG_STRING)) {
				return MysteryBoxInteractable.fromEntity(entity);
			}

			if (tags.contains(EffectInteractable.TAG_STRING)) {
				return EffectInteractable.fromEntity(entity);
			}


			//no other match
			//this is made of a BaseInteractable
			for (String tag : tags) {
				if (tag.startsWith(TAG_STRING + "-")) {
					String[] split = tag.split("-");
					String name = split[1];
					String text = readNullableString(split[2]);
					boolean canInteract = readBoolean(split[3]);
					String func = readNullableString(split[4]);
					BaseInteractable interactable = new BaseInteractable(name, entity, text, func);
					interactable.setInteractable(canInteract);
					return interactable;
				}
			}
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Exception while reading entity. Reason: " + e.getMessage());
			e.printStackTrace();
		}

		//how do we get here?
		GalleryUtils.printDebugMessage("Returning NULL BaseInteractable!! this is a problem!");
		GalleryUtils.printDebugMessage("location: " + entity.getLocation().getBlockX() + " " + entity.getLocation().getBlockY() + " " + entity.getLocation().getBlockZ());
		GalleryUtils.printDebugMessage("Tags: " + tags);

		return null;
	}

	public static @Nullable String readNullableString(@NotNull String string) {
		if (string.equals("null")) {
			return null;
		}
		return string;
	}

	public static @Nullable Integer readNullableInteger(@NotNull String string) {
		if (string.equals("null")) {
			return null;
		}
		return Integer.valueOf(string);
	}

	public static boolean readBoolean(@NotNull String string) {
		return string.equalsIgnoreCase("true");
	}


}
