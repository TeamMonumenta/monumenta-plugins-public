package com.playmonumenta.plugins.managers.travelanchor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Shulker;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.managers.travelanchor.TravelAnchorManager.TRAVEL_ANCHOR_PDC_KEY;
import static com.playmonumenta.plugins.managers.travelanchor.TravelAnchorManager.TRAVEL_ANCHOR_TAG;

public class EntityTravelAnchor implements Comparable<EntityTravelAnchor> {
	private static final double SHULKER_HEIGHT = 1.0;
	private static final TextColor DEFAULT_COLOR = TextColor.color(151, 104, 151);

	private final UUID mEntityId;
	private String mLabel;
	private TextColor mColor;
	private final Set<UUID> mGroups = new HashSet<>();
	private UUID mLastWorld;
	private Vector mLastLoc;

	public static boolean isTravelAnchor(Entity entity) {
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		return pdc.has(TRAVEL_ANCHOR_PDC_KEY);
	}

	public static EntityTravelAnchor newAnchor(Entity entity) {
		return new EntityTravelAnchor(entity);
	}

	public static EntityTravelAnchor loadAnchor(Entity entity) {
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		String anchorJson = pdc.get(TRAVEL_ANCHOR_PDC_KEY, PersistentDataType.STRING);
		Gson gson = new Gson();
		JsonObject data = gson.fromJson(anchorJson, JsonObject.class);
		return new EntityTravelAnchor(data);
	}

	public static void removeAnchor(Entity entity) {
		if (!entity.isValid()) {
			return;
		}
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		pdc.remove(TRAVEL_ANCHOR_PDC_KEY);
		entity.removeScoreboardTag(TRAVEL_ANCHOR_TAG);
	}

	// Internal method to get the travel anchor structure; must be saved to the world to apply
	private EntityTravelAnchor(Entity entity) {
		entity.addScoreboardTag(TRAVEL_ANCHOR_TAG);

		mEntityId = entity.getUniqueId();
		mLastWorld = entity.getWorld().getUID();
		mLastLoc = entity.getLocation().toVector().clone();
		if (entity instanceof Shulker shulker) {
			mLastLoc.add(new Vector(0.0, SHULKER_HEIGHT, 0.0));
			DyeColor dyeColor = shulker.getColor();
			if (dyeColor == null) {
				mColor = DEFAULT_COLOR;
			} else {
				mColor = TextColor.color(dyeColor.getColor().asRGB());
			}
		} else {
			mColor = DEFAULT_COLOR;
		}

		Component customName = entity.customName();
		if (customName != null) {
			mLabel = MessagingUtils.plainText(customName);
		} else {
			mLabel = entity.getType().key().toString();
		}
	}

	public EntityTravelAnchor(JsonObject data) {
		if (data.get("mEntityId") instanceof JsonPrimitive entityIdPrimitive && entityIdPrimitive.isString()) {
			mEntityId = UUID.fromString(entityIdPrimitive.getAsString());
		} else {
			throw new RuntimeException("Invalid EntityTravelAnchor mEntityId");
		}

		if (data.get("mLabel") instanceof JsonPrimitive labelPrimitive && labelPrimitive.isString()) {
			mLabel = labelPrimitive.getAsString();
		} else {
			throw new RuntimeException("Invalid EntityTravelAnchor mLastWorld");
		}

		if (data.get("mColor") instanceof JsonPrimitive colorPrimitive && colorPrimitive.isString()) {
			TextColor textColor = TextColor.fromHexString(colorPrimitive.getAsString());
			if (textColor == null) {
				mColor = DEFAULT_COLOR;
			} else {
				mColor = textColor;
			}
		} else {
			mColor = DEFAULT_COLOR;
		}

		if (data.get("mGroups") instanceof JsonArray groupsJsonArray) {
			for (JsonElement groupJsonElement : groupsJsonArray) {
				if (groupJsonElement instanceof JsonPrimitive groupJsonPrimitive && groupJsonPrimitive.isString()) {
					try {
						mGroups.add(UUID.fromString(groupJsonPrimitive.getAsString()));
					} catch (Exception ignored) {
						// This shouldn't happen, silently ignore it
					}
				}
			}
		}

		if (data.get("mLastWorld") instanceof JsonPrimitive worldIdPrimitive && worldIdPrimitive.isString()) {
			mLastWorld = UUID.fromString(worldIdPrimitive.getAsString());
		} else {
			throw new RuntimeException("Invalid EntityTravelAnchor mLastWorld");
		}

		List<Double> lastLocList = new ArrayList<>();
		if (data.get("mLastLoc") instanceof JsonArray lastLocArr) {
			for (JsonElement lastLocElem : lastLocArr) {
				if (lastLocElem instanceof JsonPrimitive lastLocPrim && lastLocPrim.isNumber()) {
					lastLocList.add(lastLocPrim.getAsDouble());
				} else {
					throw new RuntimeException("Invalid EntityTravelAnchor mLastLoc[]");
				}
			}
		} else {
			throw new RuntimeException("Invalid EntityTravelAnchor mLastLoc");
		}
		if (lastLocList.size() != 3) {
			throw new RuntimeException("Invalid EntityTravelAnchor mLastLoc size");
		}
		mLastLoc = new Vector(lastLocList.get(0), lastLocList.get(1), lastLocList.get(2));
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();

		result.addProperty("mEntityId", mEntityId.toString());
		result.addProperty("mLastWorld", mLastWorld.toString());

		JsonArray lastLoc = new JsonArray();
		lastLoc.add(mLastLoc.getX());
		lastLoc.add(mLastLoc.getY());
		lastLoc.add(mLastLoc.getZ());
		result.add("mLastLoc", lastLoc);

		JsonArray groupArray = new JsonArray();
		for (UUID groupId : groupIds()) {
			groupArray.add(groupId.toString());
		}
		result.add("mGroups", groupArray);

		result.addProperty("mLabel", mLabel);
		result.addProperty("mColor", mColor.asHexString());

		return result;
	}

