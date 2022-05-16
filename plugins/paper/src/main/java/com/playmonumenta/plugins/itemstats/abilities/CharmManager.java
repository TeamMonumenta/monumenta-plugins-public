package com.playmonumenta.plugins.itemstats.abilities;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.SacredProvisions;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.CosmicMoonblade;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.enchantments.Abyssal;
import com.playmonumenta.plugins.itemstats.enchantments.Adrenaline;
import com.playmonumenta.plugins.itemstats.enchantments.ArcaneThrust;
import com.playmonumenta.plugins.itemstats.enchantments.Decay;
import com.playmonumenta.plugins.itemstats.enchantments.Duelist;
import com.playmonumenta.plugins.itemstats.enchantments.Eruption;
import com.playmonumenta.plugins.itemstats.enchantments.HexEater;
import com.playmonumenta.plugins.itemstats.enchantments.IceAspect;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.itemstats.enchantments.JunglesNourishment;
import com.playmonumenta.plugins.itemstats.enchantments.LifeDrain;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Quake;
import com.playmonumenta.plugins.itemstats.enchantments.RageOfTheKeter;
import com.playmonumenta.plugins.itemstats.enchantments.Regeneration;
import com.playmonumenta.plugins.itemstats.enchantments.Regicide;
import com.playmonumenta.plugins.itemstats.enchantments.Retrieval;
import com.playmonumenta.plugins.itemstats.enchantments.Sapper;
import com.playmonumenta.plugins.itemstats.enchantments.SecondWind;
import com.playmonumenta.plugins.itemstats.enchantments.Slayer;
import com.playmonumenta.plugins.itemstats.enchantments.Smite;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enchantments.ThunderAspect;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class CharmManager {

	public static final String KEY_PLUGIN_DATA = "R3Charms";
	public static final String KEY_CHARMS = "charms";
	public static final String KEY_ITEM = "item";
	public static final int MAX_CHARM_COUNT = 7;
	private static final String CHARM_EFFECT_NAME = "CHARM EFFECT";

	public static CharmManager mInstance;

	public List<String> mCharmEffectList;

	public Map<UUID, List<ItemStack>> mPlayerCharms;

	public Map<UUID, Map<String, Double>> mPlayerCharmEffectMap;

	public Map<UUID, Multimap<ClassAbility, Effect>> mPlayerAbilityEffectMap;


	private CharmManager() {
		mPlayerCharms = new HashMap<>();
		mPlayerCharmEffectMap = new HashMap<UUID, Map<String, Double>>();
		mPlayerAbilityEffectMap = new HashMap<>();
		loadCharmEffects();
	}

	public static CharmManager getInstance() {
		if (mInstance == null) {
			mInstance = new CharmManager();
		}
		return mInstance;
	}

	public void loadCharmEffects() {
		//TODO load new charm effects here
		mCharmEffectList = Arrays.asList(
			// Custom Enchantments
			Inferno.CHARM_DAMAGE,
			ThunderAspect.CHARM_STUN_CHANCE,
			IceAspect.CHARM_DURATION,
			IceAspect.CHARM_SLOW,
			Decay.CHARM_DAMAGE,
			Decay.CHARM_DURATION,
			Sapper.CHARM_HEAL,
			HexEater.CHARM_DAMAGE,
			LifeDrain.CHARM_HEAL,
			Retrieval.CHARM_CHANCE,
			Regicide.CHARM_DAMAGE,
			SecondWind.CHARM_RESISTANCE,
			SecondWind.CHARM_THRESHOLD,
			Sniper.CHARM_DAMAGE,
			PointBlank.CHARM_DAMAGE,
			Adrenaline.CHARM_SPEED,
			Quake.CHARM_DAMAGE,
			Quake.CHARM_RADIUS,
			JunglesNourishment.CHARM_COOLDOWN,
			JunglesNourishment.CHARM_HEALTH,
			RageOfTheKeter.CHARM_COOLDOWN,
			RageOfTheKeter.CHARM_DAMAGE,
			RageOfTheKeter.CHARM_SPEED,
			Regeneration.CHARM_HEAL,
			Smite.CHARM_DAMAGE,
			Slayer.CHARM_DAMAGE,
			Duelist.CHARM_DAMAGE,
			ArcaneThrust.CHARM_DAMAGE,
			ArcaneThrust.CHARM_KNOCKBACK,
			Abyssal.CHARM_DAMAGE,
			Eruption.CHARM_DAMAGE,
			Eruption.CHARM_RADIUS,
			// Classes
			// Mage
			ManaLance.CHARM_DAMAGE,
			ManaLance.CHARM_CHARGES,
			ManaLance.CHARM_COOLDOWN,
			ManaLance.CHARM_RANGE,
			ThunderStep.CHARM_DAMAGE,
			ThunderStep.CHARM_COOLDOWN,
			ThunderStep.CHARM_DISTANCE,
			ThunderStep.CHARM_STUN,
			ThunderStep.CHARM_SIZE,
			PrismaticShield.CHARM_DURATION,
			PrismaticShield.CHARM_KNOCKBACK,
			PrismaticShield.CHARM_STUN,
			PrismaticShield.CHARM_ABSORPTION,
			PrismaticShield.CHARM_TRIGGER,
			PrismaticShield.CHARM_COOLDOWN,
			FrostNova.CHARM_DAMAGE,
			FrostNova.CHARM_DURATION,
			FrostNova.CHARM_RANGE,
			FrostNova.CHARM_SLOW,
			FrostNova.CHARM_COOLDOWN,
			FrostNova.CHARM_FROZEN,
			MagmaShield.CHARM_DAMAGE,
			MagmaShield.CHARM_DURATION,
			MagmaShield.CHARM_KNOCKBACK,
			MagmaShield.CHARM_RANGE,
			MagmaShield.CHARM_CONE,
			MagmaShield.CHARM_COOLDOWN,
			ArcaneStrike.CHARM_DAMAGE,
			ArcaneStrike.CHARM_RANGE,
			ArcaneStrike.CHARM_BONUS,
			ArcaneStrike.CHARM_COOLDOWN,
			ElementalArrows.CHARM_DAMAGE,
			ElementalArrows.CHARM_DURATION,
			ElementalArrows.CHARM_RANGE,
			Spellshock.CHARM_SPEED,
			Spellshock.CHARM_SLOW,
			Spellshock.CHARM_SPELL,
			Spellshock.CHARM_MELEE,
			AstralOmen.CHARM_DAMAGE,
			AstralOmen.CHARM_RANGE,
			AstralOmen.CHARM_MODIFIER,
			AstralOmen.CHARM_STACK,
			CosmicMoonblade.CHARM_CAP,
			CosmicMoonblade.CHARM_DAMAGE,
			CosmicMoonblade.CHARM_RANGE,
			CosmicMoonblade.CHARM_COOLDOWN,
			CosmicMoonblade.CHARM_SPELL_COOLDOWN,
			CosmicMoonblade.CHARM_SLASH,
			SagesInsight.CHARM_SPEED,
			SagesInsight.CHARM_ABILITY,
			SagesInsight.CHARM_DECAY,
			SagesInsight.CHARM_STACKS,
			Blizzard.CHARM_COOLDOWN,
			Blizzard.CHARM_SLOW,
			Blizzard.CHARM_DURATION,
			Blizzard.CHARM_RANGE,
			Blizzard.CHARM_DAMAGE,
			Starfall.CHARM_COOLDOWN,
			Starfall.CHARM_DAMAGE,
			Starfall.CHARM_RANGE,
			Starfall.CHARM_FIRE,
			ElementalSpiritFire.CHARM_COOLDOWN,
			ElementalSpiritFire.CHARM_DAMAGE,
			ElementalSpiritFire.CHARM_SIZE,

			//Cleric
			CelestialBlessing.CHARM_COOLDOWN,
			CelestialBlessing.CHARM_DAMAGE,
			CelestialBlessing.CHARM_DURATION,
			CelestialBlessing.CHARM_SPEED,
			CelestialBlessing.CHARM_RANGE,
			DivineJustice.CHARM_DAMAGE,
			DivineJustice.CHARM_ALLY,
			DivineJustice.CHARM_SELF,
			HeavenlyBoon.CHARM_CHANCE,
			HeavenlyBoon.CHARM_DURATION,
			SacredProvisions.CHARM_CHANCE,
			SacredProvisions.CHARM_RANGE,
			CleansingRain.CHARM_COOLDOWN,
			CleansingRain.CHARM_RANGE,
			CleansingRain.CHARM_REDUCTION,
			CleansingRain.CHARM_DURATION,
			HandOfLight.CHARM_COOLDOWN,
			HandOfLight.CHARM_DAMAGE,
			HandOfLight.CHARM_HEALING,
			HandOfLight.CHARM_RANGE,
			SanctifiedArmor.CHARM_DAMAGE,
			SanctifiedArmor.CHARM_DURATION,
			SanctifiedArmor.CHARM_SLOW,
			HolyJavelin.CHARM_COOLDOWN,
			HolyJavelin.CHARM_DAMAGE,
			HolyJavelin.CHARM_RANGE,
			ChoirBells.CHARM_COOLDOWN,
			ChoirBells.CHARM_RANGE,
			ChoirBells.CHARM_SLOW,
			ChoirBells.CHARM_DAMAGE,
			ChoirBells.CHARM_VULN,
			ChoirBells.CHARM_WEAKEN,
			ChoirBells.CHARM_SLOW,
			LuminousInfusion.CHARM_COOLDOWN,
			LuminousInfusion.CHARM_RANGE,
			LuminousInfusion.CHARM_DAMAGE,
			EnchantedPrayer.CHARM_COOLDOWN,
			EnchantedPrayer.CHARM_DAMAGE,
			EnchantedPrayer.CHARM_RANGE,
			EnchantedPrayer.CHARM_EFFECT_RANGE,
			EnchantedPrayer.CHARM_HEAL,
			ThuribleProcession.CHARM_COOLDOWN,
			ThuribleProcession.CHARM_EFFECT_DURATION,
			ThuribleProcession.CHARM_DAMAGE,
			ThuribleProcession.CHARM_SPEED,
			ThuribleProcession.CHARM_ATTACK,
			ThuribleProcession.CHARM_HEAL,
			HallowedBeam.CHARM_CHARGE,
			HallowedBeam.CHARM_COOLDOWN,
			HallowedBeam.CHARM_DAMAGE,
			HallowedBeam.CHARM_DISTANCE,
			HallowedBeam.CHARM_STUN,
			HallowedBeam.CHARM_HEAL

		);
	}

	public boolean addCharm(Player p, ItemStack charm) {

		if (p != null && mPlayerCharms.get(p.getUniqueId()) != null && validateCharm(p, charm)) {
			ItemStack charmCopy = new ItemStack(charm);
			mPlayerCharms.get(p.getUniqueId()).add(charmCopy);

			updateCharms(p, mPlayerCharms.get(p.getUniqueId()));

			return true;
		} else if (p != null && validateCharm(p, charm)) {
			List<ItemStack> playerCharms = new ArrayList<>();
			ItemStack charmCopy = new ItemStack(charm);
			playerCharms.add(charmCopy);
			mPlayerCharms.put(p.getUniqueId(), playerCharms);

			updateCharms(p, mPlayerCharms.get(p.getUniqueId()));

			return true;
		}

		return false;
	}

	// This method validates the charm against the player's current charm list before actually adding it
	public boolean validateCharm(Player p, ItemStack charm) {
		//Check to make sure the added charm is a valid charm (check lore) and not a duplicate of one they already have!
		//Also make sure the charm list exists if we're checking against it
		//Also make sure the charm list has space for the new charm if it exists
		//Also make sure it's not a stack (only one item)
		//Also parse the charm's power budget and make sure adding it would not overflow

		//Check item stack for valid name and amount
		if (charm == null || charm.getAmount() != 1 || !charm.hasItemMeta() || !charm.getItemMeta().hasDisplayName()) {
			return false;
		}
		// Make sure item has Charm Tier
		if (!ItemStatUtils.getTier(charm).equals(ItemStatUtils.Tier.CHARM)) {
			return false;
		}
		// Charm Power Handling
		int charmPower = ItemStatUtils.getCharmPower(charm);
		if (charmPower > 0) {
			//Now check the player charms to make sure it is different from existing charms
			if (p != null && mPlayerCharms.get(p.getUniqueId()) != null) {
				List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
				//Check max charm count in list
				if (charms.size() >= MAX_CHARM_COUNT) {
					return false;
				}
				int powerBudget = 0;
				//Check naming of each charm
				for (ItemStack c : charms) {
					if (c.getItemMeta().displayName().equals(charm.getItemMeta().displayName())) {
						return false;
					}
					powerBudget += ItemStatUtils.getCharmPower(c);
				}
				//Check to see if adding the extra charm would exceed budget
				Optional<Integer> optionalBudget = ScoreboardUtils.getScoreboardValue(p, "CharmPower");
				int totalBudget = 0;
				if (optionalBudget.isPresent()) {
					totalBudget = optionalBudget.get();
				}
				if (totalBudget == 0) {
					//Default case for testing- later will be set by mechs for all r3 players
					totalBudget = 15;
				}
				if (powerBudget + charmPower > totalBudget) {
					return false;
				}
				return true;
			} else if (p != null) {
				return true;
			}
		}
		return false;
	}

	public boolean removeCharm(Player p, ItemStack charm) {
		if (p != null && mPlayerCharms.get(p.getUniqueId()) != null) {
			//TODO make sure this actually removes the right charm (due to how the itemstack equals method works)

			if (mPlayerCharms.get(p.getUniqueId()).remove(charm)) {
				updateCharms(p, mPlayerCharms.get(p.getUniqueId()));
				return true;
			}
		}
		return false;
	}

	public boolean removeCharmBySlot(Player p, int slot) {
		if (p == null) {
			return false;
		}
		List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
		if (charms != null && slot < charms.size()) {
			charms.remove(slot);
			return true;
		}
		return false;
	}


	public boolean clearCharms(Player p) {
		mPlayerCharms.get(p.getUniqueId()).clear();
		updateCharms(p, mPlayerCharms.get(p.getUniqueId()));
		return true;
	}

	public void updateCharms(Player p, List<ItemStack> equippedCharms) {
		//Calculate the map of effects to values
		Map<String, Double> allEffects = new HashMap<>();
		//Loop through each effect
		for (String charmEffect : mCharmEffectList) {
			double finalValue = 0;
			double finalValuePct = 0;
			//Loop through each charm the player has equipped
			for (ItemStack item : equippedCharms) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				CharmParsedInfo parsedInfo = getPlayerItemLevel(item, charmEffect);
				if (parsedInfo.mIsPercent) {
					finalValuePct += parsedInfo.mValue;
				} else {
					finalValue += parsedInfo.mValue;
				}
			}
			//Separate out flat and percent values so charm effects can use both
			allEffects.put(charmEffect, finalValue);
			allEffects.put(charmEffect + "%", finalValuePct);

		}

		//Store to local map
		mPlayerCharmEffectMap.put(p.getUniqueId(), allEffects);

		//Loop through charms for onhit effects
		Multimap<ClassAbility, Effect> abilityEffects = ArrayListMultimap.create();
		for (ItemStack item : equippedCharms) {
			@NotNull List<@NotNull String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(item));
			for (@NotNull String plainLore : plainLoreLines) {
				if (plainLore.contains("Hit :")) {
					ClassAbility classAbility = null;
					//Step one- get the class ability
					String abilityName = plainLore.split("Hit :")[0]; //Trim back half of line with effect off
					abilityName = abilityName.split("n", 2)[1]; //Get rid of the initial "On"
					abilityName = abilityName.trim();
					for (ClassAbility ca : ClassAbility.values()) {
						if (ca.getName().toLowerCase().equals(abilityName.toLowerCase())) {
							classAbility = ca;
						}
					}
					if (classAbility == null) {
						continue;
					}
					//Step two- get the class effect
					String effectName = plainLore.split(":")[1]; //Trim front half of line with ability off
					Scanner s = new Scanner(effectName);
					//First word of the string will be the effect
					effectName = s.next().toLowerCase();
					double amplifier = -1;
					int duration = -1;
					Scanner s2 = new Scanner(plainLore.split(":")[1]).useDelimiter("\\D+");
					if (plainLore.contains("%")) {
						//Parse the amplifier first, if it exists (indicated by %)
						if (s2.hasNextInt()) {
							amplifier = s2.nextInt() / 100.0;
						}
					}
					if (s2.hasNextInt()) {
						//Parse the final duration now
						duration = s2.nextInt() * 20;
					}
					//Now, create the effect by name and add it
					Effect parsedEffect = null;
					//TODO add more effects to this
					if (effectName.equals("slowness")) {
						parsedEffect = new PercentSpeed(duration, amplifier * -1, CHARM_EFFECT_NAME);
					}
					if (parsedEffect != null) {
						//Add to the player's effect list
						abilityEffects.put(classAbility, parsedEffect);
					}
				}
			}
		}
		//Store to local map
		mPlayerAbilityEffectMap.put(p.getUniqueId(), abilityEffects);
		//Refresh class of player
		AbilityManager.getManager().updatePlayerAbilities(p, false);
	}

	//Helper method to parse item for charm effects
	private CharmParsedInfo getPlayerItemLevel(ItemStack itemStack, String effect) {
		@NotNull List<@NotNull String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(itemStack));
		for (@NotNull String plainLore : plainLoreLines) {
			if (plainLore.contains(effect)) {
				int value = parseValue(plainLore);
				if (plainLore.contains("%")) {
					return new CharmParsedInfo(value, true);
				} else {
					return new CharmParsedInfo(value, false);
				}
			}
		}
		return new CharmParsedInfo(0, false);
	}

	//Helper method to parse lore line for charm effects
	private int parseValue(String loreLine) {
		//Whether effect is being added to or subtracted
		boolean add = loreLine.contains("+");
		//Parse the value from the line

		loreLine = loreLine.split("\\+|-")[1];
		@SuppressWarnings("resource")
		Scanner s = new Scanner(loreLine).useDelimiter("\\D+");
		if (s.hasNextInt()) {
			int sint = s.nextInt();
			//If it's a negative effect
			if (!add) {
				sint = sint * -1;
			}
			s.close();
			return sint;
		}
		s.close();
		// TODO: Error handling if enchant is malformed
		return 0;
	}

	/**
	 * This method will be called by abilities & enchantments to get the modifier value of all
	 * charms the player currently has equipped for a particular attribute.
	 * @param p player to get
	 * @param charmAttribute string property of the charm to parse
	 * @return total value over all charms the player has equipped
	 */
	public double getValueOfAttribute(Player p, String charmAttribute) {
		//Check if charms are enabled (r3 shard), if not, return zero as the net effect
		if (!ServerProperties.getCharmsEnabled() || p == null) {
			return 0;
		}

		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects != null && allEffects.get(charmAttribute) != null) {
			return allEffects.get(charmAttribute).doubleValue();
		}
		return 0;
	}

	public String getSummaryOfAllAttributes(Player p) {
		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects != null) {
			String summary = "";
			for (String s : allEffects.keySet()) {
				if (allEffects.get(s) != 0) {
					if (s.contains("%")) {
						summary += s + " : " + (allEffects.get(s).toString() + 100) + "%" + "\n";
					} else {
						summary += s + " : " + allEffects.get(s).toString() + "\n";
					}
				}
			}
			//Finally, add the custom charm effects
			List<ItemStack> playerCharms = CharmManager.getInstance().mPlayerCharms.get(p.getUniqueId());
			List<String> effectsAlreadyAdded = new ArrayList<>();
			if (playerCharms != null) {
				for (ItemStack charm : playerCharms) {
					@NotNull List<@NotNull String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(charm));
					for (@NotNull String plainLore : plainLoreLines) {
						if (plainLore.contains("Hit :") && !effectsAlreadyAdded.contains(plainLore)) {
							summary += plainLore + "\n";
							effectsAlreadyAdded.add(plainLore);
						}
					}
				}
			}
			return summary;
		}
		return null;
	}

	public List<Component> getSummaryOfAllAttributesAsComponents(Player p) {
		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects != null) {
			List<Component> components = new ArrayList<>();
			for (String s : allEffects.keySet()) {
				if (allEffects.get(s) != 0) {
					if (s.contains("%")) {
						if ((allEffects.get(s) > 0 && !s.contains("Cooldown")) || (allEffects.get(s) < 0 && s.contains("Cooldown"))) {
							components.add(Component.text(s + " : " + allEffects.get(s).toString() + "%", TextColor.fromHexString("#4AC2E5")).decoration(TextDecoration.ITALIC, false));
						} else {
							components.add(Component.text(s + " : " + allEffects.get(s).toString() + "%", TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false));
						}
					} else {
						if ((allEffects.get(s) > 0 && !s.contains("Cooldown")) || (allEffects.get(s) > 0 && s.contains("Cooldown"))) {
							components.add(Component.text(s + " : " + allEffects.get(s).toString(), TextColor.fromHexString("#4AC2E5")).decoration(TextDecoration.ITALIC, false));
						} else {
							components.add(Component.text(s + " : " + allEffects.get(s).toString(), TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false));
						}
					}
				}
			}
			//Now add the custom charm effects
			List<ItemStack> playerCharms = CharmManager.getInstance().mPlayerCharms.get(p.getUniqueId());
			List<String> effectsAlreadyAdded = new ArrayList<>();

			if (playerCharms != null) {
				for (ItemStack charm : playerCharms) {
					@NotNull List<@NotNull String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(charm));
					for (@NotNull String plainLore : plainLoreLines) {
						if (plainLore.contains("Hit :") && !effectsAlreadyAdded.contains(plainLore)) {
							components.add(Component.text(plainLore, TextColor.fromHexString("#C8A2C8")).decoration(TextDecoration.ITALIC, false));
							effectsAlreadyAdded.add(plainLore);
						}
					}
				}
			}
			return components;
		}
		return null;
	}

	public String getSummaryOfCharmNames(Player p) {
		List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
		if (charms != null) {
			String summary = "";
			int powerBudget = 0;
			for (ItemStack item : charms) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					summary += item.getItemMeta().getDisplayName() + "\n";
					powerBudget += ItemStatUtils.getCharmPower(item);
				}
			}
			summary += ChatColor.YELLOW + "Charm Power: " + powerBudget;
			return summary;
		}
		return null;
	}

	public int getCharmPower(Player p) {
		List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
		int powerBudget = 0;
		if (charms != null) {
			for (ItemStack item : charms) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					powerBudget += ItemStatUtils.getCharmPower(item);
				}
			}
		}
		return powerBudget;
	}

	//Handlers for player lifecycle events

	//Discard charm data a few ticks after player leaves shard
	//(give time for save event to register)
	public void onQuit(Player p) {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!p.isOnline()) {
					mPlayerCharms.remove(p.getUniqueId());
					mPlayerCharmEffectMap.remove(p.getUniqueId());
					mPlayerAbilityEffectMap.remove(p.getUniqueId());
				}
			}

		}.runTaskLater(Plugin.getInstance(), 100);
	}

	//Store local charm data into plugin data
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		List<ItemStack> charms = mPlayerCharms.get(player.getUniqueId());
		if (charms != null) {
			JsonObject data = new JsonObject();
			JsonArray charmArray = new JsonArray();
			data.add(KEY_CHARMS, charmArray);
			Iterator<ItemStack> iterCharms = charms.iterator();
			while (iterCharms.hasNext()) {
				ItemStack charm = iterCharms.next();

				JsonObject charmData = new JsonObject();
				charmData.addProperty(KEY_ITEM, NBTItem.convertItemtoNBT(charm).toString());
				if (charmData != null) {
					charmArray.add(charmData);
				}
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	//Load plugin data into local charm data
	public void onJoin(Player p) {
		JsonObject charmPluginData = MonumentaRedisSyncAPI.getPlayerPluginData(p.getUniqueId(), KEY_PLUGIN_DATA);
		if (charmPluginData != null) {
			if (charmPluginData.has(KEY_CHARMS)) {
				JsonArray charmArray = charmPluginData.getAsJsonArray(KEY_CHARMS);
				List<ItemStack> playerCharms = new ArrayList<>();
				for (JsonElement charmElement : charmArray) {
					JsonObject data = charmElement.getAsJsonObject();
					if (data.has(KEY_ITEM) && data.get(KEY_ITEM).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_ITEM).isString()) {
						ItemStack item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_ITEM).getAsString()));
						if (item != null) {
							playerCharms.add(item);
						}
					}
				}
				//Check if we actually loaded any charms
				if (playerCharms.size() > 0) {
					mPlayerCharms.put(p.getUniqueId(), playerCharms);
					//Recalculate the charm map based on loaded charms by calling update
					updateCharms(p, mPlayerCharms.get(p.getUniqueId()));
				}
			}
		}
	}

	//Methods called by the abilities

	public static double getRadius(Player player, String charmEffectName, double baseRadius) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return baseRadius * ((level / 100.0) + 1);
	}

	public static double getExtraDamage(Player player, String charmEffectName) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
		return level;
	}

	public static int getExtraDuration(Player player, String charmEffectName) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
		return (int) (level * 20);
	}

	public static double getExtraPercentDamage(Player player, String charmEffectName, double baseDamage) {
		double percentage = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return baseDamage * (1 + (percentage / 100.0));
	}

	public static int getCooldown(Player player, String charmEffectName, int baseCooldown) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return (int) (baseCooldown * ((level / 100.0) + 1));
	}

	public static double getLevel(Player player, String charmEffectName) {
		return CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
	}

	public static double getLevelPercent(Player player, String charmEffectName) {
		return CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
	}

	public static double getLevelPercentDecimal(Player player, String charmEffectName) {
		return CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%") / 100.0;
	}

	public static double getExtraPercentHealing(Player player, String charmEffectName, double baseHealing) {
		double percentage = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return baseHealing * (1 + (percentage / 100.0));
	}

	public static double getExtraPercent(Player player, String charmEffectName, double base) {
		double percentage = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return base * (1 + (percentage / 100.0));
	}

	//Calculates the final amount using both flat and percent modifiers, applying flat before percent
	public static double calculateFlatAndPercentValue(Player player, String charmEffectName, double baseValue) {
		double flatLevel = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
		double percentLevel = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");

		return (baseValue + flatLevel) * ((percentLevel / 100.0) + 1);
	}

	private static class CharmParsedInfo {
		public int mValue;
		public boolean mIsPercent;

		public CharmParsedInfo(int value, boolean isPercent) {
			mValue = value;
			mIsPercent = isPercent;
		}
	}
}
