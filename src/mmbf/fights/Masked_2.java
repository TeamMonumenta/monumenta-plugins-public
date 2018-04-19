package mmbf.fights;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitScheduler;

import mmbf.main.Main;
import mmbf.main.MobSpell;
import mmbf.utils.SpellBossBar;
import mmbf.utils.Utils;

public class Masked_2
{
	Main plugin;
	MobSpell ms;

	Utils utils = new Utils(plugin);
	int detection_range = 50;
	String targetingTag = "Masked";
	String mobName = "§4§lMasked Man";
	Damageable boss = null;
	int taskIDpassive = 0;
	int taskIDactive = 0;
	int taskIDupdate = 0;
	Random rand = new Random();
	String spells[] = { "masked_frost_nova 9 70",
	                    "commandspell execute @e[tag=MaskedSpawn] ~ ~ ~ mobspell masked_shadow_glade 2",
	                    "masked_summon_blazes"
	                  };
	String passiveSpells[] = { "axtal_block_break" };
	int spellsCD[] = new int[spells.length];

	public Masked_2(Main pl)
	{
		plugin = pl;
		ms = new MobSpell(pl);
	}

	public boolean spawn(CommandSender send, Location endLoc)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Entity spawnPoint = utils.calleeEntity(send);
		int bossTargetHp = 0;
		int player_count = utils.playersInRange(spawnPoint.getLocation(), detection_range).size();
		int hp_del = 256;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		Bukkit.getServer().dispatchCommand(send, "summon wither_skeleton ~ ~1 ~ {CustomName:\"" + mobName + "\",Tags:[\"" + targetingTag + "\"],ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:1052688}}},{id:\"minecraft:diamond_leggings\",Count:1b},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:1052688}}},{id:\"minecraft:skull\",Damage:3,Count:1b,tag:{SkullOwner:{Id:\"bf8d8d03-3eb1-4fa0-9e32-ab87363f2106\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2NhMmM4YTE4NWE5NmQ1NzQ4ZmVlZTgyZGQ2NzMxOWI3OGM3MTgzN2Y0MWI0ZWVkNWU2NmU4MDJjYjViYiJ9fX0=\"}]}}}}],HandItems:[{id:\"minecraft:diamond_sword\",Count:1b,tag:{display:{Name:\"Arcane Gladius\"},ench:[{id:16,lvl:1},{id:19,lvl:1}]}},{}],ArmorDropChances:[-327.67F,-327.67F,-327.67F,-327.67F],Attributes:[{Name:generic.movementSpeed,Base:0.3},{Name:generic.followRange,Base:60},{Base:" + armor + ".0d,Name:\"generic.armor\"},{Base:" + bossTargetHp + ".0d,Name:\"generic.maxHealth\"}],Health:" + bossTargetHp + ",PersistenceRequired:1,Team:\"mask\",DeathLootTable:\"empty\"}");
		List<Entity> lel = spawnPoint.getNearbyEntities(0.1, 3.1, 0.1);
		if (lel != null && !lel.isEmpty() && lel.get(0) instanceof Damageable)
			boss = (Damageable)(lel.get(0));
		else
			return (utils.errorMsg("Something went wrong with the bossfight, if it keeps happening, please contact a mod"));
		SpellBossBar bossBar = new SpellBossBar(plugin);
		bossBar.spell(boss, detection_range);
		bossBar.changeColor(BarColor.RED);
		bossBar.changeStyle(BarStyle.SOLID);
		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				/* If no players are present, do nothing unless the boss is dead/despawned */
				if (utils.playersInRange(boss.getLocation(), detection_range).isEmpty()) {
					/*
					 * If the boss is dead or despawned but no players are nearby
					 * cancel the bossfight silently without triggering reward
					 */
					if (!boss.isValid()) {
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
				for (int i = 0; i < passiveSpells.length; i++)
				{
					ms.spellCall((CommandSender)boss, passiveSpells[i].split(" "));
				}
			}
		};
		Runnable active = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
					return;

				int sps = spells.length;
				for (int i = 0; i < sps; i++)
				{
					if (spellsCD[i] > 0)
						spellsCD[i]--;
				}
				int chosen = rand.nextInt(sps);
				while (spellsCD[chosen] > 0)
					chosen = rand.nextInt(sps);
				spellsCD[chosen] = 3;
				ms.spellCall((CommandSender)boss, spells[chosen].split(" "));
			}
		};
		Runnable update = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
					return;

				bossBar.update_bar(boss, detection_range);
			}
		};
		taskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
		taskIDupdate = scheduler.scheduleSyncRepeatingTask(plugin, update, 1L, 5L);
		taskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
		return true;
	}
}
