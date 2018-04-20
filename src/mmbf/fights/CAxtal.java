package mmbf.fights;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import mmbf.main.Main;
import mmbf.main.MobSpell;

import mmbf.utils.SpellBossBar;
import mmbf.utils.Utils;

public class CAxtal
{
	Main plugin;
	Plugin spellPlug;
	Command cmd;

	Utils utils = new Utils(plugin);
	int detection_range = 200;
	String mobName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "C'Axtal";
	Damageable boss = null;
	int taskIDpassive = 0;
	int taskIDactive = 0;
	int taskIDupdate = 0;
	Random rand = new Random();
	MobSpell ms;
	String spells[] = { "axtal_wither_aoe 13 4",
	                    "axtal_melee_minions 10 3 3",
	                    "axtal_sneakup",
	                    "axtal_tnt_throw 5 15",
	                    "axtal_death_ray"
	                  };
	String passiveSpells[] = { "axtal_block_break" };
	int spellsCD[] = {0, 0, 0, 0, 0};

	public CAxtal(Main pl)
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
		int hp_del = 1024;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		Bukkit.getServer().dispatchCommand(send, "summon minecraft:wither_skeleton ~ ~20 ~ {Attributes:[{Base:0.25d,Name:\"generic.movementSpeed\"},{Base:" + armor + ".0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:64.0d,Name:\"generic.followRange\"},{Base:2.0d,Name:\"generic.attackDamage\"},{Base:" + bossTargetHp + ".0d,Name:\"generic.maxHealth\"}],Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,FallDistance:0.0f,DeathTime:0s,WorldUUIDMost:-1041596277173696703L,HandDropChances:[-200.1f,-200.1f],PersistenceRequired:1b,Spigot.ticksLived:145,Tags:[\"Tlax\"],Motion:[0.0d,0.0d,0.0d],Leashed:0b,Health:" + bossTargetHp + ".0f,Bukkit.updateLevel:2,LeftHanded:0b,Air:300s,OnGround:1b,Dimension:0,HandItems:[{id:\"minecraft:iron_axe\",Count:1b,tag:{ench:[{lvl:4s,id:16s},{lvl:1s,id:20s}],display:{Name:\"§4§lShaman's Crusher\"}},Damage:0s},{}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],CustomName:\"§4§lC'Axtal\",Fire:-1s,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{ench:[{lvl:3s,id:4s},{lvl:5s,id:3s}],display:{color:4473924}},Damage:0s},{id:\"minecraft:chainmail_leggings\",Count:1b,tag:{ench:[{lvl:3s,id:4s},{lvl:5s,id:3s}]},Damage:0s},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{ench:[{lvl:3s,id:4s},{lvl:5s,id:3s}],display:{color:4473924}},Damage:0s},{id:\"minecraft:skull\",Count:1b,tag:{SkullOwner:{Id:\"05b9f5c4-fb70-40cd-a2c2-628bcd40e0e7\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWE2MzE0ZWFjMzQ0MTZjZTEwYWIyMmMyZTFjNGRjYjQ3MmEzZmViOThkNGUwNGQzZmJiYjg1YTlhNDcxYjE4In19fQ==\"}]}},display:{Lore:[\"§8The mask is overrun by the jungle's wrath.\"],Name:\"§4§lC'Axtal's Corrupted Mask\"}},Damage:3s}],CanPickUpLoot:0b,HurtTime:0s,WorldUUIDLeast:-7560693509725274339L,CustomNameVisible:1b}");
		SpellBossBar bossBar = new SpellBossBar(plugin);

		Runnable passive = new Runnable()
		{
			@Override
			public void run()
			{
				/* Don't progress if players aren't present */
				if (utils.playersInRange(boss.getLocation(), detection_range).isEmpty())
					return;

				if (boss.getHealth() <= 0)
				{
					Bukkit.getServer().dispatchCommand(send, "playsound minecraft:entity.enderdragon.death master @a[r=" + detection_range + "] ~ ~ ~ 100 0.8");
					ms.spellCall(Bukkit.getConsoleSender(), ("commandspell execute @e[tag=BossCenter] ~ ~ ~ /tellraw @a[r=50] [\"\",{\"text\":\"I am free... It ends at last. Is this what death feels like?..\",\"color\":\"dark_red\"}]").split(" "));
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

				for (int i = 0; i < 5; i++)
				{
					if (spellsCD[i] > 0)
						spellsCD[i]--;
				}
				int chosen = rand.nextInt(5);
				while (spellsCD[chosen] > 0)
					chosen = rand.nextInt(5);
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
						{
							boss = (Damageable)entity;
						}
					}
				}

				/* Found the boss entity - start the rest of the fight */
				if (boss != null)
				{
					bossBar.spell(boss, detection_range);
					bossBar.setEvent(100, "commandspell execute @e[tag=Tlax] ~ ~ ~ /tellraw @a[r=50] [\"\",{\"text\":\"At last, the keys are collected. I can be free finally...\",\"color\":\"dark_red\"}]");
					bossBar.setEvent(50, "commandspell execute @e[tag=Tlax] ~ ~ ~ /tellraw @a[r=50] [\"\",{\"text\":\"PLEASE. KILL ME. KAUL HOLDS ONTO MY MIND, BUT I YEARN FOR FREEDOM.\",\"color\":\"dark_red\"}]");
					bossBar.setEvent(25, "commandspell execute @e[tag=Tlax] ~ ~ ~ /tellraw @a[r=50] [\"\",{\"text\":\"YOU ARE CLOSE. END THIS. END THE REVERIE!\",\"color\":\"dark_red\"}]");
					bossBar.setEvent(10, "commandspell execute @e[tag=Tlax] ~ ~ ~ /tellraw @a[r=50] [\"\",{\"text\":\"My servant is nearly dead. You dare to impose your will on the jungle?\",\"color\":\"dark_green\"}]");
					Bukkit.getServer().dispatchCommand(send, "effect @a[r=" + detection_range + "] minecraft:blindness 2 2");
					Bukkit.getServer().dispatchCommand(send, "title @a[r=" + detection_range + "] title [\"\",{\"text\":\"C'Axtal\",\"color\":\"dark_red\",\"bold\":true}]");
					Bukkit.getServer().dispatchCommand(send, "title @a[r=" + detection_range + "] subtitle [\"\",{\"text\":\"The Soulspeaker\",\"color\":\"red\",\"bold\":true}]");
					Bukkit.getServer().dispatchCommand(send, "playsound minecraft:entity.wither.spawn master @a[r=" + detection_range + "] ~ ~ ~ 10 0.7");

					taskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);
					taskIDupdate = scheduler.scheduleSyncRepeatingTask(plugin, update, 1L, 5L);
					taskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 160L);
					this.cancel();
				}

				/* If the boss hasn't been summoned by now, abort the entire fight */
				if (failcount > 50)
				{
					this.cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		return true;

	}
}
