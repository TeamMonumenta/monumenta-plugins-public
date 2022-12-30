package com.playmonumenta.plugins.gallery.interactables;

import com.playmonumenta.plugins.gallery.GalleryUtils;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MysteryBoxInteractable extends BasePricedInteractable {
	public static final String TAG_STRING = "GalleryChest";

	private boolean mValidBox = true;
	//command that should be used when a Mbox is moved from this location
	private final @Nullable String mCommandRemove;
	//command that should be used when a Mbox is moved to this location
	private final @Nullable String mCommandPlace;

	public MysteryBoxInteractable(
		@NotNull String name,
		@NotNull Entity entity,
		@NotNull Integer price,
		@Nullable String command,
		@Nullable String commandRemove,
		@Nullable String commandPlace) {
		super(name, entity, price, "Mystery Box", command);
		mCommandRemove = commandRemove;
		mCommandPlace = commandPlace;
	}

	public void setValidBox(boolean bool) {
		mValidBox = bool;
		save();
	}

	public boolean getValid() {
		return mValidBox;
	}

	public void runCommandRemove() {
		if (mCommandRemove != null) {
			GalleryUtils.runCommandAsEntity(mArmorStand, mCommandRemove);
		}

		removeSlime();
	}

	public void runCommandPlace() {
		if (mCommandPlace != null) {
			GalleryUtils.runCommandAsEntity(mArmorStand, mCommandPlace);
		}

		summonSlime();
	}

	@Override public boolean isInteractable() {
		return mValidBox && super.isInteractable();
	}

	@Override
	public void showMessage() {
		if (mValidBox) {
			mShowingText = "Pay " + mPrice + " to obtain a random item";
		} else {
			mShowingText = "The mystery box is not here";
		}
		super.showMessage();
	}

	@Override
	public void save() {
		new HashSet<>(mArmorStand.getScoreboardTags()).forEach(s -> {
			if (s.startsWith(TAG_STRING + "-")) {
				mArmorStand.removeScoreboardTag(s);
			}
		});
		//MMBOX-name-A/D-0000-func1 art-func2 remove-func3 place
		mArmorStand.addScoreboardTag(BaseInteractable.TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING);
		mArmorStand.addScoreboardTag(TAG_STRING + "-" + mName + "-" + (mValidBox ? "Active" : "Deactivated") + "-" + mPrice + "-" + (mCommand != null ? mCommand : "") + "-" + (mCommandRemove != null ? mCommandRemove : "") + "-" + (mCommandPlace != null ? mCommandPlace : ""));
	}

	private void summonSlime() {
		Location loc = mArmorStand.getLocation().clone().add(0, -0.5, 0);
		Entity entity = loc.getWorld().spawnEntity(loc, EntityType.SLIME);
		if (entity instanceof Slime slime) {
			slime.setSize(2);
			slime.setAI(false);
			slime.setCollidable(false);
			slime.setGlowing(true);
			slime.setInvulnerable(true);
			slime.setInvisible(true);
			slime.setRemoveWhenFarAway(false);
			slime.addScoreboardTag(AbilityUtils.IGNORE_TAG);
			slime.addScoreboardTag("UNPUSHABLE");
			slime.addScoreboardTag("MMBoxSlime");
			ScoreboardUtils.addEntityToTeam(slime, "Aqua");
		}
	}

	private void removeSlime() {
		Location loc = mArmorStand.getLocation().clone().add(0, -0.5, 0);
		Collection<Entity> entities = loc.getNearbyEntitiesByType(Slime.class, 1);
		//just in case we are removing all slime with that tag in a block radius
		for (Entity entity : entities) {
			if (entity instanceof Slime slime && slime.getScoreboardTags().contains("MMBoxSlime")) {
				slime.remove();
			}
		}
	}

	//MMBOX-name-A/D-0000-lootTable-func1 art-func2 remove-func3 place
	public static MysteryBoxInteractable fromEntity(Entity entity) throws RuntimeException {

		Set<String> tags = new HashSet<>(entity.getScoreboardTags());

		try {
			for (String tag : tags) {
				if (tag.startsWith(TAG_STRING + "-")) {
					String[] split = tag.split("-");
					String name = split[1];
					boolean inThisLocation = "Active".equals(split[2]);
					Integer price = Integer.valueOf(split[3]);
					String func1 = split[4];
					String func2 = split[5];
					String func3 = split[6];
					MysteryBoxInteractable mBoxInt = new MysteryBoxInteractable(name, entity, price, func1, func2, func3);
					mBoxInt.setValidBox(inThisLocation);
					return mBoxInt;
				}
			}
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Catch an exception while converting MysteryBoxEntity. Reason: " + e.getMessage());
			e.printStackTrace();
		}
		throw new RuntimeException("Can't Load a MysteryBoxInteractable");
	}
}
