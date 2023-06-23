package com.playmonumenta.plugins.spawners;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.spawners.actions.ExplodeAction;
import com.playmonumenta.plugins.spawners.actions.KnockupAction;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SpawnerActionManager {
	public static final SpawnerActionManager INSTANCE = new SpawnerActionManager();

	private static final ImmutableMap<String, SpawnerBreakAction> mBreakActions = ImmutableMap.<String, SpawnerBreakAction>builder()
		.put(ExplodeAction.IDENTIFIER, new ExplodeAction())
		.put(KnockupAction.IDENTIFIER, new KnockupAction())
		.build();

	private SpawnerActionManager() {

	}

	public SpawnerActionManager getInstance() {
		return INSTANCE;
	}

	public static void triggerAction(String identifier, Player player, Block block) {
		SpawnerBreakAction action = mBreakActions.get(identifier);
		if (action == null) {
			return;
		}

		action.run(player, block, SpawnerUtils.getStoredParameters(block, identifier));
	}

	public static void triggerActions(List<String> breakActions, Player player, Block block) {
		breakActions.forEach(action -> triggerAction(action, player, block));
	}

	public static @Nullable SpawnerBreakAction getAction(String identifier) {
		return mBreakActions.get(identifier);
	}

	public static Map<String, Object> getActionParameters(String identifier) {
		SpawnerBreakAction action = mBreakActions.get(identifier);
		if (action == null) {
			return Collections.emptyMap();
		}

		return action.getParameters();
	}

	public static boolean actionExists(String identifier) {
		return mBreakActions.containsKey(identifier);
	}

	public static Set<String> getActionKeySet() {
		return mBreakActions.keySet();
	}

}
