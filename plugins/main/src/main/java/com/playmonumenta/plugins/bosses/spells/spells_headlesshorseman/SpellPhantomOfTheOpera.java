package com.playmonumenta.plugins.bosses.spells.spells_headlesshorseman;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 *Phantom of the Opera (Idfk) - Fires out black projectiles at anyone within 12 blocks of himself after
a 0.8 seconds delay. Players hit by these projectiles are blinded for 5 seconds, dealt 16/28 damage
and 2 phantoms are summoned above the player with their aggro set to them.
 */
public class SpellPhantomOfTheOpera extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private HeadlessHorsemanBoss mHorseman;

	public SpellPhantomOfTheOpera(Plugin plugin, LivingEntity entity, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mHorseman = horseman;
	}

	private void launch(Player player) {
		new BukkitRunnable() {
			World world = mBoss.getWorld();
			Vector dir = LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), mBoss.getEyeLocation());
			Location loc = mBoss.getEyeLocation();
			@Override
			public void run() {
				loc.add(dir.clone().multiply(1));
				world.spawnParticle(Particle.SMOKE_NORMAL, loc, 10, 0.15, 0.15, 0.15, 0.125);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.15, 0.15, 0.15, 0.05);

				for (Player p : PlayerUtils.playersInRange(loc, 1.45)) {
					if (mHorseman.getSpawnLocation().distance(p.getLocation()) < HeadlessHorsemanBoss.detectionRange
							&& p.getGameMode() == GameMode.SURVIVAL) {
						world.spawnParticle(Particle.SMOKE_NORMAL, loc, 30, 0.15, 0.15, 0.15, 0.15);
						world.spawnParticle(Particle.SMOKE_LARGE, loc, 15, 0.15, 0.15, 0.15, 0.1);
						world.playSound(loc, Sound.ENTITY_WITHER_HURT, 1, 1.25f);
						if (!player.isBlocking()) {
							p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 0));

						}

						DamageUtils.damage(mBoss, p, 28);
						world.spawnParticle(Particle.SMOKE_LARGE, p.getLocation().add(0, 3.5, 0), 20, 0.4, 0.4, 0.4, 0.1);
						world.spawnParticle(Particle.SMOKE_NORMAL, p.getLocation().add(0, 3.5, 0), 45, 0.4, 0.4, 0.4, 0.145);
						world.spawnParticle(Particle.SPELL_WITCH, p.getLocation().add(0, 3.5, 0), 60, 0.4, 0.4, 0.4, 0.1);
						world.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 0.75f);
						world.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1, 0.8f);
						for (int i = 0; i < 2; i++) {
							Phantom phantom = (Phantom) world.spawnEntity(p.getLocation().add(0, 5, 0), EntityType.PHANTOM);
							phantom.setCustomName("A Winged Flying Thing");
							phantom.setSize(6);
							phantom.setTarget(p);
						}
						this.cancel();
						break;
					}
				}
				if (loc.getBlock().getType().isSolid()) {
					this.cancel();
					return;
				}

			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 3, 0.65f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.05);
				world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 4, 0.4, 0.4, 0.4, 0.05);
				if (t >= 20) {
					this.cancel();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3, 1.1f);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.125);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.09);
					world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 50, 0.4, 0.4, 0.4, 0.05);
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 14)) {
						if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
							launch(player);
						}
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 10;
	}

}
