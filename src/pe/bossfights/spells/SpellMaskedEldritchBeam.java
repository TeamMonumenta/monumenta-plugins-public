package pe.bossfights.spells;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;

import pe.bossfights.utils.Utils;

public class SpellMaskedEldritchBeam implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;

	public SpellMaskedEldritchBeam(Plugin plugin, Entity launcher)
	{
		mPlugin = plugin;
		mLauncher = launcher;
	}

	@Override
	public void run()
	{
		for (Player player : Utils.playersInRange(mLauncher.getLocation(), 40))
			launch(player);
	}

	private void launch(Player target)
	{
		new BukkitRunnable()
		{
			private int mTicks = 0;

			@Override
			public void run()
			{
				mTicks++;

				if (mTicks % 5 == 0) {
					target.damage(3f);
				}

				Location launLoc = mLauncher.getLocation().add(0, 1.6f, 0);
				Location tarLoc = target.getLocation().add(0, 0.6f, 0);
				tarLoc.getWorld().playSound(tarLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)mTicks / 80f) * 1.5f));
				launLoc.getWorld().playSound(launLoc, Sound.UI_TOAST_IN, 2, (0.5f + ((float)mTicks / 80f) * 1.5f));
				Vector vect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ());
				vect.normalize();
				for (int i = 0; i < 200; i++)
				{
					double dist_to_player = launLoc.distance(tarLoc);
					Vector baseVect = new Vector(vect.getX() / (100 / dist_to_player), vect.getY() / (100 / dist_to_player), vect.getZ() / (100 / dist_to_player));
					Location tmpLoc = launLoc;
					Vector tmpVect = baseVect;
					tmpLoc.add(tmpVect.multiply((double)i));
					tmpLoc.getWorld().spawnParticle(Particle.CLOUD, tmpLoc, 1, 0.02, 0.02, 0.02, 0);
					tmpLoc.getWorld().spawnParticle(Particle.SPELL_MOB, tmpLoc, 1, 0.02, 0.02, 0.02, 1);
					if (tmpLoc.getBlock().getType() != Material.AIR)
					{
						this.cancel();
						break;
					}
					else if (launLoc.distance(tmpLoc) > launLoc.distance(tarLoc))
						break;
					else if (tarLoc.distance(tmpLoc) < 0.5)
						break;
				}
				if (mTicks >= 80)
					this.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}
}
