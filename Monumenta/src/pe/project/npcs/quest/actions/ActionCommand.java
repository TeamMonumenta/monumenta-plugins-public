package pe.project.npcs.quest.actions;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import pe.project.Plugin;

public class ActionCommand implements ActionBase {
	private String mCommand;

	public ActionCommand(JsonElement element) throws Exception {
		mCommand = element.getAsString();
		if (mCommand == null) {
			throw new Exception("Command value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player) {
		//	Because there's no currently good way to run commands we need to run them via the console....janky....I know.
		String commandStr = mCommand.replaceAll("@S", player.getName());
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}
}
