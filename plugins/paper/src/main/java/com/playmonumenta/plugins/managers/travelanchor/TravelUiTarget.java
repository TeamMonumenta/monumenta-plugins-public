package com.playmonumenta.plugins.managers.travelanchor;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class TravelUiTarget {
	private static final float DISTANCE = 0.75f;
	private static final float SCALE = 0.1f;

	private final EntityTravelAnchor mAnchor;
	private final Vector mDir;
	private final TextDisplay mLabelEntity;

	protected TravelUiTarget(Location eyeLoc, EntityTravelAnchor anchor, Player player) {
		mAnchor = anchor;
		String label = anchor.label();
		TextColor textColor = anchor.color();

		mDir = anchor.lastPos().clone().subtract(eyeLoc.toVector()).normalize();
		Vector deltaPos = mDir.clone().multiply(DISTANCE);
		Location loc = eyeLoc.clone().add(deltaPos).setDirection(mDir);
		World world = loc.getWorld();

		mLabelEntity = world.spawn(loc, TextDisplay.class, textDisplay -> {
			Transformation transformation = textDisplay.getTransformation();

			textDisplay.setBillboard(Display.Billboard.CENTER);
			textDisplay.text(Component.text(label, textColor));
			textDisplay.setTransformation(new Transformation(
				transformation.getTranslation(),
				transformation.getLeftRotation(),
				new Vector3f(SCALE, SCALE, SCALE),
				transformation.getRightRotation()
			));
			textDisplay.addScoreboardTag("TravelAnchorUiLabel");
			textDisplay.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
			textDisplay.setVisibleByDefault(false);
		});
		player.showEntity(Plugin.getInstance(), mLabelEntity);
	}

	public Vector getDir() {
		return mDir;
	}

	public EntityTravelAnchor getAnchor() {
		return mAnchor;
	}

	public TextDisplay getLabel() {
		return mLabelEntity;
	}

	public Set<UUID> getEntityIds() {
		Set<UUID> result = new HashSet<>();
		result.add(mAnchor.getEntityId());
		result.add(mLabelEntity.getUniqueId());
		return result;
	}

	public void highlight(boolean value, int phase) {
		String label = mAnchor.label();
		if (!value) {
			TextColor textColor = mAnchor.color();
			mLabelEntity.text(Component.text(label, textColor));
			return;
		}

		char[] charArray = label.toCharArray();
		List<Component> charComponents = new ArrayList<>();
		for (int i = 0; i < charArray.length; i++) {
			char c = charArray[i];
			float h = (((float) i / (float) charArray.length) + ((float) phase / 360.0f)) % 1.0f;
			charComponents.add(Component.text(c, TextColor.color(HSVLike.hsvLike(h, 1.0f, 1.0f))));
		}
		mLabelEntity.text(Component.join(JoinConfiguration.noSeparators(), charComponents)
			.decoration(TextDecoration.BOLD, true));
	}

	public boolean shouldRemove() {
		return mAnchor.getEntity() == null;
	}

	public void remove() {
		mLabelEntity.remove();
	}
}
