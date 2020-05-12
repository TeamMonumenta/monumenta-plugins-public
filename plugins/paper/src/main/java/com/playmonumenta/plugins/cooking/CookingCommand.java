package com.playmonumenta.plugins.cooking;

import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.GreedyStringArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class CookingCommand {
	public static void register(Plugin plugin) {

		//cooking opentable
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("opentable", new LiteralArgument("opentable"));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runOpenTable(sender, plugin);
			}
		);

		//cooking items
		arguments = new LinkedHashMap<>();
		arguments.put("items", new LiteralArgument("items"));

		//cooking items new
		arguments.put("new", new LiteralArgument("new"));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runCreateNewItem(sender);
			});
		arguments.remove("new");

		//cooking items ShowJson
		arguments.put("ShowJson", new LiteralArgument("ShowJson"));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runShowJson(sender);
			});
		arguments.remove("ShowJson");

		//cooking items update
		arguments.put("update", new LiteralArgument("update"));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runUpdate(sender);
			});
		arguments.remove("update");

		//cooking items var
		arguments.put("var", new LiteralArgument("var"));

		//cooking items var material <material_name>
		arguments.put("material", new LiteralArgument("material"));
		arguments.put("material_name", new DynamicSuggestedStringArgument(ItemUtils::getBukkitMaterialStringArray));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarMaterial(sender, (String)args[0]);
			});
		arguments.remove("material");
		arguments.remove("material_name");

		//cooking items var name <string>
		arguments.put("name", new LiteralArgument("name"));
		arguments.put("newName", new GreedyStringArgument());
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarName(sender, (String)args[0]);
			});
		arguments.remove("name");
		arguments.remove("newName");

		//cooking items var type <type_id>
		arguments.put("type", new LiteralArgument("type"));
		arguments.put("type_id", new DynamicSuggestedStringArgument(CookingUtils::getCookingItemTypeStringArray));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarType(sender, (String)args[0]);
			});
		arguments.remove("type");
		arguments.remove("type_id");

		//cooking items var lore <str>
		arguments.put("lore", new LiteralArgument("lore"));
		arguments.put("str", new GreedyStringArgument());
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarLore(sender, (String)args[0]);
			});
		arguments.remove("lore");
		arguments.remove("str");

		// cooking items var tier <region> <tier>
		arguments.put("tier", new LiteralArgument("tier"));
		arguments.put("region", new DynamicSuggestedStringArgument(() -> {
			ArrayList<String> out = new ArrayList<>();
			for (ItemUtils.ItemRegion r : ItemUtils.ItemRegion.values()) {
				out.add(r.toString());
			}
			return out.toArray(new String[0]);
		}));
		arguments.put("tierr", new DynamicSuggestedStringArgument(() -> {
			ArrayList<String> out = new ArrayList<>();
			for (ItemUtils.ItemTier t : ItemUtils.ItemTier.values()) {
				out.add(t.toString());
			}
			return out.toArray(new String[0]);
		}));
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarTier(sender, (String)args[0], (String)args[1]);
			});
		arguments.remove("tier");
		arguments.remove("region");
		arguments.remove("tierr");

		// cooking items var consumeEffects <key> <value>
		arguments.put("consumeEffects", new LiteralArgument("consumeEffects"));
		arguments.put("key", new DynamicSuggestedStringArgument(CookingEffectsEnum::valuesAsString));
		arguments.put("value", new IntegerArgument());
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarConsumeEffectsAdd(sender, (String)args[0], (int)args[1]);
			});
		arguments.remove("consumeEffects");
		arguments.remove("key");
		arguments.remove("value");

		// cooking items var cookingEffects <key> <value>
		arguments.put("cookingEffects", new LiteralArgument("cookingEffects"));
		arguments.put("key", new DynamicSuggestedStringArgument(CookingEffectsEnum::valuesAsString));
		arguments.put("value", new IntegerArgument());
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarCookingEffectsAdd(sender, (String)args[0], (int)args[1]);
			});
		arguments.remove("cookingEffects");
		arguments.remove("key");
		arguments.remove("value");

		// cooking items var nameModifiers add <value>
		arguments.put("nameModifiers", new LiteralArgument("nameModifiers"));
		arguments.put("add", new LiteralArgument("add"));
		arguments.put("value", new StringArgument());
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarNameModifiersAdd(sender, (String)args[0]);
			});
		arguments.remove("add");
		arguments.remove("value");

		// cooking items var nameModifiers remove <value>
		arguments.put("remove", new LiteralArgument("remove"));
		arguments.put("value", new DynamicSuggestedStringArgument((sender) -> {
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
		CommandAPI.getInstance().register("cooking",
			CommandPermission.fromString("monumenta.cooking"),
			arguments,
			(sender, args) -> {
				CookingCommandMethods.runVarNameModifiersRemove(sender, (String)args[0]);
			});
		arguments.remove("remove");
		arguments.remove("value");
		arguments.remove("nameModifiers");
	}
}
