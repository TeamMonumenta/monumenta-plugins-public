package pe.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import pe.bossfights.SpellManager;
import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellConditionalTeleport;
import pe.bossfights.spells.SpellMaskedEldritchBeam;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.spells.SpellPushPlayersAway;
import pe.bossfights.utils.Utils;

public class Masked_1 implements Boss
{
	public static final String identityTag = "BOSS_MASKED_1";

	Plugin plugin;
	LivingEntity boss;
	Location spawnLoc;
	Location endLoc;

	int detection_range = 50;

	int taskIDpassive;
	int taskIDactive;
	SpellManager activeSpells;
	List<Spell> passiveSpells;
	SpellBossBar bossBar;

	public Masked_1(Plugin pl, LivingEntity bossIn, Location spawnLocIn, Location endLocIn)
	{
		plugin = pl;
		boss = bossIn;
		spawnLoc = spawnLocIn;
		endLoc = endLocIn;

		bossBar = new SpellBossBar(plugin);

		activeSpells = new SpellManager(Arrays.asList(
		                                    new SpellMaskedEldritchBeam(plugin, boss),
		                                    new SpellMaskedShadowGlade(plugin, boss.getLocation(), 2),
		                                    new SpellMaskedSummonBlazes(plugin, boss)
		                                ));
		passiveSpells = Arrays.asList(
		                    new SpellBlockBreak(boss),
		                    new SpellPushPlayersAway(boss, 7, 15),
		                    // Teleport the boss to spawnLoc whenever "true" (always)
		                    new SpellConditionalTeleport(boss, spawnLoc, b -> true)
		                );

		bossBar.spell(boss, detection_range);
		bossBar.changeColor(BarColor.WHITE);
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

				activeSpells.runNextSpell();
			}
		};

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		taskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
		taskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
	}

	@Override
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

	@Override
	public void death()
	{
		endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		boss.teleport(new Location(spawnLoc.getWorld(), 0, -60, 0));
	}

	@Override
	public void unload()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.cancelTask(taskIDpassive);
		scheduler.cancelTask(taskIDactive);
		bossBar.remove();
	}
}
