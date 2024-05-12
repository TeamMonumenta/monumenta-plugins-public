package com.playmonumenta.plugins.bosses.bosses.sirius;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SiriusNPCBoss extends BossAbilityGroup {

	private static final String COMMAND = "siriustalknpc";
	private static final String PERMISSION = "monumenta.commands.siriustalknpc";
	private static final String NPC_TAG = "siriusNPC";

	public static void register() {
		new CommandAPICommand(COMMAND)
			// Syntax:
			// siriustalknpc @S @N
			// @S is the player triggering the command
			// @N is the UUID of the npc entity
			.withPermission(CommandPermission.fromString(PERMISSION))
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				new EntitySelectorArgument.OneEntity("npc")
			)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Entity npc = args.getUnchecked("npc");
				SiriusNPCBoss talkNPC = BossUtils.getBossOfClass(npc, SiriusNPCBoss.class);
				if (talkNPC != null) {
					talkNPC.onInteract(player);
				}
			})
			.register();
	}


	public static @Nullable SiriusNPCBoss construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Sirius.
		Sirius sirius = null;
		List<LivingEntity> slimes = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.SLIME));
		for (LivingEntity mob : slimes) {
			if (mob.getScoreboardTags().contains(Sirius.identityTag)) {
				sirius = BossUtils.getBossOfClass(mob, Sirius.class);
				break;
			}
		}
		if (sirius == null) {
			MMLog.warning("SiriusNPCBoss: Sirius wasn't found! (This is a bug)");
			return null;
		}
		return new SiriusNPCBoss(plugin, boss);
	}

	public static String identityTag = "boss_siriusnpc";

	public SiriusNPCBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		boss.addScoreboardTag(NPC_TAG);
		//Sirius is here in case we want to add mid fight npc talk.
		super.constructBoss(SpellManager.EMPTY, List.of(), 100, null);

	}

	// @Override onHurtByEntityWithSource
	// Due to ScriptedQuests auto-cancelling the damageEvent, we need to use ScriptedQuests
	// interactables / quests to call this function (via a command). Fun!
	public void onInteract(Player player) {
		String npcName = mBoss.getName();
		switch (npcName.toLowerCase(Locale.getDefault())) {
			case "aurora" -> {
				MessagingUtils.sendNPCMessage(player, npcName, Component.text("No time to chit-chat, we've got a Sirius to kill.", NamedTextColor.DARK_PURPLE));
			}
			case "silver knight tuulen" -> {
				MessagingUtils.sendNPCMessage(player, npcName, Component.text("This is a time to fight, not a time to talk. Get back to destroying the Herald and listen for our instructions!", NamedTextColor.GRAY));
			}
			default -> {

			}
		}
	}
}
