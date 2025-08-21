package com.playmonumenta.plugins.managers.travelanchor;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class TravelUiTarget {
	private static final float DISTANCE = 0.75f;
	private static final float EXTRA_ICON_DISTANCE = 0.1f;
	private static final float TEXT_DELTA_HEIGHT = 0.04f;
	private static final float ICON_SCALE = 0.05f;
	private static final float TEXT_SCALE = 0.1f;

	private final TravelUi mUi;
	private final EntityTravelAnchor mAnchor;
	private final ItemDisplay mGroupIconEntity;
	private final TextDisplay mLabelEntity;

	protected TravelUiTarget(TravelUi ui, EntityTravelAnchor anchor) {
		mUi = ui;
		mAnchor = anchor;
		String label = anchor.label();
		TextColor textColor = anchor.color();

		Location loc = getLocation();
		World world = loc.getWorld();

		mGroupIconEntity = world.spawn(loc, ItemDisplay.class, groupIconEntity -> {
			Transformation transformation = groupIconEntity.getTransformation();

			groupIconEntity.setBillboard(Display.Billboard.FIXED);
			groupIconEntity.customName(Component.text(label, textColor));
			groupIconEntity.setItemStack(Objects.requireNonNullElseGet(currentGroupItem(),
				() -> new ItemStack(Material.AIR)));
			groupIconEntity.setTransformation(new Transformation(
				new Vector3f(0.0f, 0.0f, EXTRA_ICON_DISTANCE),
				transformation.getLeftRotation(),
				new Vector3f(ICON_SCALE, ICON_SCALE, ICON_SCALE),
				transformation.getRightRotation()
			));
			groupIconEntity.addScoreboardTag("TravelAnchorUiLabel");
			groupIconEntity.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			groupIconEntity.setVisibleByDefault(false);
		});
		mLabelEntity = world.spawn(loc, TextDisplay.class, textDisplay -> {
			Transformation transformation = textDisplay.getTransformation();

			textDisplay.setBillboard(Display.Billboard.CENTER);
			textDisplay.text(Component.text(label, textColor));
			textDisplay.setTransformation(new Transformation(
				new Vector3f(0.0f, TEXT_DELTA_HEIGHT, 0.0f),
				transformation.getLeftRotation(),
				new Vector3f(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE),
				transformation.getRightRotation()
			));
			textDisplay.addScoreboardTag("TravelAnchorUiLabel");
			textDisplay.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			textDisplay.setVisibleByDefault(false);
		});
		ui.mPlayer.showEntity(Plugin.getInstance(), mGroupIconEntity);
		ui.mPlayer.showEntity(Plugin.getInstance(), mLabelEntity);
	}

	public Vector getDir() {
		Location eyeLoc = mUi.mPlayer.getEyeLocation();
		return mAnchor.lastPos().clone().subtract(eyeLoc.toVector()).normalize();
	}

	public Location getLocation() {
		Location eyeLoc = mUi.mPlayer.getEyeLocation();
		Vector dir = getDir();
		Vector deltaPos = dir.clone().multiply(DISTANCE);
		Location resultLoc = eyeLoc.clone().add(deltaPos).setDirection(dir.clone().multiply(-1));
		if (dir.getX() == 0.0f && dir.getZ() == 0.0f) {
			resultLoc.setYaw(resultLoc.getYaw() + 180.0f);
		}
		return resultLoc;
	}

	public EntityTravelAnchor getAnchor() {
		return mAnchor;
	}

	public ItemDisplay getGroupIconEntity() {
		return mGroupIconEntity;
	}

	public TextDisplay getLabelEntity() {
		return mLabelEntity;
	}

	public Set<UUID> getEntityIds() {
		Set<UUID> result = new HashSet<>();
		result.add(mAnchor.getEntityId());
		result.add(mGroupIconEntity.getUniqueId());
		result.add(mLabelEntity.getUniqueId());
		return result;
	}

	public void animate() {
		Location loc = getLocation();
		mGroupIconEntity.teleport(loc);
		mLabelEntity.teleport(loc);

		mGroupIconEntity.setItemStack(Objects.requireNonNullElseGet(currentGroupItem(),
			() -> new ItemStack(Material.AIR)));

		String label = mAnchor.label();
		if (!equals(mUi.mClosestTarget)) {
			TextColor textColor = mAnchor.color();
			Component labelComponent = Component.text(label, textColor);
			mGroupIconEntity.customName(labelComponent);
			mLabelEntity.text(labelComponent);
			return;
		}

		char[] charArray = label.toCharArray();
		List<Component> charComponents = new ArrayList<>();
		for (int i = 0; i < charArray.length; i++) {
			char c = charArray[i];
			float h = (((float) i / (float) charArray.length) + ((float) mUi.mRainbowAnimationPhase / 360.0f)) % 1.0f;
			charComponents.add(Component.text(c, TextColor.color(HSVLike.hsvLike(h, 1.0f, 1.0f))));
		}
		Component labelComponent = Component.join(JoinConfiguration.noSeparators(), charComponents)
			.decoration(TextDecoration.BOLD, true);
		mGroupIconEntity.customName(labelComponent);
		mLabelEntity.text(labelComponent);
	}

	public @Nullable ItemStack currentGroupItem() {
		List<AnchorGroup> commonGroups = new ArrayList<>(mUi.mStartingAnchor.commonGroups(mAnchor));
		int count = commonGroups.size();
		if (count == 0) {
			return null;
		}
		AnchorGroup currentGroup = commonGroups.get(mUi.mGroupIndex % count);
		return currentGroup.item(mAnchor.color());
	}

	public boolean shouldRemove() {
		return mAnchor.getEntity() == null;
	}

	public void remove() {
		mGroupIconEntity.remove();
		mLabelEntity.remove();
	}
}
