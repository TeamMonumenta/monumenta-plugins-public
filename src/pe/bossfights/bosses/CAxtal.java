package pe.bossfights.bosses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellAxtalDeathRay;
import pe.bossfights.spells.SpellAxtalMeleeMinions;
import pe.bossfights.spells.SpellAxtalSneakup;
import pe.bossfights.spells.SpellAxtalTntThrow;
import pe.bossfights.spells.SpellAxtalWitherAoe;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.utils.Utils;
import org.bukkit.attribute.Attribute;

public class CAxtal implements Boss
{
	Plugin plugin;
	LivingEntity boss;
	Location spawnLoc;
	Location endLoc;

	int detection_range = 200;
	String mobName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "C'Axtal";

	int taskIDpassive;
	int taskIDactive;
	int taskIDupdate;
	List<Spell> activeSpells;
	List<Spell> passiveSpells;
	SpellBossBar bossBar;

	public CAxtal(Plugin pl, LivingEntity bossIn, Location spawnLocIn, Location endLocIn)
	{
		plugin = pl;
		boss = bossIn;
		spawnLoc = spawnLocIn;
		endLoc = endLocIn;

		activeSpells = Arrays.asList(
		                   new SpellAxtalWitherAoe(plugin, boss, 13, 4),
		                   new SpellAxtalMeleeMinions(plugin, boss, 10, 3, 3),
		                   new SpellAxtalSneakup(plugin, boss),
		                   new SpellAxtalTntThrow(plugin, boss, 5, 15),
		                   new SpellAxtalDeathRay(plugin, boss)
		               );
		passiveSpells = Arrays.asList(
		                    new SpellBlockBreak(boss)
		                );

		bossBar = new SpellBossBar(plugin);

		//create bossbar
		bossBar.spell(boss, detection_range);
		//schedule hp messages
		Location loc = boss.getLocation();
		bossBar.setEvent(100, Utils.getExecuteCommandOnNearbyPlayers(loc, detection_range, "tellraw @s [\"\",{\"text\":\"At last, the keys are collected. I can be free finally...\",\"color\":\"dark_red\"}]"));
		bossBar.setEvent(50,  Utils.getExecuteCommandOnNearbyPlayers(loc, detection_range, "tellraw @s [\"\",{\"text\":\"PLEASE. KILL ME. KAUL HOLDS ONTO MY MIND, BUT I YEARN FOR FREEDOM.\",\"color\":\"dark_red\"}]"));
		bossBar.setEvent(25,  Utils.getExecuteCommandOnNearbyPlayers(loc, detection_range, "tellraw @s [\"\",{\"text\":\"YOU ARE CLOSE. END THIS. END THE REVERIE!\",\"color\":\"dark_red\"}]"));
		bossBar.setEvent(10,  Utils.getExecuteCommandOnNearbyPlayers(loc, detection_range, "tellraw @s [\"\",{\"text\":\"My servant is nearly dead. You dare to impose your will on the jungle?\",\"color\":\"dark_green\"}]"));

		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (Utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
					return;

				for (Spell spell : passiveSpells)
					spell.run();
			}
		};
		Runnable active = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (Utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
					return;

				/* Run an active spell from the list of available spells */
				// TODO: Add the cooldown back in to prevent re-running the same command twice in a row
				Collections.shuffle(activeSpells);
				activeSpells.get(0).run();
			}
		};
		Runnable update = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (Utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
					return;
				bossBar.update_bar(boss, detection_range);
			}
		};

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
		taskIDupdate = scheduler.scheduleSyncRepeatingTask(plugin, update, 1L, 5L);
		taskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
	}

	public void init()
	{
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(boss.getLocation(), detection_range).size();
		int hp_del = 1024;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		boss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		boss.setHealth(bossTargetHp);

		//launch event related spawn commands
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detection_range, "effect @s minecraft:blindness 2 2");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detection_range, "title @s title [\"\",{\"text\":\"C'Axtal\",\"color\":\"dark_red\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detection_range, "title @s subtitle [\"\",{\"text\":\"The Soulspeaker\",\"color\":\"red\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detection_range, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	public void death()
	{
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detection_range, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detection_range, "tellraw @s [\"\",{\"text\":\"It ends at last... Is this what freedom feels like?..\",\"color\":\"dark_red\"}]");
		endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	public void unload()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.cancelTask(taskIDpassive);
		scheduler.cancelTask(taskIDactive);
		scheduler.cancelTask(taskIDupdate);
		bossBar.remove();
	}
}
