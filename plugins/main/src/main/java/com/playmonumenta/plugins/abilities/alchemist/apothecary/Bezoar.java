package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Bezoar: When the Apothercary kills an enemy, if 5/3 enemies
 * have died since the last drop, the enemy drops a Bezoar on the ground
 * that lingers for 4/6 seconds. Any player walking over
 * the stone consumes it, healing 4/6 HP and ending
 * non-infinite Poison and Wither. If a mob walks over
 * the stone, it explodes in a 3 block radius for 4/6 damage.
 */

public class Bezoar extends Ability {

	private static final int BEZOAR_1_DAMAGE = 4;
	private static final int BEZOAR_2_DAMAGE = 6;
	private static final int BEZOAR_DAMAGE_RADIUS = 3;

	private int kills = 0;

	public Bezoar(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		// This is spelled incorrectly in both scoreboards and half the code, I am now big sad
		mInfo.scoreboardId = "Bezoar";
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		int per = getAbilityScore() == 1 ? 5 : 3;
		kills++;
		if (kills >= per) {
			kills = 0;
			int duration = getAbilityScore() == 1 ? 20 * 4 : 20 * 6;
			Location loc = event.getEntity().getLocation().add(0, 0.25, 0);
			Item item = mWorld.dropItemNaturally(loc, new ItemStack(Material.LIME_CONCRETE));
			item.setCustomName("Beozar");
			item.setPickupDelay(Integer.MAX_VALUE);

			new BukkitRunnable() {
				int t = 0;
				BlockData fallingDustData = Material.LIME_CONCRETE.createBlockData();
				@Override
				public void run() {
					t++;

					mWorld.spawnParticle(Particle.FALLING_DUST, item.getLocation(), 1, 0.2, 0.2, 0.2, fallingDustData);
					if (t >= 10) {
						for (Player p : PlayerUtils.getNearbyPlayers(item.getLocation(), 0.75)) {
							PlayerUtils.healPlayer(p, duration);
							p.removePotionEffect(PotionEffectType.WITHER);
							p.removePotionEffect(PotionEffectType.POISON);
							item.remove();
							this.cancel();
							mWorld.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
							mWorld.playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
							mWorld.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1f);
							mWorld.spawnParticle(Particle.BLOCK_CRACK, item.getLocation(), 30, 0.15, 0.15, 0.15, 0.75F, Material.LIME_CONCRETE.createBlockData());
							mWorld.spawnParticle(Particle.TOTEM, item.getLocation(), 20, 0, 0, 0, 0.35F);
							break;
						}
						if (EntityUtils.getNearbyMobs(item.getLocation(), 1.5) != null && EntityUtils.getNearbyMobs(item.getLocation(), 1.5).size() != 0) {
							item.remove();
							this.cancel();
							mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.75f);
							mWorld.spawnParticle(Particle.EXPLOSION_HUGE, item.getLocation(), 1, 0, 0, 0, 0);
							for (LivingEntity mob : EntityUtils.getNearbyMobs(item.getLocation(), BEZOAR_DAMAGE_RADIUS)) {
								int damage = getAbilityScore() == 1 ? BEZOAR_1_DAMAGE : BEZOAR_2_DAMAGE;
								EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
							}
						}
					}
					if (t >= duration || item.isDead() || item == null) {
						this.cancel();
						item.remove();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	public void incrementKills() {
		kills++;
	}

}
