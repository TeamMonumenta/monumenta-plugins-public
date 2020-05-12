package com.playmonumenta.plugins.cooking;

import com.google.gson.Gson;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

// acts as item editor for the cooking project
public class CookingItemObject {
	private Material mMaterial;
	private String mName;
	private CookingItemType mType;
	private String mLore;
	private ItemUtils.ItemRegion mRegion;
	private ItemUtils.ItemTier mTier;
	private Map<CookingEffectsEnum, Integer> mConsumeEffects;
	private Map<CookingEffectsEnum, Integer> mCookingEffects;
	private ArrayList<String> mNameModifiers;

	CookingItemObject() {
		this.mMaterial = Material.RED_MUSHROOM;
		this.mName = "&2&lnew cooking item";
		this.mType = CookingItemType.SECONDARY;
		this.mLore = "It's time to cook!";
		this.mRegion = ItemUtils.ItemRegion.MONUMENTA;
		this.mTier = ItemUtils.ItemTier.RARE;
		this.mConsumeEffects = new TreeMap<>();
		this.mConsumeEffects.put(CookingEffectsEnum.HEALTH, 1);
		this.mCookingEffects = new TreeMap<>();
		this.mCookingEffects.put(CookingEffectsEnum.HUNGER, -1);
		this.mNameModifiers = new ArrayList<>();
	}

	private String toJson() {
		return (new Gson()).toJson(this);
	}

	private void prepwork() {
		if (!(this.mType == CookingItemType.BASE || this.mTier == ItemUtils.ItemTier.DISH)) {
			this.mConsumeEffects = new TreeMap<>();
		}
		if (this.mTier == ItemUtils.ItemTier.DISH) {
			this.mCookingEffects = new TreeMap<>();
		}
	}

	public ItemStack toCookingItemStack() {

		this.prepwork();

		// init + material
		ItemStack out = new ItemStack(this.mMaterial);
		ItemMeta meta = out.getItemMeta();
		// name
		meta.setDisplayName(this.mName.replace("&", "§"));
		// lore lines
		ArrayList<String> loreLines = new ArrayList<>();
		//    tier
		loreLines.add(this.mRegion.asLoreString() + this.mTier.asLoreString());
		//    lore
		String[] loreLoreLines = this.mLore.split("\n");
		for (String s : loreLoreLines) {
			loreLines.add(ChatColor.DARK_GRAY + s);
		}
		//    blank line
		loreLines.add("");
		//    consume effects
		ArrayList<String> consumeEffectLines = CookingUtils.compileEffectLines(this.mConsumeEffects);
		if (consumeEffectLines.size() > 0) {
			loreLines.add(CookingConsts.NEUTER_COLOR + "When consumed: ");
			for (String s : consumeEffectLines) {
				loreLines.add("  " + s);
			}
		}
		//    type
		if (this.mType != CookingItemType.MEAL) {
			loreLines.add(this.mType.getLoreLine());
		}
		//    ccooking effects
		ArrayList<String> cookingEffectLines = CookingUtils.compileEffectLines(this.mCookingEffects);
		if (cookingEffectLines.size() > 0) {
			loreLines.add(CookingConsts.NEUTER_COLOR + "When cooked: ");
			for (String s : cookingEffectLines) {
				loreLines.add("  " + s);
			}
		}
		//    json identifier (goes to first line, whatever it is)
		loreLines.add(0, StringUtils.convertToInvisibleLoreLine(this.toJson()) + CookingConsts.COOKING_ITEM_JSON_SEPARATOR + loreLines.remove(0));
		//finish
		meta.setLore(loreLines);
		out.setItemMeta(meta);
		return out;
	}

	void setAsMainhandItem(Player p) {
		ItemStack item = this.toCookingItemStack();
		int amount = p.getInventory().getItemInMainHand().getAmount();
		item.setAmount(amount > 0 ? amount : 1);
		p.getInventory().setItemInMainHand(item);
	}

	public void giveCustomPotionEffect(Player p, CookingEffectsEnum effect, int potency, int duration) {
		p.sendMessage("Custom potion effect: " + effect.toString() + " " + potency + " " + duration + " is not yet implemented");
	}

