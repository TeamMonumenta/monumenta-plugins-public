package pe.bossfights.spells;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.Sound;

import mmbf.utils.Utils;

public class SpellAxtalWitherAoe implements Spell
{
	private Plugin mPlugin;
	private Entity mLauncher;
	private int mRadius;
	private int mPower;
	private Random mRand = new Random();
	private int w;

	public SpellAxtalWitherAoe(Plugin plugin, Entity launcher, int radius, int power)
	{
		mPlugin = plugin;
		mLauncher = launcher;
		mRadius = radius;
		mPower = power;
	}

	@Override
	public void run()
	{
		w = -80;
		animation();
		deal_damage();
	}

	private void deal_damage()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable dealer = new Runnable()
		{
			@Override
			public void run()
			{
				for (Player player : Utils.playersInRange(mLauncher.getLocation(), mRadius))
				{
					double distance = player.getLocation().distance(mLauncher.getLocation());
					int pot_pow = (int)((double)mPower * (((double)mRadius - distance) / (double)mRadius));
					player.addPotionEffect((new PotionEffect(PotionEffectType.HARM, 1, pot_pow)));
					player.addPotionEffect((new PotionEffect(PotionEffectType.BLINDNESS, 30, 1)));
				}
			}
		};
		scheduler.scheduleSyncDelayedTask(mPlugin, dealer, 80L);
	}

	private void animation()
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable anim_loop = new Runnable()
		{
			@Override
			public void run()
			{
				Location lloc = mLauncher.getLocation();
				int  n = mRand.nextInt(50) + 100;
				double precision = n;
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(lloc.getWorld(), 0, lloc.getY() + 1.5, 0);
				double rad = mRadius * (w < 0 ? ((double)w / 80) : ((double)w / 5));
				double angle = 0;
				for (int j = 0; j < precision; j++)
				{
					angle = (double)j * increment;
					particleLoc.setX(lloc.getX() + (rad * Math.cos(angle)));
					particleLoc.setZ(lloc.getZ() + (rad * Math.sin(angle)));
					particleLoc.setY(lloc.getY() + 1.5);
					particleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.02, 1.5 * rad, 0.02, 0);
				}
				if (w < -20 && w % 2 == 0)
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_CAT_HISS, ((float)mRadius / 7), (float)(0.5 + ((float)(w + 60) / 100)));
				else if (w == -1)
				{
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)mRadius / 7), 0.77F);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)mRadius / 7), 0.5F);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_WITHER_SHOOT, ((float)mRadius / 7), 0.65F);
				}
				w++;
			}
		};

		for (int i = -80; i < 5; i++)
			scheduler.scheduleSyncDelayedTask(mPlugin, anim_loop, 1L * (i + 81));
	}
}
