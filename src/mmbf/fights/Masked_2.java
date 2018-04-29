package mmbf.fights;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import mmbf.main.Main;

import mmbf.utils.SpellBossBar;

import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellMaskedFrostNova;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.utils.Utils;
import pe.bossfights.utils.Utils.ArgumentException;

public class Masked_2
{
	Main plugin;

	int detection_range = 50;
	String targetingTag = "Masked";
	String mobName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Masked Man" + ChatColor.RED;
	Damageable boss = null;
	int taskIDpassive = 0;
	int taskIDactive = 0;
	int taskIDupdate = 0;

	List<Spell> activeSpells = new ArrayList<Spell>();
	List<Spell> passiveSpells = new ArrayList<Spell>();

	public Masked_2(Main pl)
	{
		plugin = pl;
	}

	public boolean spawn(CommandSender send, Location endLoc)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Entity spawnPoint;
		try
		{
			spawnPoint = Utils.calleeEntity(send);
		}
		catch (ArgumentException ex)
		{
			send.sendMessage(ChatColor.RED + ex.getMessage());
			return false;
		}
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(spawnPoint.getLocation(), detection_range).size();
		int hp_del = 256;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		Bukkit.getServer().dispatchCommand(send, "summon wither_skeleton ~ ~1 ~ {CustomName:\"" + mobName + "\",Tags:[\"" + targetingTag + "\"],ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:1052688}}},{id:\"minecraft:diamond_leggings\",Count:1b},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:1052688}}},{id:\"minecraft:skull\",Damage:3,Count:1b,tag:{SkullOwner:{Id:\"bf8d8d03-3eb1-4fa0-9e32-ab87363f2106\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2NhMmM4YTE4NWE5NmQ1NzQ4ZmVlZTgyZGQ2NzMxOWI3OGM3MTgzN2Y0MWI0ZWVkNWU2NmU4MDJjYjViYiJ9fX0=\"}]}}}}],HandItems:[{id:\"minecraft:diamond_sword\",Count:1b,tag:{display:{Name:\"Arcane Gladius\"},ench:[{id:16,lvl:1},{id:19,lvl:1}]}},{}],ArmorDropChances:[-327.67F,-327.67F,-327.67F,-327.67F],Attributes:[{Name:generic.movementSpeed,Base:0.3},{Name:generic.followRange,Base:60},{Base:" + armor + ".0d,Name:\"generic.armor\"},{Base:" + bossTargetHp + ".0d,Name:\"generic.maxHealth\"}],Health:" + bossTargetHp + ",PersistenceRequired:1,Team:\"mask\",DeathLootTable:\"empty\"}");
		SpellBossBar bossBar = new SpellBossBar(plugin);

		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				/* If no players are present, do nothing unless the boss is dead/despawned */
				if (Utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
				{
					/*
					 * If the boss is dead or despawned but no players are nearby
					 * cancel the bossfight silently without triggering reward
					 */
					if (!boss.isValid())
					{
						scheduler.cancelTask(taskIDpassive);
						scheduler.cancelTask(taskIDactive);
						scheduler.cancelTask(taskIDupdate);
						bossBar.remove();
					}
					return;
				}
				if (boss.getHealth() <= 0)
				{
					scheduler.cancelTask(taskIDpassive);
					scheduler.cancelTask(taskIDactive);
					scheduler.cancelTask(taskIDupdate);
					bossBar.remove();
					endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
				for (Spell spell : passiveSpells)
					spell.run();
				/* if boss is in water, tp to center*/
				if (boss.getLocation().getY() < 157)
					boss.teleport(spawnPoint);

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

		/* Only start the boss finder task, which launches the rest */
		new BukkitRunnable()
		{
			int failcount = 0;

			@Override
			public void run()
			{
				failcount++;

				for (Entity entity : spawnPoint.getNearbyEntities(detection_range, detection_range, detection_range))
				{
					String name = entity.getCustomName();
					if (name != null)
					{
						if (name.equalsIgnoreCase(mobName))
							boss = (Damageable)entity;
					}
				}

				/* Found the boss entity - start the rest of the fight */
				if (boss != null)
				{
					activeSpells = Arrays.asList(
						new SpellMaskedFrostNova(plugin, boss, 9, 70),
						new SpellMaskedShadowGlade(plugin, boss.getLocation(), 2),
						new SpellMaskedSummonBlazes(plugin, boss)
					);
					passiveSpells = Arrays.asList(
						new SpellBlockBreak(boss)
					);

					bossBar.spell(boss, detection_range);
					bossBar.changeColor(BarColor.RED);
					bossBar.changeStyle(BarStyle.SOLID);

					taskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
					taskIDupdate = scheduler.scheduleSyncRepeatingTask(plugin, update, 1L, 5L);
					taskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
					this.cancel();
				}

				/* If the boss hasn't been summoned by now, abort the entire fight */
				if (failcount > 50)
					this.cancel();
			}
		}.runTaskTimer(plugin, 0, 1);

		return true;
	}
}
