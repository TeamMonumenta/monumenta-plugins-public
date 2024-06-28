package com.playmonumenta.plugins.commands;

import com.goncalomb.bukkit.mylib.reflect.NBTTagCompound;
import com.goncalomb.bukkit.mylib.reflect.NBTTagList;
import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.variables.ListVariable;
import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.bosses.PhasesManagerBoss;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.BossPhasesList;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.Tooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.SafeSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BossTagCommand {

	private static final Pattern COMMAS_REMOVER = Pattern.compile(",+");

	private static final int MAXIMUM_TAG_LENGTH = 150;

	private static final String COMMAND = "bosstag";

	private static final Map<String, List<Soul>> SEARCH_OUTCOME_MAP = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public static void register() {

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag")
			.withSubcommand(
				new CommandAPICommand("add")
					.withArguments(new GreedyStringArgument("boss_tag").replaceSafeSuggestions(SafeSuggestions.tooltipCollection(BossTagCommand::suggestionsBossTag)))
					.executesPlayer((player, args) -> {
						addNewBossTag(player, (String) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("info")
					.withArguments(new StringArgument("boss_tag").includeSuggestions(ArgumentSuggestions.strings(t -> getPossibleBosses(t.currentArg()))))
					.executesPlayer((player, args) -> {
						try {
							infoBossTag(player, (String) args[1]);
						} catch (Exception e) {
							throw CommandAPI.failWithString(e.getMessage());
						}
					})
			)

			.withSubcommand(
				new CommandAPICommand("show")
					.executesPlayer((player, args) -> {
						showBossTag(player);
					})
			)
			.withSubcommand(
				new CommandAPICommand("show")
					.withArguments(new GreedyStringArgument("boss_tag").replaceSafeSuggestions(SafeSuggestions.tooltips(BossTagCommand::suggestionBossTagBasedonBoS)))
					.executesPlayer((player, args) -> {
						showBossTag(player, (String) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("search")
					.withArguments(new GreedyStringArgument("boss_tag").replaceSafeSuggestions(SafeSuggestions.tooltipCollection(BossTagCommand::suggestionsBossTag)))
					.executesPlayer((player, args) -> {
						searchTag(player, (String) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("get")
					.withArguments(new GreedyStringArgument("boss_tag").replaceSafeSuggestions(SafeSuggestions.suggest(SEARCH_OUTCOME_MAP.keySet())))
					.executesPlayer((player, args) -> {
						getBosSearched(player, (String) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("remove")
					.withArguments(new GreedyStringArgument("boss_tag").replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsCollection(BossTagCommand::suggestionBossTagBasedonBoSAndParams)))
					.executesPlayer((player, args) -> {
						removeBossTag(player, (String) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("list")
					.executesPlayer((player, args) -> {
						listBossTags(player);
					})
			)
			.withSubcommand(
				new CommandAPICommand("edit")
					.withArguments(
						new IntegerArgument("index"),
						new GreedyStringArgument("boss_tag").replaceSafeSuggestions(SafeSuggestions.tooltipCollection(BossTagCommand::suggestionsBossTag)))
					.executesPlayer((player, args) -> {
						editBossTag(player, (int) args[0], (String) args[1]);
					})
			)
			.withSubcommand(
				new CommandAPICommand("delete")
					.withArguments(new IntegerArgument("index"))
					.executesPlayer((player, args) -> {
						deleteBossTag(player, (int) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("squash")
					.executesPlayer((player, args) -> {
						squashBossTags(player);
					})
			)

			.withSubcommand(
				new CommandAPICommand("los_errors")
					.executesPlayer((player, args) -> {
						checkAllLos(player, true);
					})
			)
			.withSubcommand(
				new CommandAPICommand("los_errors")
					.withArguments(new BooleanArgument("spawn chest"))
					.executesPlayer((player, args) -> {
						checkAllLos(player, (boolean) args[0]);
					})
			)

			.withSubcommand(
				new CommandAPICommand("help")
					.executesPlayer((player, args) -> {
						helpBossTags(player);
					})
			)

			.withSubcommand(
				new CommandAPICommand("phase")
					.withSubcommand(
						new CommandAPICommand("add")
							.withArguments(
								new StringArgument("PhaseName"),
								new BooleanArgument("reusable"),
								new GreedyStringArgument("PhaseExpression").replaceSafeSuggestions(SafeSuggestions.tooltipCollection(BossPhasesList::suggestionPhases)))
							.executesPlayer((player, args) -> {
								String phaseName = (String) args[0];
								boolean reusable = (boolean) args[1];
								String phaseString = (String) args[2];

								BookOfSouls bos = getBos(player);
								ListVariable tags = (ListVariable) bos.getEntityNBT().getVariable("Tags");
								NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

								boolean foundBaseTag = false;
								if (nbtTagsList != null && nbtTagsList.size() > 0) {
									for (Object object : nbtTagsList.getAsArray()) {
										if (object.toString().contains(PhasesManagerBoss.identityTag)) {
											foundBaseTag = true;
											break;
										}
									}
								}

								if (!foundBaseTag) {
									tags.add(PhasesManagerBoss.identityTag, player);
									bos.saveBook();
								}

								String newTag = PhasesManagerBoss.identityTag + "[phases=[(" + phaseName + "," + reusable + "," + phaseString + ")]]";

								player.sendMessage(Component.empty()
									                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
									                   .append(Component.text(newTag, NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
								tags.add(newTag, player);
								bos.saveBook();

							})
					)
					.withSubcommand(
						new CommandAPICommand("trigger")
							.withArguments(
								new EntitySelectorArgument.ManyEntities("bosses"),
								new GreedyStringArgument("triggerKey"))
							.executesPlayer((player, args) -> {
								String triggerKey = (String) args[3];
								BossManager bossManager = BossManager.getInstance();
								PhasesManagerBoss boss;
								for (Entity entity : (Collection<Entity>) args[2]) {
									boss = bossManager.getBoss(entity, PhasesManagerBoss.class);
									if (boss != null) {
										boss.onCustomTrigger(triggerKey);
									}
								}

							})
					)
			)
			.register();

	}

	private static List<Tooltip<String>> suggestionsBossTag(SuggestionInfo info) {
		String currentArg = info.currentArg();

		String[] bossTags = getPossibleBosses(currentArg);

		if (bossTags.length == 1) {
			String description = "undefined";
			BossParameters parameters = BossManager.mBossParameters.get(bossTags[0]);
			if (parameters != null) {
				BossParam annotations = parameters.getClass().getAnnotation(BossParam.class);
				if (annotations != null) {
					description = annotations.help();
				}
			}
			if (BossManager.mBossParameters.containsKey(bossTags[0])) {
				return List.of(
					Tooltip.ofString(bossTags[0], description),
					Tooltip.ofString(bossTags[0] + "[", description)
				);
			} else {
				// Tag is a real tag but doesn't have parameters - don't bother offering opening [
				return List.of(Tooltip.ofString(bossTags[0], description));
			}
		} else if (bossTags.length > 1) {
			List<Tooltip<String>> entries = new ArrayList<>(bossTags.length);
			for (String bossTag : bossTags) {
				String description = "undefined";
				BossParameters parameters = BossManager.mBossParameters.get(bossTag);
				if (parameters != null) {
					BossParam annotations = parameters.getClass().getAnnotation(BossParam.class);
					if (annotations != null) {
						description = annotations.help();
					}
				}
				entries.add(Tooltip.ofString(bossTag, description));
			}
			return entries;
		} else {
			StringReader reader = new StringReader(info.currentArg());
			String currentBossTag = reader.readUntil("[");
			if (currentBossTag == null) {
				// Invalid, boss tag doesn't match any suggestions and has no opening bracket for arguments
				return List.of();
			}

			BossParameters parameter = BossManager.mBossParameters.get(currentBossTag);
			if (parameter == null) {
				// Invalid, boss tag doesn't have parameters
				return List.of();
			}

			// TODO: parameter.clone(), very important!
			ParseResult<BossParameters> result = BossParameters.parseParameters(reader, parameter, false);
			if (result.getTooltip() != null) {
				return result.getTooltip();
			}
		}
		return List.of();
	}

	public static class TypeAndDesc {
		private final Field mField;
		private final String mDesc;
		private final boolean mIsDeprecated;

		public TypeAndDesc(Field field, String desc) {
			this(field, desc, false);
		}

		public TypeAndDesc(Field field, String desc, boolean deprecated) {
			mField = field;
			mDesc = desc;
			mIsDeprecated = deprecated;
		}

		public Field getField() {
			return mField;
		}

		public String getDesc() {
			return mDesc;
		}

		public Boolean getDeprecated() {
			return mIsDeprecated;
		}
	}

	private static String[] getPossibleBosses(String possibleBossTag) {
		if (possibleBossTag == null || possibleBossTag.isEmpty()) {
			return BossManager.getInstance().listStatelessBosses();
		}
		List<String> bossTagList = new ArrayList<>();

		for (String bossTag : BossManager.getInstance().listStatelessBosses()) {
			if (bossTag.startsWith(possibleBossTag)) {
				bossTagList.add(bossTag);
			}
		}

		return bossTagList.toArray(new String[0]);
	}

	private static void addNewBossTag(Player player, String newTag) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		ListVariable tags = (ListVariable) bos.getEntityNBT().getVariable("Tags");
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

		int index = newTag.indexOf("[");
		if (index != -1) {
			String bossTag = newTag.substring(0, index);

			try {
				BossUtils.checkParametersStringProperty(newTag);
			} catch (Exception e) {
				player.sendMessage(Component.empty()
					.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					.append(Component.text("problems during parsing bosstag. Reason: " + e.getMessage(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
				player.sendMessage(Component.empty()
					.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					.append(Component.text(newTag, NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));

				player.sendMessage(Component.empty()
					.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					.append(Component.text("Do you still want to add this tag?", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
				player.sendMessage(Component.empty()
					.append(Component.text("[YES] ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true).clickEvent(
						ClickEvent.runCommand("/bos var Tags add " + newTag)
					)));
				return;
			}


			boolean found = false;
			Map<String, String> oldParams = new HashMap<>();
			Map<String, String> newParams = new HashMap<>();

			if (nbtTagsList != null && nbtTagsList.size() > 0) {
				for (Object obj : nbtTagsList.getAsArray()) {
					if (obj.equals(bossTag)) {
						found = true;
					}

					if (obj.toString().startsWith(bossTag + "[")) {
						BossUtils.addModifiersFromString(oldParams, obj.toString().replace(bossTag, ""));
					}
				}
			}

			//checking if the bosstag is implemented or not
			if (!found) {
				player.sendMessage(Component.empty()
					                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
					                   .append(Component.text(" is missing! \n", NamedTextColor.GRAY))
					                   .append(Component.text("Going to implement. Wait...", NamedTextColor.GRAY)));
				tags.add(bossTag, player);
				bos.saveBook();
				player.sendMessage(Component.empty()
					                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
					                   .append(Component.text(" correctly implemented! ", NamedTextColor.GRAY)));
			}

			//checking if a new param overwrites an old one
			if (!oldParams.isEmpty()) {
				BossUtils.addModifiersFromString(newParams, newTag.replace(bossTag, ""));

				for (String paramName : newParams.keySet()) {
					if (oldParams.get(paramName) != null) {
						throw CommandAPI.failWithString("Parameter: " + paramName + " already implemented from another tag with value " + oldParams.get(paramName));
					}
				}
			}
		}

		tags.add(newTag, player);
		bos.saveBook();
		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Tag added! ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
	}

	private static BookOfSouls getBos(Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (BookOfSouls.isValidBook(item)) {
			BookOfSouls bos = BookOfSouls.getFromBook(item);
			if (bos != null) {
				return bos;
			}
			throw CommandAPI.failWithString("That Book of Souls is corrupted!");
		}
		throw CommandAPI.failWithString("You must be holding a Book of Souls!");
	}

	private static void infoBossTag(Player player, String bossTag) throws IllegalArgumentException, IllegalAccessException {

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Help ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
			                   .append(Component.text(bossTag, NamedTextColor.WHITE)));


		BossParameters parameters = BossManager.mBossParameters.get(bossTag);
		BossParam description = (parameters != null ? parameters.getClass().getAnnotation(BossParam.class) : null);

		player.sendMessage(Component.empty()
			                   .append(Component.text("Description: ", NamedTextColor.GOLD))
			                   .append(Component.text((description != null ? description.help() : "not written"), NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("Parameters", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(parameters == null ? Component.text(" NOT implemented.", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false) : Component.empty()));

		if (parameters != null) {
			player.sendMessage(Component.empty()
				                   .append(Component.text("Name" + " ".repeat(26) + "default value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));

			for (Field field : parameters.getClass().getFields()) {
				String fieldName = BossUtils.translateFieldNameToTag(field.getName());
				BossParam annotations = field.getAnnotation(BossParam.class);
				int spaces = 30 - fieldName.length();

				player.sendMessage(Component.empty()
					                   .append(Component.text("- " + fieldName + " ".repeat(spaces), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)
						                           .hoverEvent(Component.empty()
							                                       .append(Component.text("Parameter: ", TextColor.fromHexString("#ffd700")).decoration(TextDecoration.BOLD, true)
								                                               .append(Component.text(fieldName + "\n", TextColor.fromHexString("#808080")).decoration(TextDecoration.BOLD, false)))
							                                       .append(Component.text("Desciption: ", TextColor.fromHexString("#ffd700")).decoration(TextDecoration.BOLD, true)
								                                               .append(Component.text(annotations != null ? annotations.help() + "\n" : "\n", TextColor.fromHexString("#808080")).decoration(TextDecoration.BOLD, false)))))
					                   .append(Component.text(field.get(parameters).toString())));
			}
		}
	}

	private static void showBossTag(Player player) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");
		Set<String> statelessBoss = new HashSet<String>(Arrays.asList(BossManager.getInstance().listStatelessBosses()));
		Set<String> bossTags = new HashSet<>();

		if (nbtTagsList != null && nbtTagsList.size() > 0) {
			for (Object tag : nbtTagsList.getAsArray()) {
				if (tag instanceof String stringTag && statelessBoss.contains(stringTag)) {
					bossTags.add(stringTag);
				}
			}
		}


		if (!bossTags.isEmpty()) {
			Map<String, String> parameters = new HashMap<>();

			for (String bossTag : bossTags) {
				parameters.clear();
				for (Object tag : nbtTagsList.getAsArray()) {
					if (((String)tag).startsWith(bossTag + "[")) {
						BossUtils.addModifiersFromString(parameters, ((String)tag).replace(bossTag, ""));
					}
				}

				player.sendMessage(Component.empty()
					                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
					                   .append(Component.text(" is implemented!", NamedTextColor.GRAY)));

				player.sendMessage(Component.empty()
					                   .append(Component.text("Paramaters implemented:", NamedTextColor.GOLD))
					                   .append(parameters.isEmpty() ? Component.text(" NOT implemented.", NamedTextColor.GRAY) : Component.empty()));

				if (!parameters.isEmpty()) {
					player.sendMessage(Component.empty()
						                   .append(Component.text("Name" + " ".repeat(26) + "value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));

					for (String paramName : parameters.keySet()) {
						int spaces = 30 - paramName.length();
						player.sendMessage(Component.empty()
							                   .append(Component.text("- " + paramName + " ".repeat(spaces), NamedTextColor.GOLD))
							                   .append(Component.text(parameters.get(paramName))));
					}
				}
			}

		} else {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Didn't find any bosstag for this entity", NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));
		}
	}

	private static void showBossTag(Player player, String bossTag) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");
		Map<String, String> parameters = new HashMap<>();
		boolean found = false;


		for (Object tag : nbtTagsList.getAsArray()) {
			if (tag.equals(bossTag)) {
				found = true;
			}

			if (((String)tag).startsWith(bossTag + "[")) {
				BossUtils.addModifiersFromString(parameters, ((String)tag).replace(bossTag, ""));
			}
		}

		if (found) {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(" is implemented!", NamedTextColor.GRAY)));

			player.sendMessage(Component.empty()
				                   .append(Component.text("Paramaters implemented: ", NamedTextColor.GOLD))
				                   .append(parameters.isEmpty() ? Component.text(" NOT implemented.", NamedTextColor.GOLD) : Component.empty()));

			if (!parameters.isEmpty()) {
				player.sendMessage(Component.empty()
					                   .append(Component.text("Name" + " ".repeat(26) + "value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));

				for (String paramName : parameters.keySet()) {
					int spaces = 30 - paramName.length();
					player.sendMessage(Component.empty()
						                   .append(Component.text("- " + paramName + " ".repeat(spaces), NamedTextColor.GOLD))
						                   .append(Component.text(parameters.get(paramName))));
				}
			}
		} else {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(" is not implemented", NamedTextColor.RED)));
		}
	}

	private static void searchTag(Player player, String bossTag) {
		int indexBracket = bossTag.indexOf("[");
		String realBossTag = bossTag;
		Map<String, String> param = new LinkedHashMap<>();

		if (indexBracket != -1) {
			realBossTag = bossTag.substring(0, indexBracket);
			BossUtils.addModifiersFromString(param, realBossTag);
		}

		List<Soul> soulsList = new ArrayList<>();
		Set<String> soulsName = LibraryOfSoulsAPI.getSoulNames();

		boolean shouldAdd;

		if (indexBracket == -1) {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Searching tag that contains " + bossTag, NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(" inside " + soulsName.size() + " record", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
		} else {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Searching tag that contains " + realBossTag + " and params", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(" inside " + soulsName.size() + " record", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
		}

		for (String soulName : soulsName) {
			Soul soul = SoulsDatabase.getInstance().getSoul(soulName);
			NBTTagList nbtTagsList = soul.getNBT().getList("Tags");

			if (nbtTagsList != null && nbtTagsList.getAsArray() != null) {
				for (Object tag : nbtTagsList.getAsArray()) {

					if (tag instanceof String stringTag && stringTag.contains(realBossTag)) {
						shouldAdd = true;

						if (!param.isEmpty()) {
							//we have params to checks
							Map<String, String> currentParam = new LinkedHashMap<>();

							//load a map with the params that the soul has
							for (Object tagSecond : nbtTagsList.getAsArray()) {
								if (tagSecond instanceof String stringTagSecond && stringTagSecond.startsWith(realBossTag + "[")) {
									BossUtils.addModifiersFromString(currentParam, stringTagSecond.replace(realBossTag, ""));
								}
							}

							for (Map.Entry<String, String> e : param.entrySet()) {
								if (!e.getValue().equals(currentParam.get(e.getKey()))) {
									shouldAdd = false;
									break;
								}
							}
						}

						if (shouldAdd) {
							soulsList.add(soul);
						}

						//we found a match, we can exit and go search for others souls
						break;
					}
				}
			}
		}

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Total matching souls: " + soulsList.size(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

		SEARCH_OUTCOME_MAP.put(bossTag, soulsList);

		if (soulsList.size() > 0) {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Run ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text("/bosstag get " + bossTag, NamedTextColor.WHITE).clickEvent(ClickEvent.suggestCommand("/bosstag get " + bossTag)))
				                   .append(Component.text(" to get the first BoS", NamedTextColor.GRAY)));
		}

	}

	private static void getBosSearched(Player player, String bossTag) throws WrapperCommandSyntaxException {
		List<Soul> souls = SEARCH_OUTCOME_MAP.get(bossTag);

		if (souls == null) {
			throw CommandAPI.failWithString("You must use /bosstag seach " + bossTag + " before using this command");
		}

		if (souls.size() == 0) {
			SEARCH_OUTCOME_MAP.remove(bossTag);
			throw CommandAPI.failWithString("No BookOfSouls remaining for tag: " + bossTag);
		}

		Soul bos = souls.get(0);
		souls.remove(0);

		player.getInventory().addItem(bos.getBoS());

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Book of Souls for " + bos.getLabel(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Remaining BoS: " + souls.size(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

		if (souls.size() == 0) {
			SEARCH_OUTCOME_MAP.remove(bossTag);
		}
	}

	private static void removeBossTag(Player player, String bossTag) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		ListVariable tags = (ListVariable) bos.getEntityNBT().getVariable("Tags");
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

		int index = bossTag.indexOf("[");
		String realBossTag = bossTag;

		if (index != -1) {
			realBossTag = bossTag.substring(0, index);
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Removing parameters of ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(realBossTag, NamedTextColor.WHITE))
			);

			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Parameters removed:", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
			);
		} else {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Removing ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(realBossTag, NamedTextColor.WHITE))
				                   .append(Component.text(" and all dependencies.", NamedTextColor.GRAY))
			);
		}

		//load params
		Map<String, String> params = new LinkedHashMap<>();
		BossUtils.addModifiersFromString(params, bossTag.replace(realBossTag, ""));
		List<String> paramsList = new ArrayList<>();
		List<String> newOldTags = new ArrayList<>();

		for (String paramKey : params.keySet()) {
			paramsList.add(paramKey + "=" + params.get(paramKey));
			//why tf \t don't exists inside the Component.text()...
			player.sendMessage(Component.empty()
				                   .append(Component.text("- " + paramKey + "       ", NamedTextColor.GRAY))
				                   .append(Component.text(params.get(paramKey), NamedTextColor.GRAY))
			);
		}

		for (int i = nbtTagsList.size() - 1; i >= 0; i--) {
			String tagString = (String)nbtTagsList.get(i);
			if (index == -1) {
				//we want to remove all the dependecy
				if (tagString.startsWith(realBossTag)) {
					tags.remove(i);
				}
			} else {
				//checks for params.
				if (tagString.startsWith(realBossTag + "[")) {
					String paramString = tagString;
					boolean foundAtLeastOne = false;
					for (String param : paramsList) {
						if (paramString.contains(param)) {
							foundAtLeastOne = true;
							paramString = paramString.replace(param, "");
						}
					}
					if (foundAtLeastOne) {
						paramString = COMMAS_REMOVER.matcher(paramString).replaceAll(",").replace("[,", "[").replace(",]", "]");
						params.clear();
						BossUtils.addModifiersFromString(params, paramString.replace(realBossTag, ""));
						if (!params.isEmpty()) {
							newOldTags.add(paramString);
						}
						tags.remove(i);
					}
				}
			}
		}

		if (!newOldTags.isEmpty()) {
			//we need to reimplement some tags
			for (String tag : newOldTags) {
				tags.add(tag, player);
			}
		}

		bos.saveBook();

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Tag(s) removed!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
	}

	private static void listBossTags(Player player) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Tags in BoS:", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);


		for (int i = 0; i < nbtTagsList.size(); i++) {
			String tagString = (String) nbtTagsList.get(i);
			player.sendMessage(
				Component.empty()
					.append(Component.text("[X]", NamedTextColor.RED)
						        .hoverEvent(HoverEvent.showText(Component.text("Click to delete this tag", NamedTextColor.RED)))
						        .clickEvent(ClickEvent.suggestCommand("/bosstag delete " + i)))
					.append(Component.text(" "))
					.append(Component.text(tagString, NamedTextColor.WHITE)
						        .clickEvent(ClickEvent.suggestCommand("/bosstag edit " + i + " " + tagString))));
		}
	}

	private static void deleteBossTag(Player player, int index) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		ListVariable tags = (ListVariable) bos.getEntityNBT().getVariable("Tags");
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");
		if (index < 0 || index >= nbtTagsList.size()) {
			throw CommandAPI.failWithString("Invalid index " + index);
		}
		String tag = (String) nbtTagsList.get(index);
		tags.remove(index);

		bos.saveBook();

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Tag " + tag + " removed!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
		listBossTags(player);
	}

	private static void editBossTag(Player player, int index, String tag) throws WrapperCommandSyntaxException {
		BookOfSouls bos = getBos(player);
		ListVariable tags = (ListVariable) bos.getEntityNBT().getVariable("Tags");
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");
		if (index < 0 || index >= nbtTagsList.size()) {
			throw CommandAPI.failWithString("Invalid index " + index);
		}
		tags.remove(index);
		tags.add(tag, player);

		bos.saveBook();

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Tag edited!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
		listBossTags(player);
	}


	private static Tooltip<String>[] suggestionBossTagBasedonBoS(SuggestionInfo info) {
		try {
			BookOfSouls bos = getBos((Player) info.sender());
			NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

			List<Tooltip<String>> bossTags = new ArrayList<>();

			for (Object tag : nbtTagsList.getAsArray()) {
				String tagString = (String) tag;

				if (BossManager.mBossParameters.get(tagString) != null) {
					bossTags.add(Tooltip.ofString(tagString, null));
					//TODO- write a better Tooltip
				}
			}

			return bossTags.toArray(Tooltip.arrayOf());

		} catch (Exception e) {
			info.sender().sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
		}

		return Tooltip.arrayOf();

	}

	private static Collection<IStringTooltip> suggestionBossTagBasedonBoSAndParams(SuggestionInfo info) {
		try {
			BookOfSouls bos = getBos((Player) info.sender());
			NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

			if (nbtTagsList != null) {
				List<IStringTooltip> bossTags = new ArrayList<>();
				List<String> bossTagList = new ArrayList<>();
				Map<String, Map<String, String>> paramsMap = new LinkedHashMap<>();

				for (Object tag : nbtTagsList.getAsArray()) {
					String tagString = (String) tag;

					if (BossManager.mBossParameters.get(tagString) != null) {
						bossTagList.add(tagString);
						paramsMap.put(tagString, new LinkedHashMap<>());
						bossTags.add(StringTooltip.ofMessage(tagString, null));
						//TODO- write a better Tooltip
					}
				}

				//we need a better way to do this.
				for (Object tag : nbtTagsList.getAsArray()) {
					String tagString = (String) tag;
					for (String bossTag : bossTagList) {
						if (tagString.startsWith(bossTag + "[")) {
							BossUtils.addModifiersFromString(Objects.requireNonNull(paramsMap.get(bossTag)), tagString.replace(bossTag, ""));
						}
					}
				}
				//TODO-finish this part.
				return Collections.unmodifiableList(bossTags);
			}

		} catch (Exception e) {
			info.sender().sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
		}

		return Collections.emptyList();
	}

	private static void squashBossTags(Player player) throws WrapperCommandSyntaxException {
		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Started squashing!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		BookOfSouls bos = getBos(player);
		ListVariable tags = (ListVariable) bos.getEntityNBT().getVariable("Tags");
		NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");
		Map<String, Map<String, String>> bossTagParamertersMap = new LinkedHashMap<>();


		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("bosstags:", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
		//read for bosstags
		for (Object tag : nbtTagsList.getAsArray()) {
			String tagString = (String)tag;

			if (BossManager.mBossParameters.get(tagString) != null) {
				bossTagParamertersMap.put(tagString, new LinkedHashMap<>());
				player.sendMessage(Component.empty()
					                   .append(Component.text("- ", NamedTextColor.GOLD))
					                   .append(Component.text(tagString, NamedTextColor.GRAY))
				);
			}
		}

		//read parameters and removing from the tag
		for (int i = nbtTagsList.size() - 1; i >= 0; i--) {
			for (Map.Entry<String, Map<String, String>> e : bossTagParamertersMap.entrySet()) {
				String bossTag = e.getKey();
				if (((String) nbtTagsList.get(i)).startsWith(bossTag + "[")) {
					//found some parameters
					BossUtils.addModifiersFromString(e.getValue(), ((String) nbtTagsList.get(i)).replace(bossTag, ""));
					tags.remove(i);
					break;
				}
			}
		}

		//recreate the tags and load back
		List<String> nextTags = new ArrayList<>();
		String tag;
		for (Map.Entry<String, Map<String, String>> e : bossTagParamertersMap.entrySet()) {
			String bossTag = e.getKey();
			Map<String, String> parametersMap = e.getValue();
			if (!parametersMap.isEmpty()) {
				tag = bossTag + "[";
				BossParameters parameters = Objects.requireNonNull(BossManager.mBossParameters.get(bossTag));

				//following order of implementations inside the class
				//TODO-we may want to separate in 3 different (generics-sounds-particles)
				for (Field field : parameters.getClass().getFields()) {
					String translated = BossUtils.translateFieldNameToTag(field.getName());
					if (parametersMap.containsKey(translated)) {
						tag += translated + "=" + parametersMap.get(translated) + ",";

						if (tag.length() >= MAXIMUM_TAG_LENGTH) {
							tag = tag.substring(0, tag.length() - 1) + "]";
							nextTags.add(tag);
							tag = bossTag + "[";
						}
					}
				}
				//last tag remaning
				tag = tag.substring(0, tag.length() - 1) + "]";
				nextTags.add(tag);

			}
		}

		for (String newTag : nextTags) {
			tags.add(newTag, player);
		}
		bos.saveBook();

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Squash completed!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
	}

	private static List<String> checkEntity(Component description, String soulLabel, NBTTagCompound entityNBT, Player sender) {

		List<String> errors = new ArrayList<>();

		List<String> bosTags = new ArrayList<>();
		NBTTagList tags = entityNBT.getList("Tags");
		if (tags != null) {
			for (int i = 0; i < tags.size(); i++) {
				String tag = (String) tags.get(i);
				if (BossManager.mBossParameters.get(tag) != null) {
					//this Soul may be with some parameters, lets save it to check later
					bosTags.add(tag);
				}
			}
		}

		String customName = entityNBT.getString("CustomName");
		Component bossDescription = (customName == null || customName.isEmpty() ? Component.text("Unnamed " + entityNBT.getString("id"), NamedTextColor.BLUE) : MessagingUtils.parseComponent(customName)).append(description);

		for (String bossTag : bosTags) {
			for (int i = 0; i < tags.size(); i++) {
				String tag = (String) tags.get(i);
				if (tag.startsWith(bossTag + "[")) {
					//found a param string
					StringReader reader = new StringReader(tag);
					reader.advance(bossTag);

					Component descWithClickEvent = bossDescription.hoverEvent(HoverEvent.showText(Component.text("Click to get (base) BoS")))
						                               .clickEvent(ClickEvent.runCommand("/los get " + soulLabel));
					ParseResult<?> result = BossParameters.parseParametersWithWarnings(descWithClickEvent, reader, Objects.requireNonNull(BossManager.mBossParameters.get(bossTag)), List.of(sender));

					if (result.getResult() == null) {
						errors.add(tag);
					} else if (result.mDeprecatedParameters != null) {
						sender.sendMessage(Component.text("[BossParameters] ", NamedTextColor.GOLD)
							                   .append(Component.text("problems during parsing tag for ", NamedTextColor.RED))
							                   .append(descWithClickEvent)
							                   .append(Component.text(" | on tag: ", NamedTextColor.RED))
							                   .append(Component.text(reader.getString(), NamedTextColor.WHITE))
							                   .append(Component.text(" | deprecated parameter(s) used: ", NamedTextColor.RED))
							                   .append(Component.text(result.mDeprecatedParameters.toString(), NamedTextColor.YELLOW)));
						errors.add(tag + " contains deprecated parameter(s): " + result.mDeprecatedParameters);
					}
				}
			}
		}

		// recursively check passengers
		NBTTagList passengers = entityNBT.getList("Passengers");
		if (passengers != null) {
			for (Object passenger : passengers.getAsArray()) {
				TextComponent nestedDescription = Component.text(" (passenger of ", NamedTextColor.GRAY).append(bossDescription).append(Component.text(")", NamedTextColor.GRAY));
				List<String> passengerErrors = checkEntity(nestedDescription, soulLabel, (NBTTagCompound) passenger, sender);
				for (String passengerError : passengerErrors) {
					errors.add("  in passenger: " + passengerError);
				}
			}
		}

		return errors;

	}

	public static void checkAllLos(Player player, boolean spawnChest) {

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Checking all the mobs in the Library of Souls. This may take a while...", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		Map<Soul, List<String>> soulDeprecatedTagMap = new LinkedHashMap<>();
		for (String soulName : LibraryOfSoulsIntegration.getSoulNames()) {
			Soul soul = SoulsDatabase.getInstance().getSoul(soulName);
			List<String> errors = checkEntity(Component.text(" (soul label: " + soul.getLabel() + ")", NamedTextColor.BLUE), soul.getLabel(), soul.getNBT(), player);
			if (!errors.isEmpty()) {
				soulDeprecatedTagMap.put(soul, errors);
			}
		}

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Checked all the mobs in the Library of Souls", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Found some problems on: " + soulDeprecatedTagMap.keySet().size(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		if (spawnChest && !soulDeprecatedTagMap.isEmpty()) {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text("Generating a chest with one or more Book of Souls.", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
			);

			Location loc = player.getLocation();
			Inventory chestInventory = null;
			int startPoint = 0;
			for (Map.Entry<Soul, List<String>> entry : soulDeprecatedTagMap.entrySet()) {
				if (chestInventory == null || startPoint >= 26) {
					// First chest, or previous chest is full: create a new one
					startPoint = 0;
					loc = loc.add(1, 0, 0);
					while (loc.getBlock().getType() != Material.AIR) {
						loc.add(1, 0, 0);
					}
					Block block = loc.getBlock();
					block.setType(Material.CHEST);
					Chest chest = ((Chest) block.getState());
					chestInventory = chest.getBlockInventory();
				}

				Soul soul = entry.getKey();
				List<String> bossTags = entry.getValue();

				ItemStack book = soul.getBoS();
				ItemStack paper = buildPaper(soul.getLabel(), bossTags);

				chestInventory.addItem(book, paper);
				startPoint += 2;
			}
		}

		player.sendMessage(Component.empty()
							.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
							.append(Component.text("All done, HAVE FUN!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

	}

	private static ItemStack buildPaper(String name, List<String> lore) {
		ItemStack paper = new ItemStack(Material.PAPER);
		ItemMeta meta = paper.getItemMeta();

		meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
		List<Component> loreC = new ArrayList<>();

		for (String loreS : lore) {
			loreC.add(Component.text(loreS).decoration(TextDecoration.ITALIC, false));
		}
		meta.lore(loreC);
		paper.setItemMeta(meta);

		return paper;
	}

	private static void helpBossTags(Player player) {
		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTags] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("help", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("===========================================", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag add <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag add ")))
			                   .append(Component.text(" Adds a boss tag to your currently held Book of Souls.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag add <boss_tag[...]>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag add ")))
			                   .append(Component.text(" Add a boss tag to your currently held Book of Souls. Checks if the BoS has the current boss_tag implemented.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag remove <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag remove ")))
			                   .append(Component.text(" Remove a boss tag and all its dependencies in your currently held Book of Souls.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag remove <boss_tag[...]>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag remove ")))
			                   .append(Component.text(" Remove the specified parameter(s) of the boss tag without removing the tag entirely.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag show", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag show")))
			                   .append(Component.text(" Display every boss tag implemented in your currently held Book of Souls.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag show <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag show ")))
			                   .append(Component.text(" Only display the parameter(s) for the selected boss tag if implemented in your currently held Book of Souls.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag list", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag list")))
			                   .append(Component.text(" Lists all raw boss tags in the held BoS, and provides options to delete or edit specific tags.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag search <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag search ")))
			                   .append(Component.text(" Search the Library of Souls database for matching boss tags.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag search <boss_tag[...]>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag search ")))
			                   .append(Component.text(" Search the Library of Souls database for matching boss tags with the same parameter(s).", NamedTextColor.GRAY))
		);


		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag get <search_id>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag get ")))
			                   .append(Component.text(" Get the first Book of Souls returned by /bosstag search <boss_tag[...]>.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag info <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag info ")))
			                   .append(Component.text(" Display information about the boss tag selected including its default parameter values.", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag los_errors", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag los_errors")))
			                   .append(Component.text(" Searches through the LoS to find any errors in boss tags, and spawns chests containing BopS with mobs in error. ", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("===========================================", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true))
		);
	}
}
