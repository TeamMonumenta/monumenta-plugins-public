package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellCastEvent;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellBaseBolt;
import com.playmonumenta.bossfights.spells.SpellBaseCharge;
import com.playmonumenta.bossfights.spells.SpellBaseParticleAura;
import com.playmonumenta.bossfights.spells.SpellBlockBreak;
import com.playmonumenta.bossfights.spells.SpellConditionalTeleport;
import com.playmonumenta.bossfights.spells.SpellPurgeNegatives;
import com.playmonumenta.bossfights.spells.spells_kaul.SpellEarthenRupture;
import com.playmonumenta.bossfights.utils.Utils;

public class ImmortalElementalKaulBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kaulimmortal";
	public static final int detectionRange = 100;

	private LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ImmortalElementalKaulBoss(plugin, boss);
	}

	public ImmortalElementalKaulBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		Location spawnLoc = mBoss.getLocation();
		World world = mBoss.getWorld();
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hp_del = 512;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0) {
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseCharge(plugin, mBoss, 20, 20, true,
				(Player player) -> {
					boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
					boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4), true);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);
				},
				// Warning particles
				(Location loc) -> {
					loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 1, 1, 1, 0);
				},
				// Charge attack sound/particles at boss location
				(Player player) -> {
					boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 100, 2, 2, 2, 0);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.5f);
				},
				// Attack hit a player
				(Player player) -> {
					player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 80, 1, 1, 1, 0);
					player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation(), 20, 1, 1, 1, Material.COARSE_DIRT.createBlockData());
					boss.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.85f);
					player.damage(16, boss);
					Utils.KnockAway(mBoss.getLocation(), player, 0.4f, 0.4f);
				},
				// Attack particles
				(Location loc) -> {
					loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.02, 0.02, 0.02, 0);
				},
				// Ending particles on boss
				() -> {
					boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 200, 2, 2, 2, 0);

				}
			),
			new SpellEarthenRupture(plugin, mBoss),
			new SpellBaseBolt(plugin, mBoss, 20 * 2, 20 * 5, 1.1, detectionRange, 0.5, false, true,
				(Entity entity, int tick) -> {
					float t = tick / 15;
					world.spawnParticle(Particle.LAVA, mBoss.getLocation(), 1, 0.35, 0, 0.35, 0.005);
					world.spawnParticle(Particle.BLOCK_CRACK, mBoss.getLocation(), 3, 0, 0, 0, 0.5,
					Material.STONE.createBlockData());
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 10, t);
					mBoss.removePotionEffect(PotionEffectType.SLOW);
					mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
				},

				(Entity entity) -> {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 5, 0.5f);
					world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 80, 0.2, 0.45,
					0.2, 0.2);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 30, 0.2,
					0.45, 0.2, 0.1);
				},

				(Location loc) -> {
					world.spawnParticle(Particle.BLOCK_DUST, loc, 6, 0.45, 0.45, 0.45, 0.25,
					Material.STONE.createBlockData());
					world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 2, 0.2, 0.2, 0.2, 0.25);
					for (Block block : Utils.getNearbyBlocks(loc.getBlock(), 1)) {
						if (block.getType().isSolid()) {
							Material material = block.getType();
							if (material == Material.SMOOTH_SANDSTONE
							|| material == Material.SMOOTH_RED_SANDSTONE
							|| material == Material.NETHERRACK
							|| material == Material.MAGMA_BLOCK) {
								block.setType(Material.AIR);
							}
						}
					}
				},

				(Player player, Location loc, boolean blocked) -> {
					if (!blocked) {
						player.damage(22, mBoss);
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 1));
						player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 15, 0));
					} else {
						for (Player p : Utils.playersInRange(loc, 2.5)) {
							p.damage(16, mBoss);
							Utils.KnockAway(loc, p, 0.3f);
							p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 1));
							p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 0));
						}
					}
					world.spawnParticle(Particle.FLAME, loc, 125, 0, 0, 0, 0.175);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);
				}
			)
		));

		List<Spell> passiveSpells = Arrays.asList(new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
			world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
			0.4, 0.35, Material.BROWN_CONCRETE.createBlockData());
		}),
		new SpellBlockBreak(mBoss),
		new SpellPurgeNegatives(mBoss, 2),
		new SpellConditionalTeleport(mBoss, spawnLoc,
		                             b -> b.getLocation().getBlock().getType() == Material.BEDROCK
		                             || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
		                             || b.getLocation().getBlock().getType() == Material.LAVA
		                             || b.getLocation().getBlock().getType() == Material.WATER));


		super.constructBoss(plugin, identityTag, mBoss, activeSpells, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 30);
			}
		}
	}

	private Random rand = new Random();
	@Override
	public void bossCastAbility(SpellCastEvent event) {
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), detectionRange);
		if (players.size() > 0) {
			Player newTarget = players.get(rand.nextInt(players.size()));
			((Mob) mBoss).setTarget(newTarget);
		}
	}
}
