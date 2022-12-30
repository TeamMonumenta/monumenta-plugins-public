package com.playmonumenta.plugins.infinitytower;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobAbility;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TowerMob {

	public final TowerMobInfo mInfo;

	public int mMobLevel = 1;

	public final List<TowerMobAbility> mAbilities = new ArrayList<>();


	private double mDx;
	private double mDy;
	private double mDz;

	public TowerMob(TowerMobInfo info, double dx, double dy, double dz) {
		mInfo = info;

		for (String ability : info.mAbilities) {
			mAbilities.add(TowerMobAbility.fromString(ability));
		}

		mDx = dx;
		mDy = dy;
		mDz = dz;
	}



	public ItemStack buildTeamItem(TowerGame game) {
		ItemStack stack = new ItemStack(mInfo.mBaseItem);
		ItemMeta meta = stack.getItemMeta();

		meta.displayName(Component.text(mInfo.mDisplayName, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> list = new ArrayList<>();

		list.add(TowerGameUtils.getRarityComponent(mInfo.mMobRarity));
		list.add(TowerGameUtils.getClassComponent(mInfo.mMobClass));
		list.addAll(TowerGameUtils.getAbilityComponent(mInfo.mAbilities));
		list.add(TowerGameUtils.getLevelComponent(mMobLevel));

		list.add(Component.empty());
		list.add(TowerGameUtils.getWeightComponent(mInfo.mMobStats.mWeight));

		Location spawnLocation = getSpawnLocation(game);
		Location playerLoc = game.mPlayer.mPlayer.getLocation();
		int x = (int) (spawnLocation.getX() - playerLoc.getX());
		int y = (int) (spawnLocation.getY() - playerLoc.getY());
		int z = (int) (spawnLocation.getZ() - playerLoc.getZ());
		String locToString = "pos: x " + x + " y " + y + " z " + z;
		list.addAll(TowerGameUtils.getGenericLoreComponent(locToString));
		meta.lore(list);
		stack.setItemMeta(meta);

		return stack;
	}

	public ItemStack buildAtkItem(TowerGame game) {
		return TowerMobInfo.buildAtkItem(mInfo);

	}

	public ItemStack buildHPItem(TowerGame game) {
		return TowerMobInfo.buildHPItem(mInfo);
	}

	public @Nullable ItemStack buildSpellItem(TowerGame game, int i) {
		return TowerMobInfo.buildSpellItem(mInfo, i);
	}

	public ItemStack buildLevelItem(TowerGame game) {
		ItemStack stack = new ItemStack(Material.BIRCH_SIGN);
		ItemMeta meta = stack.getItemMeta();
		meta.displayName(TowerGameUtils.getLevelComponent(mMobLevel));

		List<Component> list = new ArrayList<>();
		int damageMult = (int) ((mInfo.mMobRarity.getDamageMult() * (mMobLevel - 1) * 100)/ 100 * 100);
		int lvlDamageMult = (int) (mInfo.mMobRarity.getDamageMult() * 100);

		if (mMobLevel < 5) {
			list.add(Component.text("This mob receives " + damageMult + "% ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
				         .append(Component.text("(+" + lvlDamageMult + "%)", NamedTextColor.DARK_PURPLE))
				         .append(Component.text(" less damage", NamedTextColor.DARK_GRAY)));
			list.add(Component.text("This mob does " + damageMult + "% ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
						.append(Component.text("(+" + lvlDamageMult + "%)", NamedTextColor.DARK_PURPLE))
						.append(Component.text(" more damage", NamedTextColor.DARK_GRAY)));
			list.add(Component.empty());
			int nextLvlCost = TowerGameUtils.getNextLevelCost(this);
			list.add(Component.text("Pay " + nextLvlCost + " to buy an upgrade!", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
		} else {
			list.addAll(TowerGameUtils.getGenericLoreComponent("This mob receives " + damageMult + "% less damage"));
			list.addAll(TowerGameUtils.getGenericLoreComponent("This mob does " + damageMult + "% more damage"));
		}
		meta.lore(list);
		stack.setItemMeta(meta);
		return stack;
	}

	public ItemStack buildClassItem(TowerGame game) {
		return TowerMobInfo.buildClassItem(mInfo);
	}


	public ItemStack buildItem() {
		ItemStack stack = new ItemStack(mInfo.mBaseItem);
		ItemMeta meta = stack.getItemMeta();

		meta.displayName(Component.text(mInfo.mDisplayName, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> list = new ArrayList<>();

		list.add(TowerGameUtils.getLevelComponent(mMobLevel));
		list.add(TowerGameUtils.getRarityComponent(mInfo.mMobRarity));
		list.add(TowerGameUtils.getClassComponent(mInfo.mMobClass));
		list.add(Component.empty());
		list.addAll(TowerGameUtils.getAbilityComponent(mInfo.mAbilities));
		meta.lore(list);
		stack.setItemMeta(meta);
		return stack;
	}

	public boolean isSameBaseMob(TowerMob mob) {
		return this.mInfo.mLosName.equals(mob.mInfo.mLosName);
	}

	public boolean isSameBaseMob(TowerMobInfo info) {
		return this.mInfo.mLosName.equals(info.mLosName);
	}

	public Location getSpawnLocation(TowerGame game) {
		TowerFloor floor = Objects.requireNonNull(game.mFloor);
		double dx = Math.min(floor.mXSize - 0.5, mDx);
		double dz = Math.min(floor.mZSize - 0.5, mDz);
		return new Location(game.mPlayer.mPlayer.getWorld(), floor.mVector.getX() + dx, floor.mVector.getY() + mDy, floor.mVector.getZ() + dz);
	}

	protected void setLocation(double x, double y, double z) {
		mDx = x;
		mDy = y;
		mDz = z;
	}

	public double getX() {
		return mDx;
	}

	public double getY() {
		return mDy;
	}

	public double getZ() {
		return mDz;
	}


	//--------------------------spawn mob-----------------------

	public @Nullable LivingEntity spawn(TowerGame game, boolean spawnedByPlayer) {
		return spawnAtLocation(game, spawnedByPlayer, getSpawnLocation(game));
	}

	public @Nullable LivingEntity spawnAtLocation(TowerGame game, boolean spawnedByPlayer, Location loc) {
		if (spawnedByPlayer) {
			loc.setDirection(new Vector(+1, 0, 0));
		} else {
			loc.setDirection(new Vector(-1, 0, 0));
		}

		LivingEntity mobspawned = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, mInfo.mLosName);

		if (mobspawned == null) {
			TowerFileUtils.warning("WARNING! ------------ TowerMob = null | name = " + mInfo.mLosName);
			game.forceStopGame();
			return null;
		}

		mobspawned.teleport(loc);

		mobspawned.customName(TowerGameUtils.getMobNameComponent(this, spawnedByPlayer));
		mobspawned.setCustomNameVisible(true);
		mobspawned.addScoreboardTag(TowerConstants.MOB_TAG);
		mobspawned.addScoreboardTag(mInfo.mMobRarity.getTag() + "_" + mMobLevel);
		mobspawned.addScoreboardTag(mInfo.mMobClass.getTag());


		for (TowerMobAbility ability : mAbilities) {
			ability.applyAbility(this, mobspawned, game, spawnedByPlayer);
		}

		if (spawnedByPlayer) {
			mobspawned.addScoreboardTag(TowerConstants.MOB_TAG_PLAYER_TEAM);
		} else {
			mobspawned.addScoreboardTag(TowerConstants.MOB_TAG_FLOOR_TEAM);
		}

		return mobspawned;
	}

	public void spawnPuppet(TowerGame game, List<LivingEntity> list) {
		spawnPuppet(game, list, false);
	}

	public void spawnPuppet(TowerGame game, List<LivingEntity> list, boolean playerMob) {
		Location loc1 = getSpawnLocation(game);
		if (playerMob) {
			loc1.setDirection(new Vector(+1, 0, 0));
		} else {
			loc1.setDirection(new Vector(-1, 0, 0));
		}

		LivingEntity mobspawned = (LivingEntity) LibraryOfSoulsIntegration.summon(loc1, mInfo.mLosName);
		if (mobspawned == null) {
			TowerFileUtils.warning("WARNING! ------------ TowerMob = null | name = " + mInfo.mLosName);
			game.forceStopGame();
			return;
		}
		mobspawned.teleport(loc1);

		mobspawned.customName(Component.empty());
		mobspawned.setCustomNameVisible(false);
		mobspawned.setAI(false);
		mobspawned.setCollidable(false);
		mobspawned.setInvulnerable(true);
		mobspawned.setSilent(true);
		mobspawned.setGravity(false);
		mobspawned.addScoreboardTag(TowerConstants.TAG_UNLOAD_ENTITY);

		LivingEntity armorClass = mobspawned.getWorld().spawn(mobspawned.getLocation().clone().add(0, 0.25, 0), ArmorStand.class);
		armorClass.setCustomNameVisible(true);
		armorClass.customName(Component.text("Class " + mInfo.mMobClass.getName(), NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		armorClass.setInvisible(true);
		armorClass.setGravity(false);
		armorClass.addScoreboardTag(TowerConstants.TAG_UNLOAD_ENTITY);

		Location loc = armorClass.getLocation().clone().add(0, 0.25, 0);
		LivingEntity armorLvl = loc.getWorld().spawn(loc, ArmorStand.class);
		armorLvl.setInvisible(true);
		armorLvl.setGravity(false);
		armorLvl.customName(Component.text("Level " + mMobLevel, NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		armorLvl.setCustomNameVisible(true);
		armorLvl.addScoreboardTag(TowerConstants.TAG_UNLOAD_ENTITY);

		Location loc2 = loc.clone().add(0, 0.25, 0);
		LivingEntity armorName = loc2.getWorld().spawn(loc2, ArmorStand.class);
		armorName.setInvisible(true);
		armorName.setGravity(false);
		armorName.customName(Component.text(mInfo.mDisplayName, NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
		armorName.setCustomNameVisible(true);
		armorName.addScoreboardTag(TowerConstants.TAG_UNLOAD_ENTITY);

		if (list != null) {
			list.add(mobspawned);
			list.add(armorClass);
			list.add(armorLvl);
			list.add(armorName);
		}
	}

	//------------------------JSON---------------------------------------

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("LoSName", mInfo.mLosName);
		object.addProperty("dx", mDx);
		object.addProperty("dy", mDy);
		object.addProperty("dz", mDz);
		object.addProperty("level", mMobLevel);

		return object;
	}

	public static @Nullable TowerMob fromJson(JsonObject object) {
		String losName = object.getAsJsonPrimitive("LoSName").getAsString();
		TowerMobInfo info = TowerFileUtils.getMobInfo(losName);
		if (info == null) {
			return null;
		}
		double dx = object.getAsJsonPrimitive("dx").getAsDouble();
		double dy = object.getAsJsonPrimitive("dy").getAsDouble();
		double dz = object.getAsJsonPrimitive("dz").getAsDouble();
		int level = object.getAsJsonPrimitive("level").getAsInt();
		TowerMob mob = new TowerMob(info, dx, dy, dz);
		mob.mMobLevel = level;
		return mob;
	}


}
