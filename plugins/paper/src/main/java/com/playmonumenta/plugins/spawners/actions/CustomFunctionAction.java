package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CustomFunctionAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "customfunction";
	public static final String FUNCTION_KEY = "function";

	public CustomFunctionAction() {
		super(IDENTIFIER);
		addParameter(FUNCTION_KEY, "");
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		String function = (String) getParameter(parameters, FUNCTION_KEY);
		Location spawnerLoc = spawner.getLocation();
		if (function.isEmpty() || function.isBlank()) {
			MMLog.warning("Spawner at " + spawnerLoc + " is missing the function parameter.");
			return;
		}

		String namespace = "";
		String path = "";
		boolean buildingNamespace = true;
		for (int i = 0; i < function.length(); i++) {
			char c = function.charAt(i);
			if (c == ':') {
				buildingNamespace = false;
				continue;
			}
			if (buildingNamespace) {
				namespace += c;
			} else {
				path += c;
			}
		}

		if (path.isBlank() || path.isEmpty()) {
			path = namespace;
			namespace = "monumenta";
		}

		NamespacedKey identifier;
		try {
			identifier = new NamespacedKey(namespace, path);
		} catch (IllegalArgumentException ex) {
			MMLog.warning("Spawner at " + spawnerLoc + "has an invalid namespace or path.");
			return;
		}

		if (!FunctionWrapper.getFunctions().contains(identifier)) {
			MMLog.warning("Spawner at " + spawnerLoc + " does not have a valid function set.");
			return;
		}

		String command = String.format("execute in %s positioned %d %d %d run function %s", spawnerLoc.getWorld().getKey().asString(), spawnerLoc.getBlockX(), spawnerLoc.getBlockY(), spawnerLoc.getBlockZ(), identifier);
		NmsUtils.getVersionAdapter().runConsoleCommandSilently(command);
	}
}
