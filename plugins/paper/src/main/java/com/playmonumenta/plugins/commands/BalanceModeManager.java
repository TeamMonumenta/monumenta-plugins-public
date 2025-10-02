package com.playmonumenta.plugins.commands;

import com.comphenix.protocol.events.PacketEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.*;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import dev.jorel.commandapi.CommandAPICommand;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.math3.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BalanceModeManager {
	private static final ArrayList<UUID> mPlayerList = new ArrayList<>();
	public static final String COMMAND = "balancemode";
	private static @Nullable JsonObject oData; // obfuscated data

	public static void register() {
		new CommandAPICommand(COMMAND)
			.executesPlayer((player, args) -> {
				UUID uuid = player.getUniqueId();
				if (mPlayerList.contains(uuid)) {
					mPlayerList.remove(uuid);
					player.sendMessage(Component.text("Balance Mode de-activated.", NamedTextColor.DARK_GRAY));
				} else {
					mPlayerList.add(uuid);
					player.sendMessage(Component.text("Balance Mode activated. Drop an item or NBT chest in hand to display balance stats.", NamedTextColor.DARK_GRAY));
					try {
						String rawObfuscationData = Files.readString(Plugin.getInstance().getDataFolder().toPath().resolve("balance_data.json"));
						Gson gson = new Gson();
						oData = gson.fromJson(rawObfuscationData, JsonObject.class);
					} catch (IOException e) {
						mPlayerList.remove(uuid);
						player.sendMessage(Component.text("Balance Mode de-activated. (Could not find balance_data.json)", NamedTextColor.DARK_GRAY));
					}
				}
			})
			.register();
	}

	public void onPlayerDropItemCreative(PacketEvent event) {
		if (Plugin.IS_PLAY_SERVER || oData == null) {
			return;
		}

		Player player = event.getPlayer();
		if (!mPlayerList.contains(player.getUniqueId())) {
			return;
		}

		event.setCancelled(true);
		Bukkit.getScheduler().runTask(Plugin.getInstance(), player::updateInventory);

		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}

		if (item.getType() == Material.CHEST) {
			boolean anyR1R2Items = false;
			ArrayList<String> spreadsheetData = new ArrayList<>();
			ReadableNBTList<ReadWriteNBT> nbtItemList = ItemUtils.getContainerItems(item);
			if (nbtItemList == null || nbtItemList.isEmpty()) {
				return;
			}
			for (ReadWriteNBT nbtItem : nbtItemList) {
				ItemStack itemStack = NBT.itemStackFromNBT(nbtItem);
				anyR1R2Items = ItemStatUtils.getRegion(itemStack) != Region.RING || anyR1R2Items;
				determineSlotAndEvaluate(itemStack, player, spreadsheetData, oData);
			}
			if (anyR1R2Items) {
				return;
			}

			StringBuilder fullSheetString = new StringBuilder();
			for (String s : spreadsheetData) {
				fullSheetString.append(s).append("\n");
			}

			Component copyAllButton = Component.text("[copy all]", NamedTextColor.GRAY)
				.hoverEvent(HoverEvent.showText(Component.text("Click to copy ALL spreadsheet data to your clipboard.", NamedTextColor.GREEN)))
				.clickEvent(ClickEvent.copyToClipboard(fullSheetString.toString()));

			player.sendMessage(copyAllButton);
		}

		determineSlotAndEvaluate(item, player, null, oData);
	}

	private void determineSlotAndEvaluate(ItemStack item, Player player, @Nullable List<String> spreadsheetData, JsonObject oData) {
		for (Slot s : Slot.values()) {
			if (!ItemStatUtils.hasAttributeInSlot(item, s)) {
				continue;
			}
			int mwLevel = MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item));
			Region region = ItemStatUtils.getRegion(item);

			if (s == Slot.MAINHAND) {
				player.sendMessage(Component.text("Mainhand items are currently not supported by Balance Mode.", NamedTextColor.DARK_GRAY));
				return;
			}
			if (region == Region.RING && mwLevel < 0) {
				player.sendMessage(Component.text("Cannot evaluate R3 items with no Masterwork.", NamedTextColor.DARK_GRAY));
				return;
			}


			// Evaluate the item and store its imbalance
			float imbalance = evaluateItem(item, s, player, 0f, spreadsheetData, oData, region);
			if (region != Region.RING) {
				return;
			}

			// Evaluate the previous item
			float evaluationLimit = oData.get("evaluation_limit").getAsFloat();
			if (imbalance < -evaluationLimit || imbalance > evaluationLimit) {
				ArrayList<ItemStack> allMasterworks = (ArrayList<ItemStack>) MasterworkUtils.getAllMasterworks(item, player);
				// If item does not have a loot-tabled masterwork tree, OR if the item is of the lowest masterwork tier, return
				Masterwork baseMasterwork = ItemStatUtils.getMasterwork(MasterworkUtils.getBaseMasterwork(item, player));
				if (allMasterworks.isEmpty() || ItemStatUtils.getMasterwork(item) == baseMasterwork) {
					return;
				}
				evaluateItem(allMasterworks.get(mwLevel - 1 - MasterworkUtils.getMasterworkAsInt(baseMasterwork)), s, player, imbalance, spreadsheetData, oData, region);
			}
		}
	}

	// returns true if "balanced", else false
	private float evaluateItem(ItemStack item, Slot slot, Player player, float nextImbalance, @Nullable List<String> spreadsheetData, JsonObject oData, Region region) {
		StringBuilder spreadsheetString = new StringBuilder(ItemUtils.getPlainName(item) + "\t" + item.getType().name() + "\t3\t");
		float[] spreadsheetValues = new float[62];
		int mw = MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item));
		spreadsheetString.append(mw <= 3 ? "Rare" : mw <= 5 ? "Artifact" : mw == 6 ? "Epic" : "Legendary");
		spreadsheetString.append("\t").append(mw).append("\t\t").append(ItemStatUtils.getLocation(item).getDisplayName());
		spreadsheetString.append("\t\t").append(mw + 4);

		float meleeProtScore = calcProtValue(item, EnchantmentType.MELEE_PROTECTION, EnchantmentType.MELEE_FRAGILITY, spreadsheetValues, 2, oData);
		float projProtScore = calcProtValue(item, EnchantmentType.PROJECTILE_PROTECTION, EnchantmentType.PROJECTILE_FRAGILITY, spreadsheetValues, 3, oData);
		float magicProtScore = calcProtValue(item, EnchantmentType.MAGIC_PROTECTION, EnchantmentType.MAGIC_FRAGILITY, spreadsheetValues, 5, oData);
		float blastProtScore = calcProtValue(item, EnchantmentType.BLAST_PROTECTION, EnchantmentType.BLAST_FRAGILITY, spreadsheetValues, 4, oData);

		float armorValueMult = calcArmorValue(item, slot, spreadsheetValues, oData, region);
		float enchantValueMult = calcBaseEnchantValues(item, slot, spreadsheetValues, oData, region);

		// This function is a mess but its a very important one so its heavily obfuscated
		String regionString = region == Region.VALLEY ? "v" : region == Region.ISLES ? "i" : "r";
		float result = obOp("op1",
			obOp("op2", enchantValueMult, armorValueMult,
				obOp("op3",
					obOp("op4",
						obOp("op5", oData.get("mepc" + regionString).getAsFloat(), sqrtOrZero(meleeProtScore)),
						obOp("op6", oData.get("ppc" + regionString).getAsFloat(), sqrtOrZero(projProtScore)),
						obOp("op7", oData.get("mapc" + regionString).getAsFloat(), sqrtOrZero(magicProtScore)),
						obOp("op8", oData.get("bpc" + regionString).getAsFloat(), sqrtOrZero(blastProtScore))
						),
					oData.get("pc").getAsFloat())),
			oData.get("fc1").getAsFloat() / oData.get("fc2").getAsFloat());

		// Calculations over, now it's time to check the budget and display info.
		if (region == Region.VALLEY || region == Region.ISLES) {
			List<Pair<String, Float>> budgets = region == Region.VALLEY ? List.of(
				new Pair<>("T1", oData.get("budget_v1").getAsFloat()),
				new Pair<>("T2", oData.get("budget_v2").getAsFloat()),
				new Pair<>("T3 / Low Uncommon", oData.get("budget_v3").getAsFloat()),
				new Pair<>("T4 / Mid Uncommon", oData.get("budget_v4").getAsFloat()),
				new Pair<>("T5 / High Uncommon / Low Rare", oData.get("budget_v5").getAsFloat()),
				new Pair<>("Mid Rare", oData.get("budget_v6").getAsFloat()),
				new Pair<>("High Rare", oData.get("budget_v7").getAsFloat()),
				new Pair<>("Artifact", oData.get("budget_v8").getAsFloat()),
				new Pair<>("Epic", oData.get("budget_v9").getAsFloat())
			) : List.of(
				new Pair<>("T1", oData.get("budget_i1").getAsFloat()),
				new Pair<>("T2", oData.get("budget_i2").getAsFloat()),
				new Pair<>("T3", oData.get("budget_i3").getAsFloat()),
				new Pair<>("T4 / Low Uncommon", oData.get("budget_i4").getAsFloat()),
				new Pair<>("T5 / Mid Uncommon / Low Rare", oData.get("budget_i5").getAsFloat()),
				new Pair<>("High Uncommon / Mid Rare", oData.get("budget_i6").getAsFloat()),
				new Pair<>("High Rare", oData.get("budget_i7").getAsFloat()),
				new Pair<>("Very High Rare / Low Artifact", oData.get("budget_i8").getAsFloat()),
				new Pair<>("Mid Artifact", oData.get("budget_i9").getAsFloat()),
				new Pair<>("High Artifact", oData.get("budget_i10").getAsFloat()),
				new Pair<>("Epic", oData.get("budget_i11").getAsFloat())
			);

			int minDiffIndex = 0;
			for (int i = 1; i < budgets.size(); i++) {
				if (Math.abs(budgets.get(i).getValue() - result) < Math.abs(budgets.get(minDiffIndex).getValue() - result)) {
					minDiffIndex = i;
				}
			}

			Component evaluation = Component.text(ItemUtils.getPlainName(item) + " has ")
				.append(Component.text(result, NamedTextColor.GOLD))
				.append(Component.text(" value.\n - Estimated tier: "))
				.append(Component.text(budgets.get(minDiffIndex).getKey(), NamedTextColor.LIGHT_PURPLE));
			float diff = result - budgets.get(minDiffIndex).getValue();
			player.sendMessage(evaluation.append(Component.text(" (" + (diff < 0 ? "" : "+") + Math.round(diff * 1000) / 1000f + ")", NamedTextColor.GRAY)));
			return 0;
		}

		float budget = getBudget(item, oData);
		float difference = result - budget;

		float evaluationLimit = oData.get("evaluation_limit").getAsFloat();
		if (nextImbalance != 0f) {
			if (Math.abs(difference) < evaluationLimit) {
				player.sendMessage(Component.text("(The previous Masterwork was in a balanced range.)", NamedTextColor.LIGHT_PURPLE));
				return difference;
			}

			if (Math.abs(nextImbalance - difference) < evaluationLimit) {
				player.sendMessage(Component.text("(The previous Masterwork was similarly imbalanced.)", NamedTextColor.GOLD));
			} else if (nextImbalance - difference > evaluationLimit) {
				player.sendMessage(Component.text("(This item is significantly greater than the previous.)", NamedTextColor.DARK_RED));
			}
			return difference;
		}

		Component evaluation;
		if (difference > evaluationLimit) {
			evaluation = Component.text(ItemUtils.getPlainName(item) + " M" + MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item)) + " (" + slot.getName() + ") has ").append(
				Component.text(result, NamedTextColor.RED).append(Component.text(" value.\n - It is over-statted by +" + difference + ".")));
		} else if (difference < -evaluationLimit) {
			evaluation = Component.text(ItemUtils.getPlainName(item) + " M" + MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item)) + " (" + slot.getName() + ") has ").append(
				Component.text(result, NamedTextColor.RED).append(Component.text(" value.\n - It is under-statted by " + difference + ".")));
		} else {
			evaluation = Component.text(ItemUtils.getPlainName(item) + " M" + MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item)) + " (" + slot.getName() + ") has ").append(
				Component.text(result, NamedTextColor.GREEN).append(Component.text(" value.")));
		}

		for (float spreadsheetValue : spreadsheetValues) {
			spreadsheetString.append("\t");
			if (spreadsheetValue != 0) {
				spreadsheetString.append(Math.round(1000 * spreadsheetValue) / 1000f);
			}
		}
		Component copyButton = Component.text("[copy]", NamedTextColor.GRAY)
			.hoverEvent(HoverEvent.showText(Component.text("Click to copy spreadsheet data to your clipboard.", NamedTextColor.GREEN)))
			.clickEvent(ClickEvent.copyToClipboard(spreadsheetString.toString()));

		if (spreadsheetData != null) {
			spreadsheetData.add(spreadsheetString.toString());
		}
		player.sendMessage(evaluation.append(Component.text(" ").append(copyButton)));

		return difference;
	}

	private float sqrtOrZero(float val) {
		return val > 0f ? (float) Math.sqrt(val) : 0f;
	}

	private float calcBaseEnchantValues(ItemStack item, Slot slot, float[] spreadsheetValues, JsonObject oData, Region region) {
		// PREDEFINED CONSTANTS ---------------------------------------------------------------------------------------------------------
		float meleeMult = oData.get("melee_constant").getAsFloat();
		float projMult = oData.get("proj_constant").getAsFloat();
		float magicMult = oData.get("magic_constant").getAsFloat();
		float standardHP = oData.get("hp_constant_std").getAsFloat();

		// ATTRIBUTES -------------------------------------------------------------------------------------------------------------------
		float attrHP = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.MAX_HEALTH, Operation.ADD, slot);
		float attrPercentHP = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.MAX_HEALTH, Operation.MULTIPLY, slot);
		float attrMeleeDamage = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.ATTACK_DAMAGE_MULTIPLY, Operation.MULTIPLY, slot);
		float attrAttackSpeedPercent = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.ATTACK_SPEED, Operation.MULTIPLY, slot);
		float attrMagicDamage = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.MAGIC_DAMAGE_MULTIPLY, Operation.MULTIPLY, slot);
		float attrProjDamage = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.PROJECTILE_DAMAGE_MULTIPLY, Operation.MULTIPLY, slot);
		float attrProjSpeed = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.PROJECTILE_SPEED, Operation.MULTIPLY, slot); // for some reason proj speed is worth 0 in all calcs lol
		float attrThrowRate = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.THROW_RATE, Operation.MULTIPLY, slot);
		float attrThornsFlat = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.THORNS, Operation.ADD, slot);
		float attrThornsPercent = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.THORNS, Operation.MULTIPLY, slot);
		float attrSpeedFlat = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.SPEED, Operation.ADD, slot);
		float attrSpeedPercent = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.SPEED, Operation.MULTIPLY, slot);
		float attrKbr = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.KNOCKBACK_RESISTANCE, Operation.ADD, slot);

		// ENCHANTMENTS -----------------------------------------------------------------------------------------------------------------
		int enchSustenance = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SUSTENANCE);
		int enchAnemia = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_ANEMIA);
		int enchRegen = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.REGENERATION);
		int enchLifeDrain = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.LIFE_DRAIN);
		int enchSapper = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SAPPER);
		int enchFirstStrike = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRST_STRIKE);
		int enchStamina = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STAMINA);
		int enchRetaliation = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.RETALIATION);
		int enchTrivium = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.TRIVIUM);
		int enchFractal = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FRACTAL);
		int enchTechnique = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.TECHNIQUE);
		int enchVersatility = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.VERSATILITY);
		int enchRegicide = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.REGICIDE);
		int enchInferno = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INFERNO);
		int enchAdrenaline = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ADRENALINE);
		int enchDepthStrider = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.DEPTH_STRIDER);
		int enchAquaAffinity = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.AQUA_AFFINITY);
		int enchRespiration = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.RESPIRATION);
		int enchGills = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.GILLS);
		int enchAbyssal = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ABYSSAL);
		int enchSecondWind = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SECOND_WIND);
		int enchAdaptability = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ADAPTABILITY);
		int enchIntuition = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INTUITION);
		int enchVanishing = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_VANISHING);
		int enchCorruption = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_CORRUPTION);
		int enchTriage = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.TRIAGE);
		int enchAptitude = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.APTITUDE);
		int enchIneptitude = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INEPTITUDE);
		int enchCrippling = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_CRIPPLING);
		int enchInstability = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_INSTABILITY);
		int enchFireProt = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_PROTECTION) - ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_FRAGILITY);
		int enchFeatherFalling = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FEATHER_FALLING) - ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FALL_FRAGILITY);

		int unbreaking = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.UNBREAKING);
		int unbreakable = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.UNBREAKABLE);
		int mending = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.MENDING);
		int shield = ItemUtils.getItemType(item) == ItemType.SHIELD ? 1 : 0;

		// LET'S ADD STUFF TO THE SPREADSHEET ARRAY -------------------------------------------------------------------------------------
		spreadsheetValues[6] = enchFireProt;
		spreadsheetValues[7] = enchFeatherFalling;
		spreadsheetValues[8] = attrHP;
		spreadsheetValues[9] = attrPercentHP * 100;
		spreadsheetValues[10] = attrSpeedFlat;
		spreadsheetValues[11] = attrSpeedPercent * 100;
		spreadsheetValues[12] = attrAttackSpeedPercent * 100;
		spreadsheetValues[13] = attrMeleeDamage * 100;
		spreadsheetValues[14] = attrProjSpeed * 100;
		spreadsheetValues[15] = attrThrowRate * 100;
		spreadsheetValues[16] = attrProjDamage * 100;
		spreadsheetValues[17] = attrMagicDamage * 100;
		spreadsheetValues[18] = attrKbr * 10;
		spreadsheetValues[19] = attrThornsFlat;
		spreadsheetValues[20] = enchAquaAffinity;
		spreadsheetValues[21] = enchRespiration;
		spreadsheetValues[22] = enchDepthStrider;
		spreadsheetValues[23] = enchGills;
		spreadsheetValues[24] = enchRegen;
		spreadsheetValues[25] = enchLifeDrain;
		spreadsheetValues[26] = enchSustenance - enchAnemia;
		spreadsheetValues[27] = enchInferno;
		spreadsheetValues[28] = enchAdrenaline;
		spreadsheetValues[29] = enchRegicide;
		spreadsheetValues[30] = enchCrippling;
		spreadsheetValues[31] = enchSecondWind;
		spreadsheetValues[32] = enchTriage;
		spreadsheetValues[33] = enchAptitude;
		spreadsheetValues[34] = enchIneptitude;
		spreadsheetValues[35] = enchAdaptability;
		spreadsheetValues[36] = enchIntuition;
		spreadsheetValues[37] = enchFirstStrike;
		spreadsheetValues[38] = enchStamina;
		spreadsheetValues[39] = enchTrivium;
		spreadsheetValues[40] = enchTechnique;
		spreadsheetValues[41] = enchVersatility;
		spreadsheetValues[42] = enchFractal;
		spreadsheetValues[53] = enchVanishing;
		spreadsheetValues[54] = enchCorruption;
		spreadsheetValues[55] = enchInstability;
		spreadsheetValues[56] = unbreaking;
		spreadsheetValues[57] = mending;
		spreadsheetValues[58] = unbreakable;
		spreadsheetValues[59] = shield;
		spreadsheetValues[60] = enchRetaliation;
		spreadsheetValues[61] = enchAbyssal;

		// THE WILD WILD WEST OF HEALTH CALCULATIONS ------------------------------------------------------------------------------------
		float artificialSustenance = oData.get("art_sust_base").getAsFloat();
		float artificialAnemia = oData.get("art_anem_base").getAsFloat();
		float overallSustenance = enchSustenance - enchAnemia;
		float overallAnemia;

		if (attrPercentHP > 0) {
			attrPercentHP = obOp("op_health1", attrPercentHP, oData.get("percent_hp_constant").getAsFloat());
		}
		float totalHealth = (standardHP + attrHP) * (1 + attrPercentHP);

		// obfuscated health calculations
		if (totalHealth > standardHP + (region == Region.RING ? 0 : 2)) {
			for (int i = 0; i < Math.floor(totalHealth - standardHP); i++) {
				artificialAnemia = obOp("op_health1", artificialAnemia, obOp("op_health2", 1f, obOp("op_health3", standardHP, i)));
			}
		} else if (totalHealth < standardHP - (region == Region.RING ? 0 : 2)) {
			for (int i = 0; i < Math.floor(standardHP - totalHealth); i++) {
				artificialSustenance = obOp("op_health1", artificialSustenance, obOp("op_health2", 1f, obOp("op_health3", standardHP, -i)));
			}
		}

		if (overallSustenance < 0) {
			overallAnemia = overallSustenance / oData.get("overall_anem_constant").getAsFloat();
			overallSustenance = oData.get("default_sust").getAsFloat();
		} else {
			overallAnemia = oData.get("default_anemia").getAsFloat();
		}

		float healthScore = obOp("op_health", (standardHP / totalHealth),
			(1 + oData.get("hs_smult1").getAsFloat() * overallSustenance + oData.get("hs_smult2").getAsFloat() * artificialSustenance),
			(1 + oData.get("hs_amult1").getAsFloat() * overallAnemia + oData.get("hs_amult2").getAsFloat() * artificialAnemia),
			(1 + oData.get("hs_rmult1").getAsFloat() * (float) Math.pow(enchRegen, oData.get("hs_rmult2").getAsFloat())),
			(1 + oData.get("hs_ldmult1").getAsFloat() * (float) Math.pow(enchLifeDrain, oData.get("hs_ldmult2").getAsFloat())),
			(1 + oData.get("hs_samult").getAsFloat() * enchSapper));

		// THE DAMAGE ZONE (OUCH) -------------------------------------------------------------------------------------------------------
		float meleeScore = (1 +
			oData.get("ms_bmult").getAsFloat() *
				(attrMeleeDamage + enchFirstStrike * oData.get("ms_fmult").getAsFloat() + enchStamina * oData.get("ms_smult").getAsFloat() + enchRetaliation * oData.get("ms_rmult").getAsFloat()))
			* (1 + oData.get("ms_asmult").getAsFloat() * attrAttackSpeedPercent) - 1;
		meleeScore /= meleeMult;
		float projScore = (1 + oData.get("ps_pmult").getAsFloat() * attrProjDamage + oData.get("ps_psmult").getAsFloat() * attrProjSpeed)
			* (1 + oData.get("ps_tmult").getAsFloat() * attrThrowRate) - 1;
		projScore /= projMult;
		float magicScore = oData.get("ws_mmult").getAsFloat() * (attrMagicDamage + oData.get("ws_tmult").getAsFloat() * enchTrivium + oData.get("ws_fmult").getAsFloat() * enchFractal);
		magicScore /= magicMult;
		float infernoScore = oData.get("os_imult").getAsFloat() * enchInferno;

		int thornsDiscount = region == Region.RING ? 4 : region == Region.ISLES ? 2 : 1;
		float thornsScore = oData.get("os_tmult").getAsFloat() * Math.max(0, attrThornsFlat - thornsDiscount);
		if (attrThornsPercent > 0) { // Unused hopefully but leaving it in so things don't break :)
			thornsScore += oData.get("os_tmult2").getAsFloat();
			thornsScore *= (oData.get("os_tmult3").getAsFloat() + attrThornsPercent);
		}

		float regicideScore;
		float versatilityScore;
		float techniqueScore;

		if (projScore > 0) {
			regicideScore = oData.get("rs_1").getAsFloat() * enchRegicide / projMult;
			versatilityScore = oData.get("vs_1").getAsFloat() * enchVersatility / projMult;
			techniqueScore = oData.get("ts_1").getAsFloat() * enchTechnique / projMult;
		} else if (magicScore > 0) {
			regicideScore = oData.get("rs_2").getAsFloat() * enchRegicide / magicMult;
			versatilityScore = oData.get("vs_2").getAsFloat() * enchVersatility / magicMult;
			techniqueScore = oData.get("ts_2").getAsFloat() * enchTechnique / magicMult;
		} else if (meleeScore > 0) {
			regicideScore = oData.get("rs_3").getAsFloat() * enchRegicide / meleeMult;
			versatilityScore = oData.get("vs_3").getAsFloat() * enchVersatility / meleeMult;
			techniqueScore = oData.get("ts_3").getAsFloat() * enchTechnique / meleeMult;
		} else {
			regicideScore = oData.get("rs_4").getAsFloat() * enchRegicide / meleeMult;
			versatilityScore = oData.get("vs_4").getAsFloat() * enchVersatility / meleeMult;
			techniqueScore = oData.get("ts_4").getAsFloat() * enchTechnique / meleeMult;
		}

		float damageScore = obOp("op_dmg", oData.get("ds_1").getAsFloat() * Math.max(meleeScore, Math.max(projScore, magicScore)),
			oData.get("ds_2").getAsFloat() * obOp("op_dmg", meleeScore, magicScore, projScore),
			infernoScore, thornsScore, regicideScore, versatilityScore, techniqueScore);

		// WATER AND SPEED STUFF :) -----------------------------------------------------------------------------------------------------
		float speedScore = oData.get("ss_smult").getAsFloat() * attrSpeedPercent
			+ oData.get("ss_fmult").getAsFloat() * attrSpeedFlat
			+ oData.get("ss_amult").getAsFloat() * enchAdrenaline;

		float respirationScore =
			enchRespiration == 0 ? 0f
				: enchRespiration == 1 ? oData.get("respiration1").getAsFloat()
				: enchRespiration == 2 ? oData.get("respiration2").getAsFloat()
				: oData.get("respiration3").getAsFloat();
		float waterScore = oData.get("depth_strider").getAsFloat() * enchDepthStrider + oData.get("aqua_affinity").getAsFloat() * enchAquaAffinity + respirationScore + oData.get("gills").getAsFloat() * enchGills + oData.get("abyssal").getAsFloat() * enchAbyssal;
		if (region == Region.VALLEY) {
			waterScore /= 3;
		}

		return obOp("op_enchants3", obOp("op_enchants1", totalHealth, standardHP),
			(oData.get("final_enchant1").getAsFloat() + obOp("op_enchants2", healthScore, oData.get("final_enchant2").getAsFloat())),
			(oData.get("final_enchant3").getAsFloat() + damageScore),
			(oData.get("final_enchant4").getAsFloat() + speedScore),
			(oData.get("final_enchant5").getAsFloat() + oData.get("final_enchant6").getAsFloat() * attrKbr),
			obOp("op_enchants4", oData.get("final_enchant7").getAsFloat(), shield),
			obOp("op_enchants4", region == Region.RING ? oData.get("final_enchant8").getAsFloat() : oData.get("final_enchant8_2").getAsFloat(), waterScore),
			obOp("op_enchants5", oData.get("final_enchant9").getAsFloat(), enchFireProt),
			obOp("op_enchants5", oData.get("final_enchant10").getAsFloat(), enchFeatherFalling),
			obOp("op_enchants5", oData.get("final_enchant11").getAsFloat(), enchSecondWind),
			obOp("op_enchants6", oData.get("final_enchant12").getAsFloat(), enchAdaptability),
			obOp("op_enchants6", oData.get("final_enchant13").getAsFloat(), enchIntuition),
			obOp("op_enchants6", oData.get("final_enchant14").getAsFloat(), enchTriage),
			obOp("op_enchants6", oData.get("final_enchant15").getAsFloat(), enchAptitude),
			obOp("op_enchants7", oData.get("final_enchant16").getAsFloat(), enchIneptitude),
			obOp("op_enchants7", oData.get("final_enchant17").getAsFloat(), enchVanishing),
			obOp("op_enchants7", oData.get("final_enchant18").getAsFloat(), enchCorruption),
			obOp("op_enchants7", oData.get("final_enchant19").getAsFloat(), enchCrippling),
			obOp("op_enchants7", oData.get("final_enchant20").getAsFloat(), enchInstability),
			obOp("op_enchants8", oData.get("final_enchant21").getAsFloat(), unbreaking),
			obOp("op_enchants8", oData.get("final_enchant22").getAsFloat(), unbreakable),
			obOp("op_enchants8", oData.get("final_enchant23").getAsFloat(), mending));
	}

	private float calcArmorValue(ItemStack item, Slot slot, float[] spreadsheetValues, JsonObject oData, Region region) {
		float attrArmor = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.ARMOR, Operation.ADD, slot);
		float attrAgility = (float) ItemStatUtils.getAttributeAmount(item, AttributeType.AGILITY, Operation.ADD, slot);

		spreadsheetValues[0] = attrArmor;
		spreadsheetValues[1] = attrAgility;

		int enchInure = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INURE);
		int enchShielding = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SHIELDING);
		int enchSteadfast = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STEADFAST);
		int enchPoise = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.POISE);
		int enchGuard = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.GUARD);
		int enchReflexes = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.REFLEXES);
		int enchTempo = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.TEMPO);
		int enchEvasion = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.EVASION);
		int enchEthereal = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ETHEREAL);
		int enchCloaked = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CLOAKED);

		spreadsheetValues[43] = enchShielding;
		spreadsheetValues[44] = enchInure;
		spreadsheetValues[45] = enchSteadfast;
		spreadsheetValues[46] = enchPoise;
		spreadsheetValues[47] = enchGuard;
		spreadsheetValues[48] = enchTempo;
		spreadsheetValues[49] = enchReflexes;
		spreadsheetValues[50] = enchEvasion;
		spreadsheetValues[51] = enchEthereal;
		spreadsheetValues[52] = enchCloaked;

		// SITUATIONAL DEFENSE VALUE ----------------------------------------------------------------------------------------------------
		String regionString = region == Region.VALLEY ? "v" : region == Region.ISLES ? "i" : "r";
		float armorScore =
			obOp("op_situational",
				oData.get("armor").getAsFloat() * attrArmor,
				oData.get("inure" + regionString).getAsFloat() * enchInure,
				oData.get("shielding" + regionString).getAsFloat() * enchShielding,
				oData.get("steadfast" + regionString).getAsFloat() * enchSteadfast,
				oData.get("poise" + regionString).getAsFloat() * enchPoise,
				oData.get("guard" + regionString).getAsFloat() * enchGuard);
		float agilityScore =
			obOp("op_situational",
				oData.get("agility").getAsFloat() * attrAgility,
				oData.get("reflexes" + regionString).getAsFloat() * enchReflexes,
				oData.get("tempo" + regionString).getAsFloat() * enchTempo,
				oData.get("evasion" + regionString).getAsFloat() * enchEvasion,
				oData.get("ethereal" + regionString).getAsFloat() * enchEthereal,
				oData.get("cloaked" + regionString).getAsFloat() * enchCloaked);

		// PROT AND ARMOR THINGS --------------------------------------------------------------------------------------------------------
		if (armorScore > 0 && agilityScore > 0) {
			armorScore = (armorScore + agilityScore) * oData.get("mixed").getAsFloat();
			agilityScore = 0f;
		} else if (armorScore == 0 && agilityScore == 0) {
			return 1f;
		}

		// very obfuscated calculation (what a mess!)
		return obOp("op_armor1", obOp("op_armor2", oData.get("defense_const1").getAsFloat(), oData.get("defense_const2").getAsFloat()),
			obOp("op_armor3", obOp("op_armor4", armorScore, obOp("op_armor5", obOp("op_armor6", armorScore, obOp("op_armor7", armorScore, agilityScore)), obOp("op_armor8", oData.get("defense_const3").getAsFloat(), oData.get("defense_const4").getAsFloat()))),
				obOp("op_armor9", agilityScore, obOp("op_armor10", obOp("op_armor11", agilityScore, obOp("op_armor12", armorScore, agilityScore)), obOp("op_armor13", oData.get("defense_const3").getAsFloat(), oData.get("defense_const4").getAsFloat())))));
	}

	private float calcProtValue(ItemStack item, EnchantmentType prot, EnchantmentType fragility, float[] spreadsheetValues, int spreadsheetIndex, JsonObject oData) {
		int enchEpfProt = ItemStatUtils.getEnchantmentLevel(item, prot) - ItemStatUtils.getEnchantmentLevel(item, fragility);
		spreadsheetValues[spreadsheetIndex] = enchEpfProt;
		return obOp("op_prot1", obOp("op_prot2", oData.get("prot_const1").getAsFloat(), oData.get("prot_const2").getAsFloat()),
			enchEpfProt > 0 ? obOp("op_prot3", oData.get("prot_const3").getAsFloat(), enchEpfProt) : obOp("op_prot4", oData.get("prot_const4").getAsFloat(), enchEpfProt));
	}

	private float getBudget(ItemStack item, JsonObject oData) {
		float[] r3MwBudgets = new float[] {
			oData.get("budget0").getAsFloat(), oData.get("budget1").getAsFloat(),
			oData.get("budget2").getAsFloat(), oData.get("budget3").getAsFloat(),
			oData.get("budget4").getAsFloat(), oData.get("budget5").getAsFloat(),
			oData.get("budget6").getAsFloat(), oData.get("budget7").getAsFloat()
		};
		int mw = MasterworkUtils.getMasterworkAsInt(ItemStatUtils.getMasterwork(item));
		if (mw >= 0 && mw <= 7) {
			return r3MwBudgets[mw];
		} else {
			return 0f;
		}
	}

	private float obOp(String operation, float... floats) {
		if (oData == null) {
			return 0f;
		}
		switch (oData.get(operation).getAsString()) {
			case "sum":
				float sum = 0;
				for (float f : floats) {
					sum += f;
				}
				return sum;
			case "product":
				float product = 1f;
				for (float f : floats) {
					product *= f;
				}
				return product;
			case "divide":
				return floats[0] / floats[1];
			case "pow":
				return (float) Math.pow(floats[0], floats[1]);
			case "log":
				return (float) Math.log(floats[0]) / (float) Math.log(floats[1]);
			default:
				return 0f;
		}
	}
}
