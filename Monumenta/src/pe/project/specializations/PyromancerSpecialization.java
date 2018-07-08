package pe.project.specializations;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import pe.project.Plugin;
import pe.project.classes.Spells;
import pe.project.utils.EntityUtils;
import pe.project.utils.ScoreboardUtils;
import pe.project.utils.particlelib.ParticleEffect;

public class PyromancerSpecialization extends BaseSpecialization {

	public PyromancerSpecialization(Plugin plugin, Random random) {
		super(plugin, random);
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				int inferno = ScoreboardUtils.getScoreboardValue(player, "Inferno");
				/*
				 * Inferno: Sneak left click to conjure a fiery Inferno,
				 * which instantly damages monsters within 8 blocks for
				 * 18/24 damage, while also igniting monsters for 6/9
				 * seconds. Each additional monster adds 3 seconds
				 * to the duration of the fire. (Cooldown: 27s)
				 */
				if (inferno > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.INFERNO)) {
						ParticleEffect.FLAME.display(8, 1, 8, 0.175f, 1000, player.getLocation(), 40);
						ParticleEffect.LAVA.display(8, 1, 8, 0.175f, 500, player.getLocation(), 40);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.85f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f);

						double damage = inferno == 1 ? 18 : 24;
						int fireDur = inferno == 1 ? 20 * 6 : 20 * 9;

						List<Entity> entities = player.getNearbyEntities(8, 3, 8);
						int flameMult = entities.size() - 1;
						for (Entity e : entities) {
							if (EntityUtils.isHostileMob(e)) {
								LivingEntity le = (LivingEntity) e;
								EntityUtils.damageEntity(mPlugin, le, damage, player);
								le.setFireTicks(fireDur + (flameMult * 20));
							}
						}
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.INFERNO, 20 * 27);
					}
				}
			}
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {

		int burningWrath = ScoreboardUtils.getScoreboardValue(player, "BurningWrath");
		/*
		 * Burning Wrath: If the monster attacked is on fire,
		 * the user¡¯s attacks deals 20%/40% more damage. On
		 * level 2, if the monster killed was on fire, all
		 * surrounding enemies within 3 blocks of the killed
		 * monster are ignited for 3 seconds, or if on fire
		 * already, adds an additional 1 second.
		 */
		LivingEntity e = (LivingEntity) event.getEntity();
		if (burningWrath > 0) {
			if (e.getFireTicks() > 0) {
				double dmgMult = burningWrath == 1 ? 1.2 : 1.4;
				double newDamage = event.getDamage() * dmgMult;
				event.setDamage(newDamage);
			}
		}

		if (player.isSprinting()) {
			int ashenHeart = ScoreboardUtils.getScoreboardValue(player, "AshenHeart");
			/*
			 * Ashen Heart: On Sprinting and hitting a monster,
			 * causes a wave of fiery ashes to flow behind the
			 * attacked monster. All enemies within the wave
			 * take 6/10 damage, ignited for 4 seconds, If the
			 * mobs hit were already on fire, they take 2 more
			 * damage. (Cooldown: 10s)
			 */

			if (ashenHeart > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ASHEN_HEART)) {
					double dmg = ashenHeart == 1 ? 6 : 10;
					new BukkitRunnable() {
						double t = 0;
						float xoffset = 0.00F;
						float zoffset = 0.00F;
						double damagerange = 0.75;
						Location loc = e.getLocation();
						Vector direction = player.getLocation().getDirection().normalize();
						public void run() {

							t = t + 0.5;
							xoffset += 0.15F;
							zoffset += 0.15F;
							damagerange += 0.25;
							double x = direction.getX() * t;
							double y = direction.getY() + 0.5;
							double z = direction.getZ() * t;
							player.getLocation().getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f);
							loc.add(x, y, z);
							ParticleEffect.SMOKE_LARGE.display(xoffset, 0.25F, zoffset, 0.075F, 50, loc, 40);
							ParticleEffect.FLAME.display(xoffset, 0.25F, zoffset, 0.05F, 10, loc, 40);

							for (Entity e : loc.getWorld().getNearbyEntities(loc, damagerange, 1.25, damagerange)) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity le = (LivingEntity) e;
									double dmgAdd = 0;
									if (le.getFireTicks() > 0) {
										dmgAdd += 2;
									}
									EntityUtils.damageEntity(mPlugin, le, dmg + dmgAdd, player);
									le.setFireTicks(20 * 4);
								}
							}
							loc.subtract(x, y, z);


							if (t > 7.5) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.ASHEN_HEART, 20 * 27);
				}
			}
		}
		return true;
	}

	@Override
	public void EntityDeathEvent(Player player, EntityDeathEvent event) {
		int burningWrath = ScoreboardUtils.getScoreboardValue(player, "BurningWrath");
		/*
		 * Burning Wrath: If the monster attacked is on fire,
		 * the user¡¯s attacks deals 20%/40% more damage. On
		 * level 2, if the monster killed was on fire, all
		 * surrounding enemies within 3 blocks of the killed
		 * monster are ignited for 3 seconds, or if on fire
		 * already, adds an additional 1 second.
		 */
		LivingEntity e = event.getEntity();
		DamageCause cause = e.getLastDamageCause().getCause();
		if (burningWrath > 0) {
			if (e.getFireTicks() > 0) {
				if (cause == DamageCause.ENTITY_ATTACK) {
					ParticleEffect.FLAME.display(3, 1, 3, 0.1f, 125, e.getLocation(), 40);
					ParticleEffect.LAVA.display(3, 1, 3, 0.1f, 25, e.getLocation(), 40);
					for (Entity ne : e.getNearbyEntities(3, 3, 3)) {
						if (EntityUtils.isHostileMob(ne)) {
							LivingEntity le = (LivingEntity) ne;
							if (le.getFireTicks() > 0) {
								le.setFireTicks(le.getFireTicks() + 20 * 1);
							} else {
								le.setFireTicks(20 * 3);
							}
						}
					}
				}
			}
		}
	}

}