	public UUID getEntityId() {
		return mEntityId;
	}

	public @Nullable Entity getEntity() {
		return Bukkit.getEntity(mEntityId);
	}

	public String label() {
		return mLabel;
	}

	public void label(String value) {
		mLabel = value;
	}

	public TextColor color() {
		return mColor;
	}

	public void pruneDeletedGroups() {
		World world = lastWorld();
		if (world == null) {
			return;
		}

		WorldAnchorGroups worldAnchorGroups = TravelAnchorManager.getInstance()
			.anchorsInWorld(world)
			.getAnchorGroups();

		mGroups.removeIf(groupId -> worldAnchorGroups.anchorGroup(groupId) == null);

		if (mGroups.isEmpty()) {
			mGroups.add(WorldAnchorGroups.DEFAULT_GROUP_UUID);
		}
	}

	protected Set<UUID> groupIds() {
		if (mGroups.isEmpty()) {
			mGroups.add(WorldAnchorGroups.DEFAULT_GROUP_UUID);
		}
		return mGroups;
	}

	public Set<AnchorGroup> groups() {
		Set<AnchorGroup> groups = new TreeSet<>();

		World world = lastWorld();
		if (world == null) {
			return groups;
		}

		WorldAnchorGroups worldAnchorGroups = TravelAnchorManager.getInstance()
			.anchorsInWorld(world)
			.getAnchorGroups();

		for (UUID groupId : groupIds()) {
			AnchorGroup anchorGroup = worldAnchorGroups.anchorGroup(groupId);
			if (anchorGroup != null) {
				groups.add(anchorGroup);
			}
		}

		return groups;
	}

	public Set<AnchorGroup> commonGroups(EntityTravelAnchor other) {
		Set<AnchorGroup> groups = new TreeSet<>();

		World world = lastWorld();
		if (world == null || !world.equals(other.lastWorld())) {
			return groups;
		}

		WorldAnchorGroups worldAnchorGroups = TravelAnchorManager.getInstance()
			.anchorsInWorld(world)
			.getAnchorGroups();

		Set<UUID> commonGroupIds = new HashSet<>(groupIds());
		commonGroupIds.retainAll(other.groupIds());

		for (UUID groupId : commonGroupIds) {
			AnchorGroup anchorGroup = worldAnchorGroups.anchorGroup(groupId);
			if (anchorGroup != null) {
				groups.add(anchorGroup);
			}
		}

		return groups;
	}

	public boolean cannotAccess(EntityTravelAnchor other) {
		return commonGroups(other).isEmpty();
	}

	public boolean inGroup(AnchorGroup group) {
		return groupIds().contains(group.id());
	}

	public void addToGroup(AnchorGroup group) {
		mGroups.add(group.id());
	}

	public boolean removeFromGroup(AnchorGroup group) {
		UUID groupId = group.id();
		if (groupIds().contains(groupId) && mGroups.size() <= 1) {
			return false;
		}
		mGroups.remove(groupId);
		return true;
	}

	public @Nullable World lastWorld() {
		return Bukkit.getWorld(mLastWorld);
	}

	public Vector lastPos() {
		return mLastLoc;
	}

	public @Nullable Location lastLocation() {
		World world = Bukkit.getWorld(mLastWorld);
		if (world == null) {
			return null;
		}
		return new Location(world, mLastLoc.getX(), mLastLoc.getY(), mLastLoc.getZ());
	}

	public void update() {
		Entity entity = getEntity();
		if (entity == null) {
			return;
		}

		mLastWorld = entity.getWorld().getUID();
		mLastLoc = entity.getLocation().toVector();
		if (entity instanceof Shulker shulker) {
			shulker.setPeek(0.0f);
			mLastLoc.add(new Vector(0.0, SHULKER_HEIGHT, 0.0));

			DyeColor dyeColor = shulker.getColor();
			if (dyeColor == null) {
				mColor = DEFAULT_COLOR;
			} else {
				mColor = TextColor.color(dyeColor.getColor().asRGB());
			}
		} else {
			mColor = DEFAULT_COLOR;
		}

		pruneDeletedGroups();

		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		pdc.set(TRAVEL_ANCHOR_PDC_KEY, PersistentDataType.STRING, toJson().toString());
	}

	@Override
	public int compareTo(@NotNull EntityTravelAnchor o) {
		return mEntityId.compareTo(o.mEntityId);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EntityTravelAnchor other)) {
			return false;
		}
		return mEntityId.equals(other.mEntityId);
	}

	@Override
	public int hashCode() {
		return mEntityId.hashCode();
	}
}
