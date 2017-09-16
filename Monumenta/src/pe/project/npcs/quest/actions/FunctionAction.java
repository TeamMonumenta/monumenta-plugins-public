package pe.project.npcs.quest.actions;

import org.bukkit.entity.Player;

import pe.project.Main;

public class FunctionAction implements BaseAction {
	private String mFunctionFileName;
	
	public FunctionAction(String functionFileName) {
		mFunctionFileName = functionFileName;
	}

	@Override
	public actionType getType() {
		return actionType.Function;
	}

	@Override
	public void trigger(Main plugin, Player player) {
		//	Because there's no currently good way to run functions we need to run them via the console....janky....I know.
		String commandStr = String.format("execute %s ~ ~ ~ function %s", player.getName(), mFunctionFileName);
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}
}
