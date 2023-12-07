package com.playmonumenta.plugins.spawners;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

	public abstract void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool);

	public String getIdentifier() {
		return mIdentifier;
	}

	public Map<String, Object> getParameters() {
		return mParameters;
	}

	public @Nullable Object getDefaultParameterValue(String key) {
		return mParameters.get(key);
	}

	public void addParameter(String key, Object value) {
		mParameters.put(key, value);
	}

	// Return the parameter from the passed map if it is set, otherwise return the default value.
	public Object getParameter(Map<String, Object> parameters, String key) {
		Object value = parameters.get(key);
		if (value != null) {
			return value;
		}
		return Objects.requireNonNull(mParameters.get(key));
	}

	// Called every second, use to display effects to show the presence of a spawner break action.
	public void periodicAesthetics(Block spawnerBlock) {

	}
}
