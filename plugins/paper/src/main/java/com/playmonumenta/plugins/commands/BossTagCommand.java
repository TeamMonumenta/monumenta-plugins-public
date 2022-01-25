package com.playmonumenta.plugins.commands;

import com.goncalomb.bukkit.mylib.reflect.NBTTagList;
import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.variables.ListVariable;
import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParseResult;
import com.playmonumenta.plugins.bosses.parameters.StringReader;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.Tooltip;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BossTagCommand {

	private static final Pattern COMMAS_REMOVER = Pattern.compile(",+");

	private static final int MAXIMUS_TAG_LENGHT = 150;

	private static final String COMMAND = "bosstag";

	private static final Map<String, List<Soul>> SEARCH_OUTCOME_MAP = new LinkedHashMap<>();

	public static void register() {

		List<Argument> arguments = new ArrayList<>();


		arguments.add(new MultiLiteralArgument("add"));
		arguments.add(new GreedyStringArgument("boss_tag").replaceWithSafeSuggestionsT(BossTagCommand::suggestionsBossTag));

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.add")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				addNewBossTag(player, (String) args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("info"));
		arguments.add(new StringArgument("boss_tag").includeSuggestions(t -> getPossibleBosses(t.currentArg())));

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.help")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				try {
					infoBossTag(player, (String) args[1]);
				} catch (Exception e) {
					CommandAPI.fail(e.getMessage());
				}
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("show"));

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.show")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				showBossTag(player);
			})
			.register();

		arguments.add(new GreedyStringArgument("boss_tag").replaceWithSafeSuggestionsT(BossTagCommand::suggestionBossTagBasedonBoS));

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.show")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				showBossTag(player, (String) args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("search"));
		arguments.add(new GreedyStringArgument("boss_tag").replaceWithSafeSuggestionsT(BossTagCommand::suggestionsBossTag));

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.search")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				searchTag(player, (String) args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("get"));
		arguments.add(new GreedyStringArgument("boss_tag").replaceWithSafeSuggestions(t -> SEARCH_OUTCOME_MAP.keySet().toArray(new String[0])));

		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.search")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				getBosSearched(player, (String) args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("remove"));
		arguments.add(new GreedyStringArgument("boss_tag").replaceWithSafeSuggestionsT(BossTagCommand::suggestionBossTagBasedonBoSAndParams));
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.remove")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				removeBossTag(player, (String) args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("squash"));
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.squash")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				squashBossTags(player);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("deprecated"));
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.deprecated")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				checksAllLos(player);
			})
			.register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("help"));
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.bosstag.help")
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				helpBossTags(player);
			})
			.register();
	}

	private static Tooltip<String>[] suggestionsBossTag(SuggestionInfo info) {
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
				return Tooltip.arrayOf(
					Tooltip.of(bossTags[0], description),
					Tooltip.of(bossTags[0] + "[", description)
				);
			} else {
				// Tag is a real tag but doesn't have paremeters - don't bother offering opening [
				return Tooltip.arrayOf(Tooltip.of(bossTags[0], description));
			}
		} else if (bossTags.length > 1) {
			List<Tooltip<String>> entries = new ArrayList<Tooltip<String>>(bossTags.length);
			for (String bossTag : bossTags) {
				String description = "undefined";
				BossParameters parameters = BossManager.mBossParameters.get(bossTag);
				if (parameters != null) {
					BossParam annotations = parameters.getClass().getAnnotation(BossParam.class);
					if (annotations != null) {
						description = annotations.help();
					}
				}
				entries.add(Tooltip.of(bossTag, description));
			}
			return entries.toArray(Tooltip.arrayOf());
		} else {
			StringReader reader = new StringReader(info.currentArg());
			String currentBossTag = reader.readUntil("[");
			if (currentBossTag == null) {
				// Invalid, boss tag doesn't match any suggestions and has no opening bracket for arguments
				return Tooltip.arrayOf();
			}

			BossParameters parameter = BossManager.mBossParameters.get(currentBossTag);
			if (parameter == null) {
				// Invalid, boss tag doesn't have parameters
				return Tooltip.arrayOf();
			}

			// TODO: parameter.clone(), very important!
			ParseResult<BossParameters> result = BossParameters.parseParameters(reader, parameter);
			if (result.getTooltip() != null) {
				return result.getTooltip();
			}
		}
		return Tooltip.arrayOf();
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
			/* TODO:
			if (!BossUtils.checkParametersStringPropriety(newTag.substring(index, newTag.length()))) {
				CommandAPI.fail("Parameters property not rispected. (May be to many brackets?)");
			}
			*/

			String bossTag = newTag.substring(0, index);
			Boolean found = false;
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
					                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
					                   .append(Component.text(" is missing! \n", NamedTextColor.GRAY))
					                   .append(Component.text("Going to implement. Wait...", NamedTextColor.GRAY)));
				tags.add(bossTag, player);
				bos.saveBook();
				player.sendMessage(Component.empty()
					                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
					                   .append(Component.text(" correctly implemented! ", NamedTextColor.GRAY)));
			}

			//checking if a new param overwrites an old one
			if (!oldParams.isEmpty()) {
				BossUtils.addModifiersFromString(newParams, newTag.replace(bossTag, ""));

				for (String paramName : newParams.keySet()) {
					if (oldParams.get(paramName) != null) {
						CommandAPI.fail("Parameter: " + paramName + " already implemented from another tag with value " + oldParams.get(paramName));
					}
				}
			}
		}

		tags.add(newTag, player);
		bos.saveBook();
		player.sendMessage(Component.empty()
			                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Tag added! ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
	}

	private static BookOfSouls getBos(Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (BookOfSouls.isValidBook(item)) {
			BookOfSouls bos = BookOfSouls.getFromBook(item);
			if (bos != null) {
				return bos;
			}
			CommandAPI.fail("That Book of Souls is corrupted!");
		}
		CommandAPI.fail("You must be holding a Book of Souls!");
		throw new RuntimeException();
	}

	private static void infoBossTag(Player player, String bossTag) throws IllegalArgumentException, IllegalAccessException {

		player.sendMessage(Component.empty()
			                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
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
				                   .append(Component.text("Name" + (" ".repeat(26)) + "default value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));

			for (Field field : parameters.getClass().getFields()) {
				String fieldName = BossUtils.translateFieldNameToTag(field.getName());
				BossParam annotations = field.getAnnotation(BossParam.class);
				int spaces = 30 - fieldName.length();

				player.sendMessage(Component.empty()
					                   .append(Component.text("- " + fieldName + (" ".repeat(spaces)), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)
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
				if (statelessBoss.contains(tag)) {
					bossTags.add((String) tag);
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
					                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
					                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
					                   .append(Component.text(" is implemented!", NamedTextColor.GRAY)));

				player.sendMessage(Component.empty()
					                   .append(Component.text("Paramaters implemented:", NamedTextColor.GOLD))
					                   .append(parameters.isEmpty() ? Component.text(" NOT implemented.", NamedTextColor.GRAY) : Component.empty()));

				if (!parameters.isEmpty()) {
					player.sendMessage(Component.empty()
						                   .append(Component.text("Name" + (" ".repeat(26)) + "value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));

					for (String paramName : parameters.keySet()) {
						int spaces = 30 - paramName.length();
						player.sendMessage(Component.empty()
							                   .append(Component.text("- " + paramName + (" ".repeat(spaces)), NamedTextColor.GOLD))
							                   .append(Component.text(parameters.get(paramName))));
					}
				}
			}

		} else {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
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
				                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(" is implemented!", NamedTextColor.GRAY)));

			player.sendMessage(Component.empty()
				                   .append(Component.text("Paramaters implemented: ", NamedTextColor.GOLD))
				                   .append(parameters.isEmpty() ? Component.text(" NOT implemented.", NamedTextColor.GOLD) : Component.empty()));

			if (!parameters.isEmpty()) {
				player.sendMessage(Component.empty()
					                   .append(Component.text("Name" + (" ".repeat(26)) + "value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));

				for (String paramName : parameters.keySet()) {
					int spaces = 30 - paramName.length();
					player.sendMessage(Component.empty()
						                   .append(Component.text("- " + paramName + (" ".repeat(spaces)), NamedTextColor.GOLD))
						                   .append(Component.text(parameters.get(paramName))));
				}
			}
		} else {
			player.sendMessage(Component.empty()
				                   .append(Component.text("[bosstag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
				                   .append(Component.text(bossTag, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
				                   .append(Component.text(" is not implemented", NamedTextColor.RED)));
		}
	}

	private static void searchTag(Player player, String bossTag) throws WrapperCommandSyntaxException {
		int indexBracket = bossTag.indexOf("[");
		String realBossTag = bossTag;
		Map<String, String> param = new LinkedHashMap<>();

		if (indexBracket != -1) {
			realBossTag = bossTag.substring(0, indexBracket);
			BossUtils.addModifiersFromString(param, realBossTag);
		}

		List<Soul> soulsList = new ArrayList<>();
		Set<String> soulsName = LibraryOfSoulsAPI.getSoulNames();

		Boolean shouldAdd = false;

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
			shouldAdd = false;


			if (nbtTagsList != null && nbtTagsList.getAsArray() != null) {
				for (Object tag : nbtTagsList.getAsArray()) {

					if (((String) tag).contains(realBossTag)) {
						shouldAdd = true;

						if (!param.isEmpty()) {
							//we have params to checks
							Map<String, String> currentParam = new LinkedHashMap<>();

							//load a map with the params that the soul has
							for (Object tagSecond : nbtTagsList.getAsArray()) {
								if (((String) tagSecond).startsWith(realBossTag + "[")) {
									BossUtils.addModifiersFromString(currentParam, ((String) tagSecond).replace(realBossTag, ""));
								}
							}

							for (String paramKey : param.keySet()) {
								if (!param.get(paramKey).equals(currentParam.get(paramKey))) {
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
			CommandAPI.fail("You must use /bosstag seach " + bossTag + " before using this command");
		}

		if (souls.size() == 0) {
			SEARCH_OUTCOME_MAP.remove(bossTag);
			CommandAPI.fail("No BookOfSouls remaining for tag: " + bossTag);
		}

		Soul bos = souls.get(0);
		souls.remove(0);

		player.getInventory().addItem(bos.getBoS());

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Book Of Soul of " + bos.getLabel(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

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
				                   .append(Component.text(" and all the dependency", NamedTextColor.GRAY))
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
			                   .append(Component.text("Tags removed!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
	}

	private static Tooltip<String>[] suggestionBossTagBasedonBoS(SuggestionInfo info) {
		try {
			BookOfSouls bos = getBos((Player)info.sender());
			NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

			List<Tooltip<String>> bossTags = new ArrayList<>();

			for (Object tag : nbtTagsList.getAsArray()) {
				String tagString = (String) tag;

				if (BossManager.mBossParameters.get(tagString) != null) {
					bossTags.add(Tooltip.of(tagString, null));
					//TODO- write a better Tooltip
				}
			}

			return bossTags.toArray(Tooltip.arrayOf());

		} catch (Exception e) {
			info.sender().sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
		}

		return Tooltip.arrayOf();

	}

	private static Tooltip<String>[] suggestionBossTagBasedonBoSAndParams(SuggestionInfo info) {
		try {
			BookOfSouls bos = getBos((Player)info.sender());
			NBTTagList nbtTagsList = bos.getEntityNBT().getData().getList("Tags");

			if (nbtTagsList != null) {
				List<Tooltip<String>> bossTags = new ArrayList<>();
				List<String> bossTagList = new ArrayList<>();
				Map<String, Map<String, String>> paramsMap = new LinkedHashMap<>();

				for (Object tag : nbtTagsList.getAsArray()) {
					String tagString = (String) tag;

					if (BossManager.mBossParameters.get(tagString) != null) {
						bossTagList.add(tagString);
						paramsMap.put(tagString, new LinkedHashMap<>());
						bossTags.add(Tooltip.of(tagString, null));
						//TODO- write a better Tooltip
					}
				}

				//we need a better way to do this.
				for (Object tag : nbtTagsList.getAsArray()) {
					String tagString = (String) tag;
					for (String bossTag : bossTagList) {
						if (tagString.startsWith(bossTag + "[")) {
							BossUtils.addModifiersFromString(paramsMap.get(bossTag), tagString.replace(bossTag, ""));
						}
					}
				}
				//TODO-finish this part.
				return bossTags.toArray(Tooltip.arrayOf());
			}

		} catch (Exception e) {
			info.sender().sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
		}

		return Tooltip.arrayOf();
	}

	private static void squashBossTags(Player player) throws WrapperCommandSyntaxException {
		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Start squashing!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
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
			for (String bossTag : bossTagParamertersMap.keySet()) {
				if (((String)nbtTagsList.get(i)).startsWith(bossTag + "[")) {
					//found some parameters
					BossUtils.addModifiersFromString(bossTagParamertersMap.get(bossTag), ((String)nbtTagsList.get(i)).replace(bossTag, ""));
					tags.remove(i);
					break;
				}
			}
		}

		//recreate the tags and load back
		List<String> nextTags = new ArrayList<>();
		String tag;
		for (String bossTag : bossTagParamertersMap.keySet()) {
			if (!bossTagParamertersMap.get(bossTag).isEmpty()) {
				tag = bossTag + "[";
				Map<String, String> parametersMap = bossTagParamertersMap.get(bossTag);
				BossParameters parameters = BossManager.mBossParameters.get(bossTag);

				//following order of implementations inside the class
				//TODO-we may want to separate in 3 different (generics-sounds-particles)
				for (Field field : parameters.getClass().getFields()) {
					String translated = BossUtils.translateFieldNameToTag(field.getName());
					if (parametersMap.containsKey(translated)) {
						tag += translated + "=" + parametersMap.get(translated) + ",";

						if (tag.length() >= MAXIMUS_TAG_LENGHT) {
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
			                   .append(Component.text("Squash complited!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
	}

	public static void checksAllLos(Player player) throws WrapperCommandSyntaxException {
		Map<Soul, List<String>> soulsBosTagMap = new LinkedHashMap<>();

		player.sendMessage(Component.empty()
			                   .append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			                   .append(Component.text("Working! it may take a while...", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);
		for (String soulName : LibraryOfSoulsIntegration.getSoulNames()) {
			Soul soul = SoulsDatabase.getInstance().getSoul(soulName);
			NBTTagList tags = soul.getNBT().getList("Tags");
			if (tags != null) {
				for (int i = 0; i < tags.size(); i++) {
					String tag = (String) tags.get(i);
					if (BossManager.mBossParameters.get(tag) != null) {
						//this Soul may be with some parameters, lets save it to check later
						if (soulsBosTagMap.get(soul) == null) {
							soulsBosTagMap.put(soul, new ArrayList<>());
						}
						soulsBosTagMap.get(soul).add(tag);
					}
				}
			}
		}

		player.sendMessage(Component.empty()
								.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
								.append(Component.text("Checked all the mobs in the LoS", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		Map<Soul, List<String>> soulDeprecatedTagMap = new LinkedHashMap<>();

		for (Map.Entry<Soul, List<String>> entry : soulsBosTagMap.entrySet()) {
			Soul soul = entry.getKey();
			List<String> bossTags = entry.getValue();
			NBTTagList tags = soul.getNBT().getList("Tags");

			for (String bossTag : bossTags) {
				for (int i = 0; i < tags.size(); i++) {
					String tag = (String) tags.get(i);
					if (tag.startsWith(bossTag + "[")) {
						//found a param string
						StringReader reader = new StringReader(tag);
						reader.advance(bossTag);

						ParseResult<?> result = BossParameters.parseParameters(reader, BossManager.mBossParameters.get(bossTag));

						if (result.getResult() == null || result.mContainsDeprecated) {
							//we get a deprecated tag somehow
							//lets save it to give it to the player
							if (soulDeprecatedTagMap.get(soul) == null) {
								soulDeprecatedTagMap.put(soul, new ArrayList<>());
							}

							soulDeprecatedTagMap.get(soul).add(tag);
						}
					}
				}
			}
		}

		player.sendMessage(Component.empty()
								.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
								.append(Component.text("Found some problems on: " + soulDeprecatedTagMap.keySet().size(), NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);


		player.sendMessage(Component.empty()
								.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
								.append(Component.text("Summoning chest", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
		);

		Location loc = player.getLocation().add(1, 0, 0);
		Block block = loc.getBlock();
		block.setType(Material.CHEST);
		Chest chest = ((Chest)block.getState());
		Inventory chestInventory = chest.getBlockInventory();
		int startPoint = 0;
		for (Map.Entry<Soul, List<String>> entry : soulDeprecatedTagMap.entrySet()) {
			Soul soul = entry.getKey();
			List<String> bossTags = entry.getValue();

			ItemStack book = soul.getBoS();
			ItemStack paper = buildPaper(soul.getLabel(), bossTags);

			chestInventory.addItem(book, paper);
			startPoint += 2;

			if (startPoint >= 26) {
				//the chest is full, create a new one
				startPoint = 0;
				loc = loc.add(1, 0, 0);
				block = loc.getBlock();
				block.setType(Material.CHEST);
				chest = ((Chest)block.getState());
				chestInventory = chest.getBlockInventory();
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
			                   .append(Component.text(" Add the bosstag to the holding BoS", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag add <boss_tag[...]>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag add ")))
			                   .append(Component.text(" Add the bosstag to the holding BoS and checks if the BoS has the current boss_tag implemented", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag remove <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag remove ")))
			                   .append(Component.text(" Remove the boss_tag and all the dependecies to the holding BoS", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag remove <boss_tag[...]>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag remove ")))
			                   .append(Component.text(" Remove the parameters selected inside [...] for the boss_tag selected", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag show", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag show")))
			                   .append(Component.text(" Show all the bosstag implemented and parameters in the holding BoS", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag show <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag show ")))
			                   .append(Component.text(" Show the parameters implemented for the selected boss_tag in the holding BoS", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag search <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag search ")))
			                   .append(Component.text(" Search inside the BoS database for matching tags", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag search <boss_tag[...]>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag search ")))
			                   .append(Component.text(" Search inside the BoS database for matching tags with the selected parameters", NamedTextColor.GRAY))
		);


		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag get <search_id>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag get ")))
			                   .append(Component.text(" Get the first BoS of the outcome of /bosstag search <search_id> ", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("/bosstag info <boss_tag>", NamedTextColor.GOLD).clickEvent(ClickEvent.suggestCommand("/bosstag info ")))
			                   .append(Component.text(" Show the info about the boss_tag selected and the default value of the parameters ", NamedTextColor.GRAY))
		);

		player.sendMessage(Component.empty()
			                   .append(Component.text("===========================================", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true))
		);
	}
}
