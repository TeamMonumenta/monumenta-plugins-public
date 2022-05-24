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
			String masterwork = row.get(4);
			String nameColor = row.get(5);
			if (nameColor == null || nameColor.equals("")) {
				nameColor = "none";
			}
			String location = row.get(6);
			if (location == null || location.equals("")) {
				location = "none";
			}
			String[] loreText = new String[6];
			if (row.get(69).equals("1")) {
				loreText[0] = row.get(7).replace('_', ',');
				loreText[1] = row.get(8).replace('_', ',');
				loreText[2] = row.get(9).replace('_', ',');
				loreText[3] = row.get(10).replace('_', ',');
				loreText[4] = row.get(11).replace('_', ',');
				loreText[5] = row.get(12).replace('_', ',');
			} else {
				String fullText = "";
				for (int i = 0; i < 6; i++) {
					if (row.get(7 + i).equals("") || row.get(7 + i) == null || row.get(7 + i).length() <= 1) {
						continue;
					} else {
						fullText += " ";
						fullText += row.get(7 + i).replace('_', ',');
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
			double armorStat = Double.parseDouble(row.get(14));
			double agilStat = Double.parseDouble(row.get(15));
			// Protections
			int meleeProt = Integer.parseInt(row.get(16));
			int projProt = Integer.parseInt(row.get(17));
			int blastProt = Integer.parseInt(row.get(18));
			int magicProt = Integer.parseInt(row.get(19));
			int fireProt = Integer.parseInt(row.get(20));
			int featherProt = Integer.parseInt(row.get(21));
			// Attributes
			double healthAttr = Double.parseDouble(row.get(22));
			double pHealthAttr = Double.parseDouble(row.get(23)) / 100.0;
			double speedAttr = Double.parseDouble(row.get(24));
			double pSpeedAttr = Double.parseDouble(row.get(25)) / 100.0;
			double atksAttr = Double.parseDouble(row.get(26));
			double pAtksAttr = Double.parseDouble(row.get(27)) / 100.0;
			double meleeAttr = Double.parseDouble(row.get(28));
			double pMeleeAttr = Double.parseDouble(row.get(29)) / 100.0;
			double pPrjsAttr = Double.parseDouble(row.get(30)) / 100.0;
			double pProjAttr = Double.parseDouble(row.get(31)) / 100.0;
			double magicAttr = Double.parseDouble(row.get(32));
			double pMagicAttr = Double.parseDouble(row.get(33)) / 100.0;
			double kbrAttr = Double.parseDouble(row.get(34)) / 10;
			double thornsAttr = Double.parseDouble(row.get(35));
			// Enchantments
			int aquaAffinity = Integer.parseInt(row.get(36));
			int respiration = Integer.parseInt(row.get(37));
			int depthStrider = Integer.parseInt(row.get(38));
			int gills = Integer.parseInt(row.get(39));
			int regen = Integer.parseInt(row.get(40));
			int lifeDrain = Integer.parseInt(row.get(41));
			int sapper = Integer.parseInt(row.get(42));
			int sustenance = Integer.parseInt(row.get(43));
			int inferno = Integer.parseInt(row.get(44));
			int adrenaline = Integer.parseInt(row.get(45));
			int regicide = Integer.parseInt(row.get(46));
			int crippling = Integer.parseInt(row.get(47));
			int secondWind = Integer.parseInt(row.get(48));
			int triage = Integer.parseInt(row.get(49));
			int aptitude = Integer.parseInt(row.get(50));
			int ineptitude = Integer.parseInt(row.get(51));
			int intuition = Integer.parseInt(row.get(53));
			int firstStrike = Integer.parseInt(row.get(54));
			int stamina = Integer.parseInt(row.get(55));
			int trivium = Integer.parseInt(row.get(56));
			int vanishing = Integer.parseInt(row.get(65));
			int corruption = Integer.parseInt(row.get(66));
			int retrieval = Integer.parseInt(row.get(108));
			int weightless = Integer.parseInt(row.get(109));
			int voidTether = Integer.parseInt(row.get(122));
			int resurrection = Integer.parseInt(row.get(123));
			int divineAura = Integer.parseInt(row.get(127));
			int material = Integer.parseInt(row.get(129));
			int irrepairibiliy = Integer.parseInt(row.get(128));
			// Defense Mod Enchants
			int adaptability = Integer.parseInt(row.get(52));
			int shielding = Integer.parseInt(row.get(57));
			int inure = Integer.parseInt(row.get(58));
			int steadfast = Integer.parseInt(row.get(59));
			int poise = Integer.parseInt(row.get(60));
			int tempo = Integer.parseInt(row.get(61));
			int reflexes = Integer.parseInt(row.get(62));
			int evasion = Integer.parseInt(row.get(63));
			int ethereal = Integer.parseInt(row.get(64));
			// Durability
			int unbreaking = Integer.parseInt(row.get(67));
			int mending = Integer.parseInt(row.get(68));
			int unbreakable = Integer.parseInt(row.get(69));
			// Mainhand Stats
			double mainhandAttackDamage = Double.parseDouble(row.get(73));
			double mainhandAttackSpeed = Double.parseDouble(row.get(74));
			double spellPower = Double.parseDouble(row.get(78)) / 100.0;
			int wand = Integer.parseInt(row.get(79));
			double mainhandProjDamage = Double.parseDouble(row.get(75));
			double mainhandProjSpeed = Double.parseDouble(row.get(76));
			double throwRate = Double.parseDouble(row.get(77));
			// Enchants
			int sweepingEdge = Integer.parseInt(row.get(80));
			int arcaneThrust = Integer.parseInt(row.get(81));
			int knockback = Integer.parseInt(row.get(82));
			int looting = Integer.parseInt(row.get(83));
			int fireAspect = Integer.parseInt(row.get(84));
			int iceAspect = Integer.parseInt(row.get(85));
			int thunderAspect = Integer.parseInt(row.get(86));
			int windAspect = Integer.parseInt(row.get(87));
			int earthAspect = Integer.parseInt(row.get(88));
			int decay = Integer.parseInt(row.get(89));
			int bleeding = Integer.parseInt(row.get(90));
			int smite = Integer.parseInt(row.get(91));
			int slayer = Integer.parseInt(row.get(92));
			int duelist = Integer.parseInt(row.get(93));
			int abyssal = Integer.parseInt(row.get(94));
			int chaotic = Integer.parseInt(row.get(95));
			int hexEater = Integer.parseInt(row.get(96));
			int twoHanded = Integer.parseInt(row.get(97));
			int quake = Integer.parseInt(row.get(98));
			int ephemerality = Integer.parseInt(row.get(99));
			int pointBlank = Integer.parseInt(row.get(100));
			int sniper = Integer.parseInt(row.get(101));
			int punch = Integer.parseInt(row.get(102));
			int quickCharge = Integer.parseInt(row.get(103));
			int piercing = Integer.parseInt(row.get(104));
			int multishot = Integer.parseInt(row.get(105));
			int infinity = Integer.parseInt(row.get(106));
			int recoil = Integer.parseInt(row.get(107));
			int riptide = Integer.parseInt(row.get(110));
			int efficiency = Integer.parseInt(row.get(111));
			int silkTouch = Integer.parseInt(row.get(112));
			int fortune = Integer.parseInt(row.get(113));
			int eruption = Integer.parseInt(row.get(114));
			int shrapnel = Integer.parseInt(row.get(115));
			int multitool = Integer.parseInt(row.get(116));
			int luckOfTheSea = Integer.parseInt(row.get(117));
			int lure = Integer.parseInt(row.get(118));
			int ashes = Integer.parseInt(row.get(119));
			int jungles = Integer.parseInt(row.get(120));
			int rageKeter = Integer.parseInt(row.get(121));
			int depths = Integer.parseInt(row.get(124));

			// Logic for item creation
			ItemStack item = new ItemStack(Material.valueOf(baseItem));
			player.getEquipment().setItemInMainHand(item, true);
			player.updateInventory();

			if (tier.equals("0") || tier.equals("1") || tier.equals("2") || (tier.equals("3") && !region.equals("ring")) || tier.equals("4") || (tier.equals("legacy") && nameColor.equals("none"))) {
				player.chat("/editname " + "white " + "false" + " false " + itemName);
			} else if (tier.equals("5") || tier.equals("3")) {
				player.chat("/editname " + "lime " + "false" + " false " + itemName);
			} else if (tier.equals("epic")) {
				player.chat("/editname " + nameColor + " " + "true" + " true " + itemName);
			} else {
				player.chat("/editname " + nameColor + " " + "true" + " false " + itemName);
			}

			// Logic for commands to run
			// Item Info
			player.chat("/editinfo " + region + " " + tier + " " + location + " " + masterwork);
			// Lore Text
			for (int i = 0; i < 4; i++) {
				String line = loreText[i];
				if (line != null && !line.equals("") && !line.equals("-")) {
					player.chat("/editlore" + " add " + Integer.toString(i) + " " + line);
				}
			}
			// Defense
			if (armorStat != 0) {
				player.chat("/editattr" + " Armor " + Double.toString(armorStat) + " add " + slot);
			}
			if (agilStat != 0) {
				player.chat("/editattr" + " Agility " + Double.toString(agilStat) + " add " + slot);
			}
			// Attributes
			if (healthAttr != 0) {
				player.chat("/editattr" + " MaxHealth " + Double.toString(healthAttr) + " add " + slot);
			}
			if (pHealthAttr != 0) {
				player.chat("/editattr" + " MaxHealth " + Double.toString(pHealthAttr) + " multiply " + slot);
			}
			if (speedAttr != 0) {
				player.chat("/editattr" + " Speed " + Double.toString(speedAttr) + " add " + slot);
			}
			if (pSpeedAttr != 0) {
				player.chat("/editattr" + " Speed " + Double.toString(pSpeedAttr) + " multiply " + slot);
			}
			if (atksAttr != 0) {
				player.chat("/editattr" + " AttackSpeed " + Double.toString(atksAttr) + " add " + slot);
			}
			if (pAtksAttr != 0) {
				player.chat("/editattr" + " AttackSpeed " + Double.toString(pAtksAttr) + " multiply " + slot);
			}
			if (meleeAttr != 0) {
				player.chat("/editattr" + " AttackDamageAdd " + Double.toString(meleeAttr) + " add " + slot);
			}
			if (pMeleeAttr != 0) {
				player.chat("/editattr" + " AttackDamageMultiply " + Double.toString(pMeleeAttr) + " multiply " + slot);
			}
			if (magicAttr != 0) {
				player.chat("/editattr" + " MagicDamageAdd " + Double.toString(magicAttr) + " add " + slot);
			}
			if (pMagicAttr != 0) {
				player.chat("/editattr" + " MagicDamageMultiply " + Double.toString(pMagicAttr) + " multiply " + slot);
			}
			if (pProjAttr != 0) {
				player.chat("/editattr" + " ProjectileDamageMultiply " + Double.toString(pProjAttr) + " multiply " + slot);
			}
			if (pPrjsAttr != 0) {
				player.chat("/editattr" + " ProjectileSpeed " + Double.toString(pPrjsAttr) + " multiply " + slot);
			}
			if (thornsAttr != 0) {
				player.chat("/editattr" + " ThornsDamage " + Double.toString(thornsAttr) + " add " + slot);
			}
			if (kbrAttr != 0) {
				player.chat("/editattr" + " KnockbackResistance " + Double.toString(kbrAttr) + " add " + slot);
			}
			// Enchants
			if (meleeProt != 0) {
				player.chat("/editench" + " MeleeProtection " + Integer.toString(meleeProt));
			}
			if (projProt != 0) {
				player.chat("/editench" + " ProjectileProtection " + Integer.toString(projProt));
			}
			if (magicProt != 0) {
				player.chat("/editench" + " MagicProtection " + Integer.toString(magicProt));
			}
			if (blastProt != 0) {
				player.chat("/editench" + " BlastProtection " + Integer.toString(blastProt));
			}
			if (fireProt != 0) {
				player.chat("/editench" + " FireProtection " + Integer.toString(fireProt));
			}
			if (featherProt != 0) {
				player.chat("/editench" + " FeatherFalling " + Integer.toString(featherProt));
			}
			if (aquaAffinity != 0) {
				player.chat("/editench" + " AquaAffinity " + Integer.toString(aquaAffinity));
			}
			if (respiration != 0) {
				player.chat("/editench" + " Respiration " + Integer.toString(respiration));
			}
			if (depthStrider != 0) {
				player.chat("/editench" + " DepthStrider " + Integer.toString(depthStrider));
			}
			if (gills != 0) {
				player.chat("/editench" + " Gills " + Integer.toString(gills));
			}
			if (regen != 0) {
				player.chat("/editench" + " Regeneration " + Integer.toString(regen));
			}
			if (lifeDrain != 0) {
				player.chat("/editench" + " LifeDrain " + Integer.toString(lifeDrain));
			}
			if (sapper != 0) {
				player.chat("/editench" + " Sapper " + Integer.toString(sapper));
			}
			if (sustenance > 0) {
				player.chat("/editench" + " Sustenance " + Integer.toString(Math.abs(sustenance)));
			} else if (sustenance < 0) {
				player.chat("/editench" + " CurseofAnemia " + Integer.toString(Math.abs(sustenance)));
			}
			if (inferno > 0) {
				player.chat("/editench" + " Inferno " + Integer.toString(inferno));
			}
			if (adrenaline > 0) {
				player.chat("/editench" + " Adrenaline " + Integer.toString(adrenaline));
			}
			if (regicide > 0) {
				player.chat("/editench" + " Regicide " + Integer.toString(regicide));
			}
			if (abyssal != 0) {
				player.chat("/editench" + " Abyssal " + Integer.toString(abyssal));
			}
			if (retrieval > 0) {
				player.chat("/editench" + " Retrieval " + Integer.toString(retrieval));
			}
			if (weightless > 0) {
				player.chat("/editench" + " Weightless " + Integer.toString(weightless));
			}
			if (crippling > 0) {
				player.chat("/editench" + " CurseofCrippling " + Integer.toString(crippling));
			}
			if (secondWind > 0) {
				player.chat("/editench" + " SecondWind " + Integer.toString(secondWind));
			}
			if (triage > 0) {
				player.chat("/editench" + " Triage " + Integer.toString(triage));
			}
			if (aptitude > 0) {
				player.chat("/editench" + " Aptitude " + Integer.toString(aptitude));
			}
			if (ineptitude > 0) {
				player.chat("/editench" + " Ineptitude " + Integer.toString(ineptitude));
			}
			if (intuition > 0) {
				player.chat("/editench" + " Intuition " + Integer.toString(intuition));
			}
			if (firstStrike > 0) {
				player.chat("/editench" + " FirstStrike " + Integer.toString(firstStrike));
			}
			if (stamina > 0) {
				player.chat("/editench" + " Stamina " + Integer.toString(stamina));
			}
			if (trivium > 0) {
				player.chat("/editench" + " Trivium " + Integer.toString(trivium));
			}
			if (divineAura > 0) {
				player.chat("/editench" + " DivineAura " + Integer.toString(divineAura));
			}
			if (vanishing > 0) {
				player.chat("/editench" + " CurseofVanishing " + Integer.toString(vanishing));
			}
			if (corruption > 0) {
				player.chat("/editench" + " CurseofCorruption " + Integer.toString(corruption));
			}
			if (ephemerality > 0) {
				player.chat("/editench" + " CurseofEphemerality " + Integer.toString(ephemerality));
			}
			if (voidTether > 0) {
				player.chat("/editench" + " VoidTether " + Integer.toString(voidTether));
			}
			if (resurrection > 0) {
				player.chat("/editench" + " Resurrection " + Integer.toString(resurrection));
			}
			if (irrepairibiliy > 0) {
				player.chat("/editench" + " CurseofIrreparability " + Integer.toString(irrepairibiliy));
			}
			if (adaptability > 0) {
				player.chat("/editench" + " Adaptability " + Integer.toString(adaptability));
			}
			if (shielding > 0) {
				player.chat("/editench" + " Shielding " + Integer.toString(shielding));
			}
			if (inure > 0) {
				player.chat("/editench" + " Inure " + Integer.toString(inure));
			}
			if (steadfast > 0) {
				player.chat("/editench" + " Steadfast " + Integer.toString(steadfast));
			}
			if (poise > 0) {
				player.chat("/editench" + " Poise " + Integer.toString(poise));
			}
			if (tempo > 0) {
				player.chat("/editench" + " Tempo " + Integer.toString(tempo));
			}
			if (reflexes > 0) {
				player.chat("/editench" + " Reflexes " + Integer.toString(reflexes));
			}
			if (evasion > 0) {
				player.chat("/editench" + " Evasion " + Integer.toString(evasion));
			}
			if (ethereal > 0) {
				player.chat("/editench" + " Ethereal " + Integer.toString(ethereal));
			}
			if (material > 0) {
				player.chat("/editench" + " Material " + Integer.toString(material));
			}
			if (unbreaking > 0) {
				player.chat("/editench" + " Unbreaking " + Integer.toString(unbreaking));
			}
			if (mending > 0) {
				player.chat("/editench" + " Mending " + Integer.toString(mending));
			}
			if (unbreakable > 0) {
				player.chat("/editench" + " Unbreakable " + Integer.toString(unbreakable));
			}

			if (isMainhand) {
				// Mainhand Stats
				if (mainhandAttackDamage > 0) {
					player.chat("/editattr " + " AttackDamageAdd " + Double.toString(mainhandAttackDamage - 1) + " add mainhand");
				}
				if (mainhandAttackSpeed > 0) {
					player.chat("/editattr " + " AttackSpeed " + Double.toString(mainhandAttackSpeed - 4) + " add mainhand");
				}
				if (mainhandProjDamage > 0) {
					player.chat("/editattr " + " ProjectileDamageAdd " + Double.toString(mainhandProjDamage) + " add mainhand");
				}
				if (mainhandProjSpeed > 0) {
					player.chat("/editattr " + " ProjectileSpeed " + Double.toString(mainhandProjSpeed) + " multiply mainhand");
				}
				if (throwRate > 0) {
					player.chat("/editattr " + " ThrowRate " + Double.toString(throwRate) + " add mainhand");
				}
				if (spellPower > 0) {
					player.chat("/editattr " + " SpellPower " + Double.toString(spellPower) + " multiply mainhand");
					player.chat("/editench" + " MagicWand 1");
				}
				// Enchants
				if (sweepingEdge != 0) {
					player.chat("/editench" + " SweepingEdge " + Integer.toString(sweepingEdge));
				}
				if (arcaneThrust != 0) {
					player.chat("/editench" + " ArcaneThrust " + Integer.toString(arcaneThrust));
				}
				if (knockback != 0) {
					player.chat("/editench" + " Knockback " + Integer.toString(knockback));
				}
				if (looting != 0) {
					player.chat("/editench" + " Looting " + Integer.toString(looting));
				}
				if (fireAspect != 0) {
					player.chat("/editench" + " FireAspect " + Integer.toString(fireAspect));
				}
				if (iceAspect != 0) {
					player.chat("/editench" + " IceAspect " + Integer.toString(iceAspect));
				}
				if (thunderAspect != 0) {
					player.chat("/editench" + " ThunderAspect " + Integer.toString(thunderAspect));
				}
				if (windAspect != 0) {
					player.chat("/editench" + " WindAspect " + Integer.toString(windAspect));
				}
				if (earthAspect != 0) {
					player.chat("/editench" + " EarthAspect " + Integer.toString(earthAspect));
				}
				if (decay != 0) {
					player.chat("/editench" + " Decay " + Integer.toString(decay));
				}
				if (bleeding != 0) {
					player.chat("/editench" + " Bleeding " + Integer.toString(bleeding));
				}
				if (smite != 0) {
					player.chat("/editench" + " Smite " + Integer.toString(smite));
				}
				if (slayer != 0) {
					player.chat("/editench" + " Slayer " + Integer.toString(slayer));
				}
				if (duelist != 0) {
					player.chat("/editench" + " Duelist " + Integer.toString(duelist));
				}
				if (hexEater > 0) {
					player.chat("/editench" + " HexEater " + Integer.toString(hexEater));
				}
				if (chaotic > 0) {
					player.chat("/editench" + " Chaotic " + Integer.toString(chaotic));
				}
				if (twoHanded > 0) {
					player.chat("/editench" + " TwoHanded " + Integer.toString(twoHanded));
				}
				if (quake > 0) {
					player.chat("/editench" + " Quake " + Integer.toString(quake));
				}
				if (pointBlank > 0) {
					player.chat("/editench" + " PointBlank " + Integer.toString(pointBlank));
				}
				if (sniper > 0) {
					player.chat("/editench" + " Sniper " + Integer.toString(sniper));
				}
				if (punch > 0) {
					player.chat("/editench" + " Punch" + Integer.toString(punch));
				}
				if (recoil > 0) {
					player.chat("/editench" + " Recoil " + Integer.toString(recoil));
				}
				if (quickCharge > 0) {
					player.chat("/editench" + " QuickCharge " + Integer.toString(quickCharge));
				}
				if (multishot > 0) {
					player.chat("/editench" + " Multishot " + Integer.toString(multishot));
				}
				if (piercing > 0) {
					player.chat("/editench" + " Piercing " + Integer.toString(piercing));
				}
				if (infinity > 0) {
					player.chat("/editench" + " Infinity " + Integer.toString(infinity));
				}
				if (riptide > 0) {
					player.chat("/editench" + " Riptide " + Integer.toString(riptide));
				}
				if (efficiency > 0) {
					player.chat("/editench" + " Efficiency " + Integer.toString(efficiency));
				}
				if (fortune > 0) {
					player.chat("/editench" + " Fortune " + Integer.toString(fortune));
				}
				if (silkTouch > 0) {
					player.chat("/editench" + " SilkTouch" + Integer.toString(silkTouch));
				}
				if (eruption > 0) {
					player.chat("/editench" + " Eruption " + Integer.toString(eruption));
				}
				if (shrapnel > 0) {
					player.chat("/editench" + " CurseofShrapnel " + Integer.toString(shrapnel));
				}
				if (multitool > 0) {
					player.chat("/editench" + " Multitool " + Integer.toString(multitool));
				}
				if (luckOfTheSea > 0) {
					player.chat("/editench" + " LuckOfTheSea " + Integer.toString(luckOfTheSea));
				}
				if (lure > 0) {
					player.chat("/editench" + " Lure " + Integer.toString(lure));
				}
				if (ashes > 0) {
					player.chat("/editench" + " AshesofEternity " + Integer.toString(ashes));
				}
				if (jungles > 0) {
					player.chat("/editench" + " JunglesNourishment " + Integer.toString(jungles));
				}
				if (depths > 0) {
					player.chat("/editench" + " ProtectionoftheDepths " + Integer.toString(depths));
				}
				if (rageKeter > 0) {
					player.chat("/editench" + " RageoftheKeter " + Integer.toString(rageKeter));
				}
				if (wand > 0) {
					player.chat("/editench" + " MagicWand 1");
				}
				player.chat("/editench" + " MainhandOffhandDisable 1");
			} else {
				player.chat("/editench" + " OffhandMainhandDisable 1");
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
