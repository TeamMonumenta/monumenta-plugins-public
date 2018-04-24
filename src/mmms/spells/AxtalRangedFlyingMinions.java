package mmms.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

public class AxtalRangedFlyingMinions
{
	private Plugin plugin;

	public AxtalRangedFlyingMinions(mmbf.main.Main plugin2)
	{
		plugin = plugin2;
	}

	Random rand = new Random();

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 5)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Ranged_Flying_Minions_1 <Count> <Scope> <Repeats> <fly-duration>");
			return (true);
		}
		boolean error = false;
		int count = Integer.parseInt(arg[1]);
		if (count < 0 || count > 64)
		{
			System.out.println(ChatColor.RED + "Count must be between 0 and 64");
			error = true;
		}
		int scope = Integer.parseInt(arg[2]);
		if (scope < 0 || scope > 32)
		{
			System.out.println(ChatColor.RED + "Scope must be between 0 and 32");
			error = true;
		}
		int repeats = Integer.parseInt(arg[3]);
		if (repeats < 0 || repeats > 5)
		{
			System.out.println(ChatColor.RED + "Repeats must be between 0 and 5");
			error = true;
		}
		int duration = Integer.parseInt(arg[4]);
		if (duration < 0 || duration > 60)
		{
			System.out.println(ChatColor.RED + "Duration must be between 0 and 60");
			error = true;
		}
		if (error)
			return (true);

		spell(sender, count, scope, repeats, duration);
		return true;
	}

	public void spell(CommandSender sender, int count, int scope, int repeats, int duration)
	{
		Entity launcher = null;

		if (sender instanceof Entity)
			launcher = (Entity)sender;
		else if (sender instanceof ProxiedCommandSender)
		{
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Entity)
				launcher = (Entity)callee;
		}
		if (launcher == null)
		{
			System.out.println("wither_aoe spell failed");
			return ;
		}
		Location lLoc = launcher.getLocation();
		animation(lLoc, repeats, launcher);
		spawn(sender, launcher, lLoc, count, scope, repeats, duration);
	}

	public void spawn(CommandSender sender, Entity esender, Location loc, int count, int scope, int repeats, int duration)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable single_spawn = new Runnable()
		{
			@Override
			public void run()
			{
				int nb_to_spawn = count + (rand.nextInt(2 * scope) - scope);
				for (int j = 0; j < nb_to_spawn; j++)
				{
					System.out.println("hey");
					Bukkit.getServer().dispatchCommand(sender, "summon minecraft:skeleton ~ ~4 ~ {HurtByTimestamp:0,Attributes:[{Base:0.0d,Name:\"generic.knockbackResistance\"},{Base:0.25d,Name:\"generic.movementSpeed\"},{Base:0.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:16.0d,Name:\"generic.followRange\"},{Base:2.0d,Name:\"generic.attackDamage\"},{Base:25.0d,Name:\"generic.maxHealth\"}],Invulnerable:0b,FallFlying:0b,PortalCooldown:0,AbsorptionAmount:0.0f,FallDistance:0.0f,DeathTime:0s,WorldUUIDMost:-1041596277173696703L,HandDropChances:[-200.1f,-200.1f],PersistenceRequired:0b,Spigot.ticksLived:434,Motion:[0.0d,-0.0784000015258789d,0.0d],Leashed:0b,Health:25.0f,Bukkit.updateLevel:2,LeftHanded:0b,Air:300s,OnGround:1b,Dimension:0,Rotation:[0.0f,3.0153077f],HandItems:[{id:\"minecraft:bow\",Count:1b,tag:{ench:[{lvl:5s,id:48s},{lvl:1s,id:49s}]},Damage:0s},{}],ArmorDropChances:[-200.1f,-200.1f,-200.1f,-200.1f],CustomName:\"Strong Lost Soul\",Pos:[-1027.5d,82.0d,-1382.5d],Fire:-1s,ArmorItems:[{},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{ench:[{lvl:5s,id:4s},{lvl:10s,id:3s}],display:{color:11579568},AttributeModifiers:[]},Damage:0s},{id:\"minecraft:skull\",Count:1b,Damage:0s}],CanPickUpLoot:0b,DeathLootTable:\"ayylmao\",HurtTime:0s,WorldUUIDLeast:-7560693509725274339L,ActiveEffects:[{Ambient:1b,ShowParticles:1b,Duration:11566,Id:5b,Amplifier:0b},{Ambient:1b,ShowParticles:1b,Duration:999566,Id:14b,Amplifier:0b}]}");
				}
				System.out.println("hey");
				for (Entity skelly : esender.getNearbyEntities(0.2, 5, 0.2))
				{
					System.out.println("hey");
					if (skelly.getType() == EntityType.SKELETON && skelly instanceof LivingEntity)
					{
						double x = Math.cos(((double)rand.nextInt(628) / 100));
						double z = Math.sin(((double)rand.nextInt(628) / 100));
						skelly.setVelocity(new Vector(x, 0f, z));
						((LivingEntity)skelly).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, rand.nextInt(100) + 40, 1));
					}
				}
			}
		};
		for (int i = 0; i < repeats; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, single_spawn, (long)(40 + 15 * i));
	}

	public void animation(Location loc, int repeats, Entity launcher)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable()
		{
			@Override
			public void run()
			{
				Location centerLoc = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1, loc.getZ());
				Location particleLoc = new Location(loc.getWorld(), 0, 0, 0);
				launcher.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));
				centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_GHAST_AMBIENT, 1f, 0.5f);
				for (int j = 0; j < 5; j++)
				{
					while (particleLoc.distance(centerLoc) > 2)
					{
						particleLoc.setX(loc.getX() + ((double)(rand.nextInt(4000) - 2000) / 1000));
						particleLoc.setZ(loc.getZ() + ((double)(rand.nextInt(4000) - 2000) / 1000));
						particleLoc.setY(loc.getY() + ((double)(rand.nextInt(4000) - 2000) / 1000));
					}
					particleLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, particleLoc, 4, 0, 0, 0, 0.01);
					particleLoc.setX(0);
					particleLoc.setY(0);
					particleLoc.setZ(0);
				}
			}
		};
		for (int i = 0; i < (40 + repeats * 15) / 3; i++)
			scheduler.scheduleSyncDelayedTask(this.plugin, anim_loop , (long)i * 3);
	}
}
