package pe.bossfights.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mmbf.utils.SpellBossBar;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellMaskedEldritchBeam;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.utils.Utils;
import pe.bossfights.utils.Utils.ArgumentException;

public class Masked_1 implements Boss
{
	Plugin plugin;
	Damageable boss;
	Location spawnLoc;
	Location endLoc;

	int detection_range = 50;
	String mobName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Masked Man";

	int taskIDpassive;
	int taskIDactive;
	int taskIDupdate;
	List<Spell> activeSpells;
	List<Spell> passiveSpells;
	SpellBossBar bossBar;

	public Masked_1(Plugin pl, Damageable bossIn, Location spawnLocIn, Location endLocIn)
	{
		plugin = pl;
		boss = bossIn;
		spawnLoc = spawnLocIn;
		endLoc = endLocIn;

		bossBar = new SpellBossBar(plugin);

		activeSpells = Arrays.asList(
		                   new SpellMaskedEldritchBeam(plugin, boss),
		                   new SpellMaskedShadowGlade(plugin, boss.getLocation(), 2),
		                   new SpellMaskedSummonBlazes(plugin, boss)
		               );
		passiveSpells = Arrays.asList(
		                    new SpellBlockBreak(boss)
		                );

		bossBar.spell(boss, detection_range);
		bossBar.changeColor(BarColor.WHITE);
		bossBar.changeStyle(BarStyle.SOLID);

		Runnable passive = new Runnable()
		{
			/* Tracks how long players have been too close to the boss */
			Map<UUID, Integer> playerNearTime = new HashMap<UUID, Integer>();

			@Override
			public void run()
			{
				if (Utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
				{
					/* Kill the boss if no players are within range */
					boss.teleport(new Location(spawnLoc.getWorld(), 0, -60, 0));
					return;
				}

				/* TODO: Convert this to a spell */
				boss.teleport(spawnLoc);

				for (Spell spell : passiveSpells)
					spell.run();

				/* Push players away that have been too close for too long */
				/* TODO: Convert this to a spell */
				for (Player player : Utils.playersInRange(boss.getLocation(), detection_range))
				{
					Integer nearTime = 0;
					Location pLoc = player.getLocation();
					if (pLoc.distance(boss.getLocation()) < 7)
					{
						nearTime = playerNearTime.get(player.getUniqueId());
						if (nearTime == null)
							nearTime = 0;
						nearTime++;
						if (nearTime > 15)
						{
							Location lLoc = boss.getLocation();
							Vector vect = new Vector(pLoc.getX() - lLoc.getX(), 0, pLoc.getZ() - lLoc.getZ());
							vect.normalize().setY(0.7f).multiply(2);
							player.setVelocity(vect);
						}
					}
					playerNearTime.put(player.getUniqueId(), nearTime);
				}
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
		int hp_del = 256;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		/* TODO: Set health, maxhealth, armor */
		/*
		Bukkit.getServer().dispatchCommand(send,
		                                   "summon wither_skeleton ~ ~1 ~ {CustomName:\"" + mobName + "\",ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:1052688}}},{id:\"minecraft:diamond_leggings\",Count:1b},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:1052688}}},{id:\"minecraft:skull\",Damage:3,Count:1b,tag:{SkullOwner:{Id:\"bf8d8d03-3eb1-4fa0-9e32-ab87363f2106\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2NhMmM4YTE4NWE5NmQ1NzQ4ZmVlZTgyZGQ2NzMxOWI3OGM3MTgzN2Y0MWI0ZWVkNWU2NmU4MDJjYjViYiJ9fX0=\"}]}}}}],HandItems:[{id:\"minecraft:bow\",Count:1b,tag:{display:{Name:\"" +
		                                   ChatColor.DARK_GRAY + ChatColor.BOLD + "Shadow's Flames\"},ench:[{id:48,lvl:2},{id:49,lvl:1},{id:50,lvl:1}]}},{}],ArmorDropChances:[-327.67F,-327.67F,-327.67F,-327.67F],Attributes:[{Name:generic.knockbackResistance,Base:1},{Name:generic.movementSpeed,Base:0.0},{Name:generic.followRange,Base:60},{Base:" + armor + ".0d,Name:\"generic.armor\"},{Base:" + bossTargetHp + ".0d,Name:\"generic.maxHealth\"}],Health:" + bossTargetHp + ",PersistenceRequired:1,Team:\"mask\",DeathLootTable:\"empty\"}");
		*/
	}

	public void death()
	{
		endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		boss.teleport(new Location(spawnLoc.getWorld(), 0, -60, 0));
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
