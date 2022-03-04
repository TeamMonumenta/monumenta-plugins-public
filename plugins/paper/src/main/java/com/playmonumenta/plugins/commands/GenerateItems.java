package com.playmonumenta.plugins.commands;

import com.opencsv.CSVReader;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GenerateItems extends GenericCommand {
	public static void register() {
		registerPlayerCommand("generateitems", "monumenta.command.generateitems", GenerateItems::run);
	}

	public static void run(CommandSender sender, Player player) {
		if (((Player) sender).getGameMode() != GameMode.CREATIVE) {
			return;
		}
		List<List<String>> records = new ArrayList<List<String>>();

		String path = Plugin.getInstance().getDataFolder() + File.separator + "item_rework_stats.csv";
		try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8))) {
			String[] values = null;
			while ((values = csvReader.readNext()) != null) {
				records.add(Arrays.asList(values));
			}
		} catch (Exception ex) {
			MessagingUtils.sendStackTrace(sender, ex);
			ex.printStackTrace();
		}
		int itemCounter = 0;
		int chestCounter = 0;
		for (List<String> row : records) {
			for (int i = 12; i < row.size(); i++) {
				if (row.get(i).equals("") || row.get(i) == null) {
					row.set(i, "0");
				}
			}
			// Item Params
			String itemName = row.get(0);
			String baseItem = row.get(1);
			baseItem = baseItem.split(":", 2)[1].toUpperCase();
			Boolean isMainhand = row.get(67).equals("1");
			String slot;
			if (isMainhand) {
				slot = "mainhand";
			} else if (baseItem.equals("LEATHER_HELMET") || baseItem.equals("GOLDEN_HELMET") || baseItem.equals("CHAINMAIL_HELMET")
				|| baseItem.equals("IRON_HELMET") || baseItem.equals("DIAMOND_HELMET") || baseItem.equals("NETHERITE_HELMET")
				|| baseItem.equals("PLAYER_HEAD") || baseItem.equals("CREEPER_HEAD") || baseItem.equals("TURTLE_HELMET")) {
				slot = "head";
			} else if (baseItem.equals("LEATHER_CHESTPLATE") || baseItem.equals("GOLDEN_CHESTPLATE") || baseItem.equals("CHAINMAIL_CHESTPLATE")
				|| baseItem.equals("IRON_CHESTPLATE") || baseItem.equals("DIAMOND_CHESTPLATE") || baseItem.equals("NETHERITE_CHESTPLATE")) {
				slot = "chest";
			} else if (baseItem.equals("LEATHER_LEGGINGS") || baseItem.equals("GOLDEN_LEGGINGS") || baseItem.equals("CHAINMAIL_LEGGINGS")
				|| baseItem.equals("IRON_LEGGINGS") || baseItem.equals("DIAMOND_LEGGINGS") || baseItem.equals("NETHERITE_LEGGINGS")) {
				slot = "legs";
			} else if (baseItem.equals("LEATHER_BOOTS") || baseItem.equals("GOLDEN_BOOTS") || baseItem.equals("CHAINMAIL_BOOTS")
				|| baseItem.equals("IRON_BOOTS") || baseItem.equals("DIAMOND_BOOTS") || baseItem.equals("NETHERITE_BOOTS")) {
				slot = "feet";
			} else {
				slot = "offhand";
			}
			String region = row.get(2);
			if (region.equals("1")) {
				region = "valley";
			} else if (region.equals("2")) {
				region = "isles";
			} else if (region.equals("3")) {
				region = "ring";
			} else {
				region = "none";
			}
			String tier = row.get(3).toLowerCase();
			String nameColor = row.get(4);
			if (nameColor == null || nameColor.equals("")) {
				nameColor = "none";
			}
			String location = row.get(5);
			if (location == null || location.equals("")) {
				location = "none";
			}
			String[] loreText = new String[6];
			if (row.get(68).equals("1")) {
				loreText[0] = row.get(6).replace('_', ',');
				loreText[1] = row.get(7).replace('_', ',');
				loreText[2] = row.get(8).replace('_', ',');
				loreText[3] = row.get(9).replace('_', ',');
				loreText[4] = row.get(10).replace('_', ',');
				loreText[5] = row.get(11).replace('_', ',');
			} else {
				String fullText = "";
				for (int i = 0; i < 6; i++) {
					if (row.get(6 + i).equals("") || row.get(6 + i).equals("-") || row.get(6 + i) == null) {
						continue;
					} else {
						fullText += " ";
						fullText += row.get(6 + i).replace('_', ',');
						fullText = fullText.replace("  ", " ");
						int fullLength = fullText.length();
						int numLines = Math.min((fullLength / 40) + 1, 6);
						int charPerLine = fullLength / numLines;
						String[] splitStr = fullText.trim().split("\\s+");
						int currLineLength = 0;
						String currLine = "";
						int j = 0;
						for (String word : splitStr) {
							if (currLineLength + word.length() > charPerLine) {
								currLine = currLine + " " + word;
								loreText[j] = currLine.trim();
								j++;
								currLine = "";
								currLineLength = 0;
							} else {
								currLine = currLine + " " + word;
								currLineLength = currLine.length();
							}
						}
						loreText[j] = currLine.trim();
					}
				}
			}
			// Base Defense
			double armorStat = Double.parseDouble(row.get(13));
			double agilStat = Double.parseDouble(row.get(14));
			// Protections
			int meleeProt = Integer.parseInt(row.get(15));
			int projProt = Integer.parseInt(row.get(16));
			int magicProt = Integer.parseInt(row.get(18));
			int blastProt = Integer.parseInt(row.get(17));
			int fireProt = Integer.parseInt(row.get(19));
			int featherProt = Integer.parseInt(row.get(20));
			// Attributes
			double healthAttr = Double.parseDouble(row.get(21));
			double pHealthAttr = Double.parseDouble(row.get(22)) / 100.0;
			double speedAttr = Double.parseDouble(row.get(23));
			double pSpeedAttr = Double.parseDouble(row.get(24)) / 100.0;
			double atksAttr = Double.parseDouble(row.get(25));
			double pAtksAttr = Double.parseDouble(row.get(26)) / 100.0;
			double meleeAttr = Double.parseDouble(row.get(27));
			double pMeleeAttr = Double.parseDouble(row.get(28)) / 100.0;
			double pPrjsAttr = Double.parseDouble(row.get(29)) / 100.0;
			double pProjAttr = Double.parseDouble(row.get(30)) / 100.0;
			double magicAttr = Double.parseDouble(row.get(31));
			double pMagicAttr = Double.parseDouble(row.get(32)) / 100.0;
			double thornsAttr = Double.parseDouble(row.get(34));
			double kbrAttr = Double.parseDouble(row.get(33)) / 10;
			// Enchantments
			int aquaAffinity = Integer.parseInt(row.get(35));
			int respiration = Integer.parseInt(row.get(36));
			int depthStrider = Integer.parseInt(row.get(37));
			int gills = Integer.parseInt(row.get(38));
			int regen = Integer.parseInt(row.get(39));
			int lifeDrain = Integer.parseInt(row.get(40));
			int sapper = Integer.parseInt(row.get(41));
			int sustenance = Integer.parseInt(row.get(42));
			int inferno = Integer.parseInt(row.get(43));
			int adrenaline = Integer.parseInt(row.get(44));
			int regicide = Integer.parseInt(row.get(45));
			int crippling = Integer.parseInt(row.get(46));
			int secondWind = Integer.parseInt(row.get(47));
			int triage = Integer.parseInt(row.get(48));
			int aptitude = Integer.parseInt(row.get(49));
			int ineptitude = Integer.parseInt(row.get(50));
			int intuition = Integer.parseInt(row.get(52));
			int vanishing = Integer.parseInt(row.get(61));
			int corruption = Integer.parseInt(row.get(62));
			int retrieval = Integer.parseInt(row.get(105));
			int weightless = Integer.parseInt(row.get(106));
			int voidTether = Integer.parseInt(row.get(119));
			int resurrection = Integer.parseInt(row.get(120));
			//int festive = Integer.parseInt(row.get(122)); DO LATER
			int divineAura = Integer.parseInt(row.get(124));
			int material = Integer.parseInt(row.get(126));
			int irrepairibiliy = Integer.parseInt(row.get(125));
			// Defense Mod Enchants
			int adaptability = Integer.parseInt(row.get(51));
			int shielding = Integer.parseInt(row.get(53));
			int inure = Integer.parseInt(row.get(54));
			int steadfast = Integer.parseInt(row.get(55));
			int poise = Integer.parseInt(row.get(56));
			int tempo = Integer.parseInt(row.get(57));
			int reflexes = Integer.parseInt(row.get(58));
			int evasion = Integer.parseInt(row.get(59));
			int ethereal = Integer.parseInt(row.get(60));
			// Durability
			int unbreaking = Integer.parseInt(row.get(63));
			int mending = Integer.parseInt(row.get(64));
			int unbreakable = Integer.parseInt(row.get(65));
			// Mainhand Stats
			double mainhandAttackDamage = Double.parseDouble(row.get(69));
			double mainhandAttackSpeed = Double.parseDouble(row.get(70));
			double spellPower = Double.parseDouble(row.get(74)) / 100.0;
			int wand = Integer.parseInt(row.get(75));
			double mainhandProjDamage = Double.parseDouble(row.get(71));
			double mainhandProjSpeed = Double.parseDouble(row.get(72));
			double throwRate = Double.parseDouble(row.get(73));
			// Enchants
			int sweepingEdge = Integer.parseInt(row.get(76));
			int arcaneThrust = Integer.parseInt(row.get(77));
			int knockback = Integer.parseInt(row.get(78));
			int looting = Integer.parseInt(row.get(79));
			int fireAspect = Integer.parseInt(row.get(80));
			int iceAspect = Integer.parseInt(row.get(81));
			int thunderAspect = Integer.parseInt(row.get(82));
			int decay = Integer.parseInt(row.get(83));
			int bleeding = Integer.parseInt(row.get(84));
			int smite = Integer.parseInt(row.get(85));
			int slayer = Integer.parseInt(row.get(86));
			int duelist = Integer.parseInt(row.get(87));
			int abyssal = Integer.parseInt(row.get(88));
			int chaotic = Integer.parseInt(row.get(89));
			int hexEater = Integer.parseInt(row.get(90));
			int twoHanded = Integer.parseInt(row.get(91));
			int quake = Integer.parseInt(row.get(92));
			int ephemerality = Integer.parseInt(row.get(93));
			int pointBlank = Integer.parseInt(row.get(94));
			int sniper = Integer.parseInt(row.get(95));
			int punch = Integer.parseInt(row.get(96));
			int flame = Integer.parseInt(row.get(97));
			int frost = Integer.parseInt(row.get(98));
			int spark = Integer.parseInt(row.get(99));
			int quickCharge = Integer.parseInt(row.get(100));
			int piercing = Integer.parseInt(row.get(101));
			int multishot = Integer.parseInt(row.get(102));
			int infinity = Integer.parseInt(row.get(103));
			int recoil = Integer.parseInt(row.get(104));
			int riptide = Integer.parseInt(row.get(107));
			int efficiency = Integer.parseInt(row.get(108));
			int silkTouch = Integer.parseInt(row.get(109));
			int fortune = Integer.parseInt(row.get(110));
			int eruption = Integer.parseInt(row.get(111));
			int shrapnel = Integer.parseInt(row.get(112));
			int multitool = Integer.parseInt(row.get(113));
			int luckOfTheSea = Integer.parseInt(row.get(114));
			int lure = Integer.parseInt(row.get(115));
			int ashes = Integer.parseInt(row.get(116));
			int jungles = Integer.parseInt(row.get(117));
			int rageKeter = Integer.parseInt(row.get(118));
			int depths = Integer.parseInt(row.get(121));

			// Logic for item creation
			ItemStack item = new ItemStack(Material.valueOf(baseItem));
			player.getEquipment().setItemInMainHand(item, true);
			player.updateInventory();

			if (tier.equals("0") || tier.equals("1") || tier.equals("2") || tier.equals("3") || tier.equals("4") || (tier.equals("legacy") && nameColor.equals("none"))) {
				player.chat("/editname " + player.getDisplayName() + " white " + "false" + " false " + itemName);
			} else if (tier.equals("5")) {
				player.chat("/editname " + player.getDisplayName() + " lime " + "false" + " false " + itemName);
			} else if (tier.equals("epic")) {
				player.chat("/editname " + player.getDisplayName() + " " + nameColor + " " + "true" + " true " + itemName);
			} else {
				player.chat("/editname " + player.getDisplayName() + " " + nameColor + " " + "true" + " false " + itemName);
			}

			// Logic for commands to run
			// Item Info
			player.chat("/editinfo " + player.getDisplayName() + " " + region + " " + tier + " " + location);
			// Lore Text
			for (int i = 0; i < 4; i++) {
				String line = loreText[i];
				if (line != null && !line.equals("") && !line.equals("-")) {
					player.chat("/editlore " + player.getDisplayName() + " add " + Integer.toString(i) + " " + line);
				}
			}
			// Defense
			if (armorStat != 0) {
				player.chat("/editattr " + player.getDisplayName() + " Armor " + Double.toString(armorStat) + " add " + slot);
			}
			if (agilStat != 0) {
				player.chat("/editattr " + player.getDisplayName() + " Agility " + Double.toString(agilStat) + " add " + slot);
			}
			// Attributes
			if (healthAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " MaxHealth " + Double.toString(healthAttr) + " add " + slot);
			}
			if (pHealthAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " MaxHealth " + Double.toString(pHealthAttr) + " multiply " + slot);
			}
			if (speedAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " Speed " + Double.toString(speedAttr) + " add " + slot);
			}
			if (pSpeedAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " Speed " + Double.toString(pSpeedAttr) + " multiply " + slot);
			}
			if (atksAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " AttackSpeed " + Double.toString(atksAttr) + " add " + slot);
			}
			if (pAtksAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " AttackSpeed " + Double.toString(pAtksAttr) + " multiply " + slot);
			}
			if (meleeAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " AttackDamageAdd " + Double.toString(meleeAttr) + " add " + slot);
			}
			if (pMeleeAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " AttackDamageMultiply " + Double.toString(pMeleeAttr) + " multiply " + slot);
			}
			if (magicAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " MagicDamageAdd " + Double.toString(magicAttr) + " add " + slot);
			}
			if (pMagicAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " MagicDamageMultiply " + Double.toString(pMagicAttr) + " multiply " + slot);
			}
			if (pProjAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " ProjectileDamageMultiply " + Double.toString(pProjAttr) + " multiply " + slot);
			}
			if (pPrjsAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " ProjectileSpeed " + Double.toString(pPrjsAttr) + " multiply " + slot);
			}
			if (thornsAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " ThornsDamage " + Double.toString(thornsAttr) + " add " + slot);
			}
			if (kbrAttr != 0) {
				player.chat("/editattr " + player.getDisplayName() + " KnockbackResistance " + Double.toString(kbrAttr) + " add " + slot);
			}
			// Enchants
			if (meleeProt != 0) {
				player.chat("/editench " + player.getDisplayName() + " MeleeProtection " + Integer.toString(meleeProt));
			}
			if (projProt != 0) {
				player.chat("/editench " + player.getDisplayName() + " ProjectileProtection " + Integer.toString(projProt));
			}
			if (magicProt != 0) {
				player.chat("/editench " + player.getDisplayName() + " MagicProtection " + Integer.toString(magicProt));
			}
			if (blastProt != 0) {
				player.chat("/editench " + player.getDisplayName() + " BlastProtection " + Integer.toString(blastProt));
			}
			if (fireProt != 0) {
				player.chat("/editench " + player.getDisplayName() + " FireProtection " + Integer.toString(fireProt));
			}
			if (featherProt != 0) {
				player.chat("/editench " + player.getDisplayName() + " FeatherFalling " + Integer.toString(featherProt));
			}
			if (aquaAffinity != 0) {
				player.chat("/editench " + player.getDisplayName() + " AquaAffinity " + Integer.toString(aquaAffinity));
			}
			if (respiration != 0) {
				player.chat("/editench " + player.getDisplayName() + " Respiration " + Integer.toString(respiration));
			}
			if (depthStrider != 0) {
				player.chat("/editench " + player.getDisplayName() + " DepthStrider " + Integer.toString(depthStrider));
			}
			if (gills != 0) {
				player.chat("/editench " + player.getDisplayName() + " Gills " + Integer.toString(gills));
			}
			if (regen != 0) {
				player.chat("/editench " + player.getDisplayName() + " Regeneration " + Integer.toString(regen));
			}
			if (lifeDrain != 0) {
				player.chat("/editench " + player.getDisplayName() + " LifeDrain " + Integer.toString(lifeDrain));
			}
			if (sapper != 0) {
				player.chat("/editench " + player.getDisplayName() + " Sapper " + Integer.toString(sapper));
			}
			if (sustenance > 0) {
				player.chat("/editench " + player.getDisplayName() + " Sustenance " + Integer.toString(Math.abs(sustenance)));
			} else if (sustenance < 0) {
				player.chat("/editench " + player.getDisplayName() + " CurseofAnemia " + Integer.toString(Math.abs(sustenance)));
			}
			if (inferno > 0) {
				player.chat("/editench " + player.getDisplayName() + " Inferno " + Integer.toString(inferno));
			}
			if (adrenaline > 0) {
				player.chat("/editench " + player.getDisplayName() + " Adrenaline " + Integer.toString(adrenaline));
			}
			if (regicide > 0) {
				player.chat("/editench " + player.getDisplayName() + " Regicide " + Integer.toString(regicide));
			}
			if (abyssal != 0) {
				player.chat("/editench " + player.getDisplayName() + " Abyssal " + Integer.toString(abyssal));
			}
			if (retrieval > 0) {
				player.chat("/editench " + player.getDisplayName() + " Retrieval " + Integer.toString(retrieval));
			}
			if (weightless > 0) {
				player.chat("/editench " + player.getDisplayName() + " Weightless " + Integer.toString(weightless));
			}
			if (crippling > 0) {
				player.chat("/editench " + player.getDisplayName() + " CurseofCrippling " + Integer.toString(crippling));
			}
			if (secondWind > 0) {
				player.chat("/editench " + player.getDisplayName() + " SecondWind " + Integer.toString(secondWind));
			}
			if (triage > 0) {
				player.chat("/editench " + player.getDisplayName() + " Triage " + Integer.toString(triage));
			}
			if (aptitude > 0) {
				player.chat("/editench " + player.getDisplayName() + " Aptitude " + Integer.toString(aptitude));
			}
			if (ineptitude > 0) {
				player.chat("/editench " + player.getDisplayName() + " Ineptitude " + Integer.toString(ineptitude));
			}
			if (intuition > 0) {
				player.chat("/editench " + player.getDisplayName() + " Intuition " + Integer.toString(intuition));
			}
			if (divineAura > 0) {
				player.chat("/editench " + player.getDisplayName() + " DivineAura " + Integer.toString(divineAura));
			}
			if (vanishing > 0) {
				player.chat("/editench " + player.getDisplayName() + " CurseofVanishing " + Integer.toString(vanishing));
			}
			if (corruption > 0) {
				player.chat("/editench " + player.getDisplayName() + " CurseofCorruption " + Integer.toString(corruption));
			}
			if (ephemerality > 0) {
				player.chat("/editench " + player.getDisplayName() + " CurseofEphemerality " + Integer.toString(ephemerality));
			}
			if (voidTether > 0) {
				player.chat("/editench " + player.getDisplayName() + " VoidTether " + Integer.toString(voidTether));
			}
			if (resurrection > 0) {
				player.chat("/editench " + player.getDisplayName() + " Resurrection " + Integer.toString(resurrection));
			}
			if (irrepairibiliy > 0) {
				player.chat("/editench " + player.getDisplayName() + " CurseofIrreparability " + Integer.toString(irrepairibiliy));
			}
			if (adaptability > 0) {
				player.chat("/editench " + player.getDisplayName() + " Adaptability " + Integer.toString(adaptability));
			}
			if (shielding > 0) {
				player.chat("/editench " + player.getDisplayName() + " Shielding " + Integer.toString(shielding));
			}
			if (inure > 0) {
				player.chat("/editench " + player.getDisplayName() + " Inure " + Integer.toString(inure));
			}
			if (steadfast > 0) {
				player.chat("/editench " + player.getDisplayName() + " Steadfast " + Integer.toString(steadfast));
			}
			if (poise > 0) {
				player.chat("/editench " + player.getDisplayName() + " Poise " + Integer.toString(poise));
			}
			if (tempo > 0) {
				player.chat("/editench " + player.getDisplayName() + " Tempo " + Integer.toString(tempo));
			}
			if (reflexes > 0) {
				player.chat("/editench " + player.getDisplayName() + " Reflexes " + Integer.toString(reflexes));
			}
			if (evasion > 0) {
				player.chat("/editench " + player.getDisplayName() + " Evasion " + Integer.toString(evasion));
			}
			if (ethereal > 0) {
				player.chat("/editench " + player.getDisplayName() + " Ethereal " + Integer.toString(ethereal));
			}
			if (material > 0) {
				player.chat("/editench " + player.getDisplayName() + " Material " + Integer.toString(material));
			}
			if (unbreaking > 0) {
				player.chat("/editench " + player.getDisplayName() + " Unbreaking " + Integer.toString(unbreaking));
			}
			if (mending > 0) {
				player.chat("/editench " + player.getDisplayName() + " Mending " + Integer.toString(mending));
			}
			if (unbreakable > 0) {
				player.chat("/editench " + player.getDisplayName() + " Unbreakable " + Integer.toString(unbreakable));
			}

			if (isMainhand) {
				// Mainhand Stats
				if (mainhandAttackDamage > 0) {
					player.chat("/editattr " + player.getDisplayName() + " AttackDamageAdd " + Double.toString(mainhandAttackDamage - 1) + " add mainhand");
				}
				if (mainhandAttackSpeed > 0) {
					player.chat("/editattr " + player.getDisplayName() + " AttackSpeed " + Double.toString(mainhandAttackSpeed - 4) + " add mainhand");
				}
				if (mainhandProjDamage > 0) {
					player.chat("/editattr " + player.getDisplayName() + " ProjectileDamageAdd " + Double.toString(mainhandProjDamage) + " add mainhand");
				}
				if (mainhandProjSpeed > 0) {
					player.chat("/editattr " + player.getDisplayName() + " ProjectileSpeed " + Double.toString(mainhandProjSpeed) + " multiply mainhand");
				}
				if (throwRate > 0) {
					player.chat("/editattr " + player.getDisplayName() + " ThrowRate " + Double.toString(throwRate) + " add mainhand");
				}
				if (spellPower > 0) {
					player.chat("/editattr " + player.getDisplayName() + " SpellPower " + Double.toString(spellPower) + " multiply mainhand");
					player.chat("/editench " + player.getDisplayName() + " MagicWand 1");
				}
				// Enchants
				if (sweepingEdge != 0) {
					player.chat("/editench " + player.getDisplayName() + " SweepingEdge " + Integer.toString(sweepingEdge));
				}
				if (arcaneThrust != 0) {
					player.chat("/editench " + player.getDisplayName() + " ArcaneThrust " + Integer.toString(arcaneThrust));
				}
				if (knockback != 0) {
					player.chat("/editench " + player.getDisplayName() + " Knockback " + Integer.toString(knockback));
				}
				if (looting != 0) {
					player.chat("/editench " + player.getDisplayName() + " Looting " + Integer.toString(looting));
				}
				if (fireAspect != 0) {
					player.chat("/editench " + player.getDisplayName() + " FireAspect " + Integer.toString(fireAspect));
				}
				if (iceAspect != 0) {
					player.chat("/editench " + player.getDisplayName() + " IceAspect " + Integer.toString(iceAspect));
				}
				if (thunderAspect != 0) {
					player.chat("/editench " + player.getDisplayName() + " ThunderAspect " + Integer.toString(thunderAspect));
				}
				if (decay != 0) {
					player.chat("/editench " + player.getDisplayName() + " Decay " + Integer.toString(decay));
				}
				if (bleeding != 0) {
					player.chat("/editench " + player.getDisplayName() + " Bleeding " + Integer.toString(bleeding));
				}
				if (smite != 0) {
					player.chat("/editench " + player.getDisplayName() + " Smite " + Integer.toString(smite));
				}
				if (slayer != 0) {
					player.chat("/editench " + player.getDisplayName() + " Slayer " + Integer.toString(slayer));
				}
				if (duelist != 0) {
					player.chat("/editench " + player.getDisplayName() + " Duelist " + Integer.toString(duelist));
				}
				if (hexEater > 0) {
					player.chat("/editench " + player.getDisplayName() + " HexEater " + Integer.toString(hexEater));
				}
				if (chaotic > 0) {
					player.chat("/editench " + player.getDisplayName() + " Chaotic " + Integer.toString(chaotic));
				}
				if (twoHanded > 0) {
					player.chat("/editench " + player.getDisplayName() + " TwoHanded " + Integer.toString(twoHanded));
				}
				if (quake > 0) {
					player.chat("/editench " + player.getDisplayName() + " Quake " + Integer.toString(quake));
				}
				if (pointBlank > 0) {
					player.chat("/editench " + player.getDisplayName() + " PointBlank " + Integer.toString(pointBlank));
				}
				if (sniper > 0) {
					player.chat("/editench " + player.getDisplayName() + " Sniper " + Integer.toString(sniper));
				}
				if (punch > 0) {
					player.chat("/editench " + player.getDisplayName() + " Punch " + Integer.toString(punch));
				}
				if (flame > 0) {
					player.chat("/editench " + player.getDisplayName() + " Flame " + Integer.toString(flame));
				}
				if (frost > 0) {
					player.chat("/editench " + player.getDisplayName() + " Frost " + Integer.toString(frost));
				}
				if (spark > 0) {
					player.chat("/editench " + player.getDisplayName() + " Spark " + Integer.toString(spark));
				}
				if (recoil > 0) {
					player.chat("/editench " + player.getDisplayName() + " Recoil " + Integer.toString(recoil));
				}
				if (quickCharge > 0) {
					player.chat("/editench " + player.getDisplayName() + " QuickCharge " + Integer.toString(quickCharge));
				}
				if (multishot > 0) {
					player.chat("/editench " + player.getDisplayName() + " Multishot " + Integer.toString(multishot));
				}
				if (piercing > 0) {
					player.chat("/editench " + player.getDisplayName() + " Piercing " + Integer.toString(piercing));
				}
				if (infinity > 0) {
					player.chat("/editench " + player.getDisplayName() + " Infinity " + Integer.toString(infinity));
				}
				if (riptide > 0) {
					player.chat("/editench " + player.getDisplayName() + " Riptide " + Integer.toString(riptide));
				}
				if (efficiency > 0) {
					player.chat("/editench " + player.getDisplayName() + " Efficiency " + Integer.toString(efficiency));
				}
				if (fortune > 0) {
					player.chat("/editench " + player.getDisplayName() + " Fortune " + Integer.toString(fortune));
				}
				if (silkTouch > 0) {
					player.chat("/editench " + player.getDisplayName() + " SilkTouch " + Integer.toString(silkTouch));
				}
				if (eruption > 0) {
					player.chat("/editench " + player.getDisplayName() + " Eruption " + Integer.toString(eruption));
				}
				if (shrapnel > 0) {
					player.chat("/editench " + player.getDisplayName() + " CurseofShrapnel " + Integer.toString(shrapnel));
				}
				if (multitool > 0) {
					player.chat("/editench " + player.getDisplayName() + " Multitool " + Integer.toString(multitool));
				}
				if (luckOfTheSea > 0) {
					player.chat("/editench " + player.getDisplayName() + " LuckOfTheSea " + Integer.toString(luckOfTheSea));
				}
				if (lure > 0) {
					player.chat("/editench " + player.getDisplayName() + " Lure " + Integer.toString(lure));
				}
				if (ashes > 0) {
					player.chat("/editench " + player.getDisplayName() + " AshesofEternity " + Integer.toString(ashes));
				}
				if (jungles > 0) {
					player.chat("/editench " + player.getDisplayName() + " JunglesNourishment " + Integer.toString(jungles));
				}
				if (depths > 0) {
					player.chat("/editench " + player.getDisplayName() + " ProtectionoftheDepths " + Integer.toString(depths));
				}
				if (rageKeter > 0) {
					player.chat("/editench " + player.getDisplayName() + " RageoftheKeter " + Integer.toString(rageKeter));
				}
				if (wand > 0) {
					player.chat("/editench" + player.getDisplayName() + " MagicWand 1");
				}
				player.chat("/editench " + player.getDisplayName() + " MainhandOffhandDisable 1");
			} else {
				player.chat("/editench " + player.getDisplayName() + " OffhandMainhandDisable 1");
			}
			// Logic for inventory manipulation and chest creation
			ItemStack itemToInventory = player.getEquipment().getItemInMainHand();
			player.getInventory().remove(itemToInventory);
			player.getInventory().setItem(9 + itemCounter, itemToInventory);
			player.updateInventory();
			itemCounter++;
			if (itemCounter == 27) {
				itemCounter = 0;
				chestCounter++;
				player.getLocation().add(chestCounter, 0, 0).getBlock().setType(Material.CHEST);
				Chest chest = (Chest) player.getLocation().add(chestCounter, 0, 0).getBlock().getState();
				for (int i = 9; i < 36; i++) {
					chest.getInventory().setItem(i - 9, player.getInventory().getItem(i));
				}
			}
		}
	}
}
