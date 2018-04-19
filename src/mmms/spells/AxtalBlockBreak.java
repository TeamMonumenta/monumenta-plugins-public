package mmms.spells;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import net.md_5.bungee.api.ChatColor;

public class AxtalBlockBreak
{

	public AxtalBlockBreak()
	{
	}

	public boolean onSpell(CommandSender sender, String[] arg)
	{
		if (arg.length != 1)
		{
			System.out.println(ChatColor.RED + "wrong number of parameters given!\n" + ChatColor.GREEN + "Usage: " + ChatColor.DARK_GREEN + "/mobspell Tnt_Throw <Count> <Cooldown>");
			return (true);
		}
		boolean error = false;
		if (error)
			return (true);

		spell(sender);
		return true;
	}

	public void spell(CommandSender sender)
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
		Location loc = launcher.getLocation();
		if (check_blocks(loc) > 2)
		{
			destroy_blocks(loc);
			animation(loc);
		}
	}

	public int check_blocks(Location loc)
	{
		int count = 0;
		Location testloc = new Location(loc.getWorld(), 0, 0, 0);
		for (int x = -1; x <= 1; x++)
		{
			testloc.setX(loc.getX() + (double)x);
			for (int y = 1; y <= 3; y++)
			{
				testloc.setY(loc.getY() + (double)y);
				for (int z = -1; z <= 1; z++)
				{
					testloc.setZ(loc.getZ() + (double)z);
					Material material = testloc.getBlock().getType();
					if (testloc.getBlock().getType() != Material.BEDROCK && material.isSolid())
						count++;
				}
			}
		}
		return (count);
	}

	public void destroy_blocks(Location loc)
	{
		Location targetloc = new Location(loc.getWorld(), 0, 0, 0);
		for (int x = -1; x <= 1; x++)
		{
			targetloc.setX(loc.getX() + (double)x);
			for (int y = 1; y <= 3; y++)
			{
				targetloc.setY(loc.getY() + (double)y);
				for (int z = -1; z <= 1; z++)
				{
					targetloc.setZ(loc.getZ() + (double)z);
					Material material = targetloc.getBlock().getType();
					if (targetloc.getBlock().getType() != Material.BEDROCK && material.isSolid())
						targetloc.getBlock().setType(Material.AIR);


				}
			}
		}
	}

	public void animation(Location loc)
	{
		loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 3f, 0.6f);
		loc.getWorld().playSound(loc, Sound.ENTITY_IRONGOLEM_HURT, 3f, 0.6f);
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.8f);
		Location particleLoc = loc.add(new Location(loc.getWorld(), -0.5f, 0f, 0.5f));
		particleLoc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, particleLoc, 10, 1, 1, 1, 0.03);
	}
}
