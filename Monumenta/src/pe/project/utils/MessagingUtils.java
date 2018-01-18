package pe.project.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
// https://www.spigotmc.org/wiki/the-chat-component-api/

import net.md_5.bungee.api.ChatMessageType;
/*
ChatMessageType contains the following:
ACTION_BAR
CHAT
SYSTEM

https://ci.md-5.net/job/BungeeCord/ws/chat/target/apidocs/net/md_5/bungee/api/ChatMessageType.html
*/

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class MessagingUtils {
	public static void sendActionBarMessage(Plugin plugin, Player player, String message) {
		TextComponent formattedMessage = new TextComponent( message );
		formattedMessage.setColor( ChatColor.YELLOW );
		player.spigot().sendMessage( ChatMessageType.ACTION_BAR, formattedMessage );
	}

	public static void sendAbilityTriggeredMessage(Plugin plugin, Player player, String message) {
		TextComponent formattedMessage = new TextComponent( message );
		formattedMessage.setColor( ChatColor.RED );
		player.spigot().sendMessage( ChatMessageType.ACTION_BAR, formattedMessage );
	}

	public static void sendNPCMessage(Player player, String displayName, String message) {
		TextComponent formattedMessage = new TextComponent( "[" + displayName + "] " );
		formattedMessage.setColor( ChatColor.GOLD );
		TextComponent tempText = new TextComponent( message );
		tempText.setColor( ChatColor.WHITE );
		formattedMessage.addExtra(tempText);

		player.spigot().sendMessage( formattedMessage );
	}

	public static void sendRawMessage(Player player, String message) {
		TextComponent formattedMessage = new TextComponent( ChatColor.translateAlternateColorCodes('&', message) );
		player.spigot().sendMessage(formattedMessage);
	}

	public static void sendClickableNPCMessage(Plugin plugin, Player player, String message, String commandStr) {
		TextComponent formattedMessage = new TextComponent( "[" + message + "]" );
		formattedMessage.setColor( ChatColor.LIGHT_PURPLE );
		formattedMessage.setClickEvent( new ClickEvent( ClickEvent.Action.RUN_COMMAND, commandStr ) );
		player.spigot().sendMessage(formattedMessage);
	}

	public static void sendStackTrace(CommandSender sender, Exception e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
		  formattedMessage = new TextComponent( errorMessage );
		} else {
		  formattedMessage = new TextComponent( "An error occured without a set message. Hover for stack trace." );
		}
		formattedMessage.setColor( ChatColor.RED );

		// Get the first 1000 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 1000));

		BaseComponent[] textStackTrace = new ComponentBuilder( sStackTrace.replace("\t","  ") ).color( ChatColor.RED ).create();
		formattedMessage.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, textStackTrace ) );
		sender.spigot().sendMessage( formattedMessage );
	}
}
