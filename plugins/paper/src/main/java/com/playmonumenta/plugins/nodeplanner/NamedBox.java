package com.playmonumenta.plugins.nodeplanner;

import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import static com.playmonumenta.plugins.nodeplanner.NodePlanner.TAG;

public class NamedBox {
	// The magic numbers here are the measured width/height of a Text Display entity for 2x2 chars vs 1x1 chars
	// This is done with the font minecraft:uniform because it is monospace, unlike the default font

	// 2x <1 char text display size> - <2 char text display size> = 0 char/min width/height
	public static final float TEXT_DISPLAY_MIN_WIDTH = 2.0f * 0.125f - 0.225f;
	public static final float TEXT_DISPLAY_MIN_HEIGHT = 2.0f * 0.275f - 0.525f;

	// <1 char text display size> - <0 char/min width/height> = delta size per char
	public static final float TEXT_DISPLAY_CHAR_WIDTH = 0.125f - TEXT_DISPLAY_MIN_WIDTH;
	public static final float TEXT_DISPLAY_CHAR_HEIGHT = 0.275f - TEXT_DISPLAY_MIN_HEIGHT;

	private Location mBottom;
	private final Vector mSize;
	private final Component mLabel;
	private final Interaction mInteraction;
	private final BlockDisplay mBlockDisplay;
	private final TextDisplay mTextDisplay;

	public NamedBox(Location location, float width, float height, BlockData blockData, Component label) {
		mBottom = location.clone();
		mSize = new Vector(width, height, width);
		mLabel = label.font(NamespacedKey.fromString("minecraft:uniform"));

		float halfWidth = width * 0.5f;
		Interaction interaction = mBottom.getWorld().spawn(mBottom, Interaction.class);
		interaction.setInteractionWidth(width);
		interaction.setInteractionHeight(height);
		interaction.customName(mLabel);
		interaction.addScoreboardTag(TAG);
		mInteraction = interaction;

		Location blockDisplayLocation = mBottom.clone().subtract(halfWidth, 0.0, halfWidth);
		BlockDisplay blockDisplay = blockDisplayLocation.getWorld().spawn(blockDisplayLocation, BlockDisplay.class);
		blockDisplay.setBlock(blockData);
		setDisplayEntityScale(blockDisplay, mSize);
		blockDisplay.customName(mLabel);
		blockDisplay.addScoreboardTag(TAG);
		mBlockDisplay = blockDisplay;

		Location textLocation = mBottom.clone();
		textLocation.subtract(halfWidth + 0.001, 0.0, 0.0);
		TextDisplay textDisplay = textLocation.getWorld().spawn(textLocation, TextDisplay.class);
		textDisplay.text(mLabel);
		DisplayEntityUtils.setTextDisplayBackgroundColor(textDisplay,
			Color.fromARGB(0x00, 0x3f, 0x3f, 0x3f));

		// Exactly center/fill the shape with text without exceeding its bounds
		int entityMaxLineWidth = textDisplay.getLineWidth();
		int totalLines = 0;
		int maxLineWidth = 0;
		for (String line : MessagingUtils.plainText(mLabel).split("\\n")) {
			totalLines += 1;
			int lineWidth = line.length();
			if (lineWidth >= entityMaxLineWidth) {
				maxLineWidth = entityMaxLineWidth;
				totalLines += Math.floorDiv(lineWidth, entityMaxLineWidth);
			} else {
				maxLineWidth = Integer.max(maxLineWidth, lineWidth);
			}
		}
		totalLines = Integer.max(totalLines, 1);
		maxLineWidth = Integer.max(maxLineWidth, 1);
		float textWidth = TEXT_DISPLAY_CHAR_WIDTH * maxLineWidth + TEXT_DISPLAY_MIN_WIDTH;
		float textHeight = TEXT_DISPLAY_CHAR_HEIGHT * totalLines + TEXT_DISPLAY_MIN_HEIGHT;
		float textScale = Math.min(width / textWidth, height / textHeight);
		//textWidth *= textScale; // Unused past this point, display entity is always centered (plus a small offset)
		textHeight *= textScale;
		setDisplayEntityScale(textDisplay, new Vector(textScale, textScale, textScale));
		setDisplayEntityTranslation(textDisplay, new Vector(-0.0125f, 0.5f * (height - textHeight), 0.0f));

		textDisplay.customName(mLabel);
		textDisplay.addScoreboardTag(TAG);
		textDisplay.setRotation(90.0f, 0.0f);
		mTextDisplay = textDisplay;
	}

	public Location getBottomPos() {
		return mBottom.clone();
	}

	public Location getTopPos() {
		return mBottom.clone().add(new Vector(0.0, mSize.getY(), 0.0));
	}

	public Component getLabel() {
		return mLabel;
	}

	public UUID getInteractionUuid() {
		return mInteraction.getUniqueId();
	}

	public Set<UUID> getEntityUuids() {
		return Set.of(
			mInteraction.getUniqueId(),
			mBlockDisplay.getUniqueId(),
			mTextDisplay.getUniqueId()
		);
	}

	// Discard object after use
	public void killEntities() {
		if (mInteraction.isValid()) {
			mInteraction.remove();
		}
		if (mBlockDisplay.isValid()) {
			mBlockDisplay.remove();
		}
		if (mTextDisplay.isValid()) {
			mTextDisplay.remove();
		}
	}

	public void setBottomPos(Location pos) {
		mBottom = pos.clone();

		float width = (float) mSize.getX();
		float halfWidth = width * 0.5f;
		float height = (float) mSize.getY();

		mInteraction.teleport(mBottom);
		mInteraction.setInteractionWidth(width);
		mInteraction.setInteractionHeight(height);

		Location blockDisplayLocation = mBottom.clone().subtract(halfWidth, 0.0, halfWidth);
		mBlockDisplay.teleport(blockDisplayLocation);
		setDisplayEntityScale(mBlockDisplay, mSize);

		Location textLocation = mBottom.clone();
		textLocation.subtract(halfWidth + 0.001, 0.0, 0.0);
		textLocation.setYaw(90.0f);
		textLocation.setPitch(0.0f);
		mTextDisplay.teleport(textLocation);
	}

	public BlockData getBlockData() {
		return mBlockDisplay.getBlock();
	}

	public void setBlockData(BlockData blockData) {
		mBlockDisplay.setBlock(blockData);
	}

	private void setDisplayEntityScale(Display display, Vector size) {
		Transformation transformation = display.getTransformation();
		transformation = new Transformation(transformation.getTranslation(),
			transformation.getLeftRotation(),
			new Vector3f((float) size.getX(), (float) size.getY(), (float) size.getZ()),
			transformation.getRightRotation());
		display.setTransformation(transformation);
	}

	private void setDisplayEntityTranslation(Display display, Vector translation) {
		Transformation transformation = display.getTransformation();
		Vector3f translation3f = new Vector3f((float) translation.getX(),
			(float) translation.getY(),
			(float) translation.getZ());
		transformation = new Transformation(translation3f,
			transformation.getLeftRotation(),
			transformation.getScale(),
			transformation.getRightRotation());
		display.setTransformation(transformation);
	}
}
