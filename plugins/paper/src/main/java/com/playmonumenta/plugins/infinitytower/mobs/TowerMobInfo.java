package com.playmonumenta.plugins.infinitytower.mobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class TowerMobInfo {

	public final String mLosName;
	//reference to /los get <mob_name>
	public final TowerMobStats mMobStats;

	public final List<String> mAbilities = new ArrayList<>();


	//there are only used for the item
	public @Nullable String mDisplayName;

	public @Nullable String mLore;

	public @Nullable ItemStack mBaseItem;


	public TowerMobRarity mMobRarity = TowerMobRarity.COMMON;
	//base on the leve the player will be able to see more common/rare/epic mobs when buy a new one

	public TowerMobClass mMobClass = TowerMobClass.PROTECTOR;

	public boolean mBuyable = true;

	public TowerMobInfo(String losName, TowerMobStats stats) {
		mLosName = losName;
		mMobStats = stats;
	}


	public ItemStack getBuyableItem() {
		ItemStack stack = new ItemStack(mBaseItem);

		ItemMeta meta = stack.getItemMeta();

		meta.displayName(Component.text(mDisplayName, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> list = new ArrayList<>();

		list.addAll(TowerGameUtils.getGenericLoreComponent(mLore));

		list.add(TowerGameUtils.getRarityComponent(mMobRarity));
		list.add(TowerGameUtils.getClassComponent(mMobClass));
		list.add(TowerGameUtils.getWeightComponent(mMobStats.mWeight));
		list.add(TowerGameUtils.getHpComponent(mMobStats.mHP));
		list.add(TowerGameUtils.getAtkComponent(mMobStats.mAtk));
		list.addAll(TowerGameUtils.getAbilityComponent(mAbilities));
		list.add(Component.empty());
		list.add(TowerGameUtils.getPriceComponent(mMobStats.mCost));
		list.add(TowerGameUtils.getLimitComponent(mMobStats.mLimit));

		meta.lore(list);

		stack.setItemMeta(meta);

		return stack;
	}

	public void stampStats(Player player) {
		TowerGameUtils.sendMessage(player, "Mob stats for mob: " + ChatColor.WHITE + mLosName);

		player.sendMessage(TowerGameUtils.getRarityComponent(mMobRarity));
		player.sendMessage(TowerGameUtils.getWeightComponent(mMobStats.mWeight));
		player.sendMessage(TowerGameUtils.getAtkComponent(mMobStats.mAtk));
		player.sendMessage(TowerGameUtils.getHpComponent(mMobStats.mHP));
		player.sendMessage(Component.empty());
		player.sendMessage(TowerGameUtils.getPriceComponent(mMobStats.mCost));
		player.sendMessage(TowerGameUtils.getLimitComponent(mMobStats.mLimit));

	}




	public JsonObject toJson() {
		JsonObject obj = new JsonObject();

		obj.addProperty("LosName", mLosName);
		obj.add("BaseStats", mMobStats.toJson());
		obj.addProperty("DisplayName", mDisplayName);
		obj.addProperty("Lore", mLore);
		if (mBaseItem != null) {
			obj.addProperty("ItemMaterial", mBaseItem.getType().name());
			if (mBaseItem.getType() == Material.PLAYER_HEAD) {
				obj.addProperty("HeadTexture", TowerFileUtils.getHeadTexture(mBaseItem));
			}
		}

		obj.addProperty("Rarity", mMobRarity.getName());
		obj.addProperty("Class", mMobClass.getName());

		obj.addProperty("Buyable", mBuyable);

		JsonArray arr = new JsonArray();
		for (String ability : mAbilities) {
			arr.add(ability);
		}

		obj.add("abilities", arr);

		return obj;
	}

	public static @Nullable TowerMobInfo fromJson(JsonObject obj) {
		if (obj == null) {
			return null;
		}

		try {
			String los = obj.getAsJsonPrimitive("LosName").getAsString();

			TowerMobStats mobStats = TowerMobStats.fromJson(obj.getAsJsonObject("BaseStats"));

			TowerMobInfo item = new TowerMobInfo(los, mobStats);

			Material mat = Material.getMaterial(obj.getAsJsonPrimitive("ItemMaterial").getAsString());

			if (mat == Material.PLAYER_HEAD) {
				item.mBaseItem = TowerFileUtils.getHeadFromTexture(obj.getAsJsonPrimitive("HeadTexture").getAsString());
			} else {
				item.mBaseItem = new ItemStack(mat);
			}

			if (obj.getAsJsonPrimitive("Lore") != null) {
				item.mLore = obj.getAsJsonPrimitive("Lore").getAsString();
			}

			if (obj.getAsJsonPrimitive("DisplayName") != null) {
				item.mDisplayName = obj.getAsJsonPrimitive("DisplayName").getAsString();
			}

			if (obj.getAsJsonPrimitive("Rarity") != null) {
				item.mMobRarity = TowerMobRarity.fromName(obj.getAsJsonPrimitive("Rarity").getAsString());
			}

			if (obj.getAsJsonPrimitive("Class") != null) {
				item.mMobClass = TowerMobClass.getFromName(obj.getAsJsonPrimitive("Class").getAsString());
			}

			if (obj.getAsJsonPrimitive("Buyable") != null) {
				item.mBuyable = obj.getAsJsonPrimitive("Buyable").getAsBoolean();
			}

			if (obj.getAsJsonArray("abilities") != null) {
				JsonArray arr = obj.getAsJsonArray("abilities");
				for (int i = 0; i < arr.size(); i++) {
					item.mAbilities.add(arr.get(i).getAsString());
				}
			}

			return item;
		} catch (Exception e) {
			TowerFileUtils.warning("Catch an exception during parsing a mob");
			TowerConstants.SHOULD_GAME_START = false;
			e.printStackTrace();
			return null;
		}

	}


	public static ItemStack buildClassItem(TowerMobInfo info) {
		ItemStack stack;
		List<Component> list = new ArrayList<>();
		if (info.mMobClass == TowerMobClass.CASTER) {
			stack = new ItemStack(Material.STICK);
			ItemMeta meta = stack.getItemMeta();
			meta.displayName(TowerGameUtils.getClassComponent(info.mMobClass));
			list.addAll(TowerGameUtils.getGenericLoreComponent("This unit deals " + (TowerConstants.DAMAGE_MLT_CLASS * 100 - 100) + "% more damage to Protectors"));
			meta.lore(list);
			stack.setItemMeta(meta);
		} else if (info.mMobClass == TowerMobClass.PROTECTOR) {
			stack = new ItemStack(Material.SHIELD);
			ItemMeta meta = stack.getItemMeta();
			meta.displayName(TowerGameUtils.getClassComponent(info.mMobClass));
			list.addAll(TowerGameUtils.getGenericLoreComponent("This unit deals " + (TowerConstants.DAMAGE_MLT_CLASS * 100 - 100) + "% more damage to Fighters"));
			meta.lore(list);
			stack.setItemMeta(meta);
		} else if (info.mMobClass == TowerMobClass.FIGHTER) {
			stack = new ItemStack(Material.GOLDEN_AXE);
			ItemMeta meta = stack.getItemMeta();
			meta.displayName(TowerGameUtils.getClassComponent(info.mMobClass));
			list.addAll(TowerGameUtils.getGenericLoreComponent("This unit deals " + (TowerConstants.DAMAGE_MLT_CLASS * 100 - 100) + "% more damage to Casters"));
			meta.lore(list);
			stack.setItemMeta(meta);
		} else {
			stack = new ItemStack(Material.POTION);
			ItemMeta meta = stack.getItemMeta();
			meta.displayName(TowerGameUtils.getClassComponent(info.mMobClass));
			meta.lore(list);
			stack.setItemMeta(meta);
		}

		return stack;
	}

	public static @Nullable ItemStack buildSpellItem(TowerMobInfo info, int index) {
		if (index >= info.mAbilities.size()) {
			return null;
		}

		TowerMobAbility ability = TowerMobAbility.fromString(info.mAbilities.get(index));

		if (ability.mDescription.contains("FAIL!")) {
			return null;
		}
		ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
		ItemMeta meta = stack.getItemMeta();

		meta.displayName(Component.text(ability.mName, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

		List<Component> list = new ArrayList<>(TowerGameUtils.getGenericLoreComponent(ability.mDescription));
		meta.lore(list);
		stack.setItemMeta(meta);
		return stack;

	}


	public static ItemStack buildHPItem(TowerMobInfo info) {
		ItemStack stack = new ItemStack(Material.HEART_OF_THE_SEA);
		ItemMeta meta = stack.getItemMeta();
		meta.displayName(TowerGameUtils.getHpComponent(info.mMobStats.mHP));

		stack.setItemMeta(meta);
		return stack;
	}

	public static ItemStack buildAtkItem(TowerMobInfo info) {
		ItemStack stack;

		if (info.mMobRarity == TowerMobRarity.COMMON) {
			stack = new ItemStack(Material.WOODEN_SWORD);
		} else if (info.mMobRarity == TowerMobRarity.RARE) {
			stack = new ItemStack(Material.STONE_SWORD);
		} else if (info.mMobRarity == TowerMobRarity.EPIC) {
			stack = new ItemStack(Material.IRON_SWORD);
		} else {
			stack = new ItemStack(Material.DIAMOND_SWORD);
		}
		stack.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		ItemMeta meta = stack.getItemMeta();
		meta.displayName(TowerGameUtils.getAtkComponent(info.mMobStats.mAtk));

		stack.setItemMeta(meta);
		return stack;
	}

}
