package com.playmonumenta.plugins.spawners;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public abstract class SpawnerBreakAction {

	private final String mIdentifier;

	private final Map<String, Object> mParameters;

	public SpawnerBreakAction(String identifier) {
		mIdentifier = identifier;
		mParameters = new HashMap<>();
	}

	public abstract void run(Player player, Block spawner, Map<String, Object> parameters);

	public String getIdentifier() {
		return mIdentifier;
	}

	public Map<String, Object> getParameters() {
		return mParameters;
	}

	public @Nullable Object getParameterValue(String key) {
		return mParameters.get(key);
	}

	public void addParameter(String key, Object value) {
		mParameters.put(key, value);
	}
}
