package com.playmonumenta.plugins.cooking;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemindex.ItemTier;
import com.playmonumenta.plugins.itemindex.Region;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class CookingCommand {
	private static final String COMMAND = "cooking";

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.cooking");

		//cooking opentable
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("opentable", new LiteralArgument("opentable"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runOpenTable(sender, plugin);
			})
			.register();

		//cooking items
		arguments.clear();
		arguments.put("items", new LiteralArgument("items"));

		//cooking items new
		arguments.put("new", new LiteralArgument("new"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runCreateNewItem(sender);
			})
			.register();
		arguments.remove("new");

		//cooking items ShowJson
		arguments.put("ShowJson", new LiteralArgument("ShowJson"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runShowJson(sender);
			})
			.register();
		arguments.remove("ShowJson");

		//cooking items update
		arguments.put("update", new LiteralArgument("update"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runUpdate(sender);
			})
			.register();
		arguments.remove("update");

		//cooking items var
		arguments.put("var", new LiteralArgument("var"));

		//cooking items var material <material_name>
		arguments.put("material", new LiteralArgument("material"));
		arguments.put("material_name", new StringArgument().overrideSuggestions(ItemUtils.getBukkitMaterialStringArray()));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarMaterial(sender, (String)args[0]);
			})
			.register();
		arguments.remove("material");
		arguments.remove("material_name");

		//cooking items var name <string>
		arguments.put("name", new LiteralArgument("name"));
		arguments.put("newName", new GreedyStringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarName(sender, (String)args[0]);
			})
			.register();
		arguments.remove("name");
		arguments.remove("newName");

		//cooking items var type <type_id>
		arguments.put("type", new LiteralArgument("type"));
		arguments.put("type_id", new StringArgument().overrideSuggestions(CookingUtils.getCookingItemTypeStringArray()));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarType(sender, (String)args[0]);
			})
			.register();
		arguments.remove("type");
		arguments.remove("type_id");

		//cooking items var lore <str>
		arguments.put("lore", new LiteralArgument("lore"));
		arguments.put("str", new GreedyStringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarLore(sender, (String)args[0]);
			})
			.register();
		arguments.remove("lore");
		arguments.remove("str");

		// cooking items var tier <region> <tier>
		arguments.put("tier", new LiteralArgument("tier"));
		arguments.put("region", new StringArgument().overrideSuggestions(Region.valuesLowerCase()));
		arguments.put("tierr", new StringArgument().overrideSuggestions(ItemTier.valuesLowerCase()));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarTier(sender, (String)args[0], (String)args[1]);
			})
			.register();
		arguments.remove("tier");
		arguments.remove("region");
		arguments.remove("tierr");

		// cooking items var consumeEffects <key> <value>
		arguments.put("consumeEffects", new LiteralArgument("consumeEffects"));
		arguments.put("key", new StringArgument().overrideSuggestions(CookingEffectsEnum.valuesAsString()));
		arguments.put("value", new IntegerArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarConsumeEffectsAdd(sender, (String)args[0], (int)args[1]);
			})
			.register();
		arguments.remove("consumeEffects");
		arguments.remove("key");
		arguments.remove("value");

		// cooking items var cookingEffects <key> <value>
		arguments.put("cookingEffects", new LiteralArgument("cookingEffects"));
		arguments.put("key", new StringArgument().overrideSuggestions(CookingEffectsEnum.valuesAsString()));
		arguments.put("value", new IntegerArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarCookingEffectsAdd(sender, (String)args[0], (int)args[1]);
			})
			.register();
		arguments.remove("cookingEffects");
		arguments.remove("key");
		arguments.remove("value");

		// cooking items var nameModifiers add <value>
		arguments.put("nameModifiers", new LiteralArgument("nameModifiers"));
		arguments.put("add", new LiteralArgument("add"));
		arguments.put("value", new StringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarNameModifiersAdd(sender, (String)args[0]);
			})
			.register();
		arguments.remove("add");
		arguments.remove("value");

		// cooking items var nameModifiers remove <value>
		arguments.put("remove", new LiteralArgument("remove"));
		arguments.put("value", new StringArgument().overrideSuggestions((sender) -> {
			Player p = CommandUtils.getPlayerFromSender(sender);
			if (p == null) {
				return null;
			}
			String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
			if (json == null) {
				return null;
			}
			return CookingUtils.cookingItemObjectFromJson(json).getNameModifiers();
		}));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				CookingCommandMethods.runVarNameModifiersRemove(sender, (String)args[0]);
			})
			.register();
		arguments.remove("remove");
		arguments.remove("value");
		arguments.remove("nameModifiers");
	}
}
