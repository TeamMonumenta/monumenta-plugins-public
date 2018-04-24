package mmms.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class AxtalBlockBreak
{
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
		List<Location> badBlockList = check_blocks(loc);
		if (badBlockList.size() > 2)
		{
			for (Location targetLoc : badBlockList)
				targetLoc.getBlock().setType(Material.AIR);
			animation(loc);
		}
	}

	public List<Location> check_blocks(Location loc)
	{
		List<Location> badBlockList = new ArrayList<Location>();
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
						badBlockList.add(testloc.clone());
				}
			}
		}
		return badBlockList;
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
