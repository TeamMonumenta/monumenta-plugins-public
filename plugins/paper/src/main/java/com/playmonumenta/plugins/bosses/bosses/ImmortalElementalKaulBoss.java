package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthenRupture;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ImmortalElementalKaulBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kaulimmortal";
	public static final int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ImmortalElementalKaulBoss(plugin, boss);
	}

	public ImmortalElementalKaulBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);

		Location spawnLoc = mBoss.getLocation();
		World world = mBoss.getWorld();
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 512;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseCharge(plugin, mBoss, 20, 20, 160, true,
				(LivingEntity target) -> {
					boss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0);
					boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);
				},
				// Warning particles
				(Location loc) -> {
					loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 1, 1, 1, 0);
				},
				// Charge attack sound/particles at boss location
				(LivingEntity player) -> {
					boss.getWorld().spawnParticle(Particle.SMOKE_LARGE, boss.getLocation(), 100, 2, 2, 2, 0);
					boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.5f);
				},
				// Attack hit a player
				(LivingEntity target) -> {
					target.getWorld().spawnParticle(Particle.SMOKE_NORMAL, target.getLocation(), 80, 1, 1, 1, 0);
					target.getWorld().spawnParticle(Particle.BLOCK_DUST, target.getLocation(), 20, 1, 1, 1, Material.COARSE_DIRT.createBlockData());
					boss.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.85f);
					BossUtils.bossDamage(mBoss, target, 25);
					MovementUtils.knockAway(mBoss.getLocation(), target, 0.4f, 0.4f);
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
			PrimordialElementalKaulBoss.getBoltSpell(plugin, mBoss)
		));

		List<Spell> passiveSpells = Arrays.asList(new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) -> {
			world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
			0.4, 0.35, Material.BROWN_CONCRETE.createBlockData());
		}),
		new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true),
		new SpellPurgeNegatives(mBoss, 2),
		new SpellConditionalTeleport(mBoss, spawnLoc,
		                             b -> b.getLocation().getBlock().getType() == Material.BEDROCK
		                             || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
		                             || b.getLocation().getBlock().getType() == Material.LAVA
		                             || b.getLocation().getBlock().getType() == Material.WATER));


		super.constructBoss(activeSpells, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 30);
			}
		}
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() > 0) {
			Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
			((Mob) mBoss).setTarget(newTarget);
		}
	}
}