	PotionEffectType getBukkitEffect(CookingEffectsEnum effect) {
		switch (effect) {
			case RESISTANCE_POTENCY:
				return PotionEffectType.DAMAGE_RESISTANCE;
			case NAUSEA_POTENCY:
				return PotionEffectType.CONFUSION;
			case HASTE_POTENCY:
				return PotionEffectType.FAST_DIGGING;
			case STRENGTH_POTENCY:
				return PotionEffectType.INCREASE_DAMAGE;
			case SLOWNESS_POTENCY:
				return PotionEffectType.SLOW;
			default:
				String effectID = effect.toString().replace("_POTENCY", "");
				return PotionEffectType.getByName(effectID);
		}
	}

	public void givePotionEffect(Player p, CookingEffectsEnum effect, int potency, int duration) {
		PotionEffectType potionEffect = this.getBukkitEffect(effect);
		if (potionEffect != null) {
			Plugin.getInstance().mPotionManager.addPotion(p, PotionManager.PotionID.SAFE_ZONE, new PotionEffect(
				potionEffect, duration * 20, potency - 1, true, false));
		} else {
			//effect is a custom effect
			this.giveCustomPotionEffect(p, effect, potency, duration);
		}
	}

	public void consumeItem(Player player) {
		Map<CookingEffectsEnum, Integer> effects = this.getConsumeEffects();
		if (effects.size() == 0) {
			return;
		}
		for (Map.Entry<CookingEffectsEnum, Integer> e : effects.entrySet()) {
			switch (e.getKey().getKind()) {
				case POTENCY:
					givePotionEffect(player, e.getKey(), e.getValue(), effects.get(CookingEffectsEnum
						.valueOf(e.getKey().toString().replace("POTENCY", "DURATION"))));
					break;
				case FOOD:
					switch (e.getKey()) {
						case HEALTH:
							player.setHealth(player.getHealth() + e.getValue());
							break;
						case HUNGER:
							player.setFoodLevel(player.getFoodLevel() + e.getValue());
							break;
						case SATURATION:
							player.setSaturation(player.getSaturation() + e.getValue());
							break;
						default:
							//nothing, yet
							break;
					}
					break;
				default:
					break;
			}
		}
	}

	public void setMaterial(Material material) {
		this.mMaterial = material;
	}

	public Material getMaterial() {
		return this.mMaterial;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public String getName() {
		return this.mName;
	}

	public void setType(CookingItemType t) {
		this.mType = t;
	}

	public CookingItemType getType() {
		return this.mType;
	}

	public void setLore(String lore) {
		this.mLore = lore;
	}

	public void setRegion(ItemUtils.ItemRegion region) {
		this.mRegion = region;
	}

	public ItemUtils.ItemRegion getRegion() {
		return this.mRegion;
	}

	public void setTier(ItemUtils.ItemTier tier) {
		this.mTier = tier;
	}

	public void setConsumeEffect(CookingEffectsEnum key, int val) {
		this.mConsumeEffects.put(key, val);
	}

	public void setConsumeEffects(Map<CookingEffectsEnum, Integer> effects) {
		this.mConsumeEffects = effects;
	}

	public Map<CookingEffectsEnum, Integer> getConsumeEffects() {
		return this.mConsumeEffects;
	}

	public void setCookingEffect(CookingEffectsEnum key, int val) {
		this.mCookingEffects.put(key, val);
	}

	public void setCookingEffects(Map<CookingEffectsEnum, Integer> cookingEffects) {
		this.mCookingEffects = cookingEffects;
	}

	public Map<CookingEffectsEnum, Integer> getCookingEffects() {
		return this.mCookingEffects;
	}

	public void addNameModifier(String str) {
		this.mNameModifiers.add(str);
	}

	public void removeNameModifier(String str) {
		this.mNameModifiers.remove(str);
	}

	public String[] getNameModifiers() {
		return this.mNameModifiers.toArray(new String[0]);
	}
}
