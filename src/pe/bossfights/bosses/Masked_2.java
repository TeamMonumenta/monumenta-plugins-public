package pe.bossfights.bosses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellConditionalTeleport;
import pe.bossfights.spells.SpellMaskedFrostNova;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.utils.Utils;
import org.bukkit.attribute.Attribute;

public class Masked_2 implements Boss
{
	Plugin plugin;
	LivingEntity boss;
	Location spawnLoc;
	Location endLoc;

	int detection_range = 50;
	String mobName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Masked Man" + ChatColor.RED;

	int taskIDpassive;
	int taskIDactive;
	List<Spell> activeSpells;
	List<Spell> passiveSpells;
	SpellBossBar bossBar;

	public Masked_2(Plugin pl, LivingEntity bossIn, Location spawnLocIn, Location endLocIn)
	{
		plugin = pl;
		boss = bossIn;
		spawnLoc = spawnLocIn;
		endLoc = endLocIn;

		bossBar = new SpellBossBar(plugin);

		activeSpells = Arrays.asList(
		                   new SpellMaskedFrostNova(plugin, boss, 9, 70),
		                   new SpellMaskedShadowGlade(plugin, spawnLoc, 2),
		                   new SpellMaskedSummonBlazes(plugin, boss)
		               );
		passiveSpells = Arrays.asList(
		                    new SpellBlockBreak(boss),
							// Teleport the boss to spawnLoc whenever condition is true
							new SpellConditionalTeleport(boss, spawnLoc, b -> b.getLocation().getY() < 157)
		                );

		bossBar.spell(boss, detection_range);
		bossBar.changeColor(BarColor.RED);
		bossBar.changeStyle(BarStyle.SOLID);

		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				if (Utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
				{
					/* Kill the boss if no players are within range */
					boss.teleport(new Location(spawnLoc.getWorld(), 0, -60, 0));
					return;
				}

				bossBar.update_bar(boss, detection_range);

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

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
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
		boss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		boss.setHealth(bossTargetHp);
	}

	public void death()
	{
		endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	public void unload()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.cancelTask(taskIDpassive);
		scheduler.cancelTask(taskIDactive);
		bossBar.remove();
	}
}
