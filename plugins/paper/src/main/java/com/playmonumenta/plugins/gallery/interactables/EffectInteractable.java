package com.playmonumenta.plugins.gallery.interactables;

import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.gallery.GalleryUtils;
import com.playmonumenta.plugins.gallery.effects.GalleryEffectType;
import java.util.HashSet;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.jmh.runner.RunnerException;

public class EffectInteractable extends BasePricedInteractable {
	public static final String TAG_STRING = "GalleryEffectInteractable";

	private final GalleryEffectType mType;

	public EffectInteractable(@NotNull String name, @NotNull Entity entity, @NotNull GalleryEffectType type, @Nullable Integer price, @Nullable String showingText, @Nullable String command) {
		super(name, entity, price, showingText, command);
		mType = type;
	}

	@Override public boolean canInteractWithObject(GalleryGame game, GalleryPlayer player) {
		return super.canInteractWithObject(game, player) && mType.canBuy(player);
	}

	@Override public boolean interactWithObjectAfterPay(GalleryPlayer player) {
		player.giveEffect(mType.newEffect());
		return super.interactWithObjectAfterPay(player);
	}

	@Override
	public void save() {
		new HashSet<>(mArmorStand.getScoreboardTags()).forEach(s -> {
			if (s.startsWith(TAG_STRING + "-")) {
				mArmorStand.removeScoreboardTag(s);
			}
		});
		mArmorStand.addScoreboardTag(BaseInteractable.TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING + "-" + mName + "-" + mType.name() + "-" + (mPrice != null ? mPrice : "null") + "-" + (mShowingText != null ? mShowingText : "null") + "-" + (mIsInteractable ? "True" : "False") + "-" + (mCommand != null ? mCommand : "null"));
	}

	public static EffectInteractable fromEntity(Entity entity) {
		EffectInteractable interactable = null;

		try {
			for (String tag : new HashSet<>(entity.getScoreboardTags())) {
				if (tag.startsWith(TAG_STRING + "-")) {
					String[] split = tag.split("-");
					String name = split[1];
					GalleryEffectType type = GalleryEffectType.fromName(readNullableString(split[2]));
					if (type == null) {
						throw new RunnerException("effect TYPE == null ? for entity at: " + entity.getLocation().getBlockX() + " " + entity.getLocation().getBlockY() + " " + entity.getLocation().getBlockZ() + " \n tag: " + tag);
					}
					Integer price = readNullableInteger(split[3]);
					String text = readNullableString(split[4]);
					boolean isInteractable = readBoolean(split[5]);
					String func = readNullableString(split[6]);
					interactable = new EffectInteractable(name, entity, type, price, text, func);
					interactable.setInteractable(isInteractable);
				}
			}
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Catch an exception while converting BasePricedInteractable. Reason: " + e.getMessage());
			e.printStackTrace();
		}
		return interactable;
	}
}
