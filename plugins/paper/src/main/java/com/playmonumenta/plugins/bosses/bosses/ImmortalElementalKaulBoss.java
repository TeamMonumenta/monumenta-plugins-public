package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthenRupture;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPrimordialBolt;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ImmortalElementalKaulBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kaulimmortal";
	public static final int detectionRange = 100;

	public ImmortalElementalKaulBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Location spawnLoc = mBoss.getLocation();
		World world = mBoss.getWorld();

		// NOTE: Some of the Immortal's stats are inherent to the mob and aren't set here. Check the LoS entry when
		// making changes to the mob
		mBoss.setRemoveWhenFarAway(false);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0) + 0.01);

		GlowingManager.startGlowing(mBoss, NamedTextColor.GOLD, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseCharge(plugin, mBoss, 40, 65, 60, false,
				(LivingEntity target) -> {
					new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 30, 1, 1.25f, 1, 0).spawnAsEntityActive(boss);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
						new BaseMovementSpeedModifyEffect(65, -0.9));
					world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3.0f, 1.5f);
				},
				// Warning particles
				(Location loc) -> new PartialParticle(Particle.SMOKE_NORMAL, loc, 5, 0.7f, 0.9f, 0.7f, 0).spawnAsEntityActive(boss),
				// Charge attack sound/particles at boss location
				(LivingEntity player) -> {
					new PartialParticle(Particle.SMOKE_LARGE, boss.getLocation(), 75, 1, 1.5f, 1, 0).spawnAsEntityActive(boss);
					world.playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3.0f, 0.5f);
				},
				// Attack hit a player
				(LivingEntity target) -> {
					new PartialParticle(Particle.REDSTONE, target.getLocation(), 50, 1, 1.5f, 1, 0)
						.data(new Particle.DustOptions(Color.RED, 2)).spawnAsEntityActive(boss);
					new PartialParticle(Particle.BLOCK_DUST, target.getLocation(), 20, 1, 1, 1, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(boss);
					world.playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2.0f, 0.85f);
					BossUtils.blockableDamage(mBoss, target, DamageType.MELEE, 25);
					MovementUtils.knockAway(mBoss.getLocation(), target, 0.4f, 0.4f);
				},
				// Attack particles
				(Location loc) -> new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).minimumCount(1).spawnAsEntityActive(boss),
				// Ending particles and sound on boss
				() -> {
					new PartialParticle(Particle.BLOCK_DUST, boss.getLocation(), 50, 1, 1.5f, 1, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(boss);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_MUD_PLACE, SoundCategory.HOSTILE, 3.0f, 0.7f);
				}
			),
			new SpellEarthenRupture(plugin, mBoss),
			new SpellPrimordialBolt(plugin, mBoss)
		));

		List<Spell> passiveSpells = Arrays.asList(new SpellBaseParticleAura(boss, 1, (LivingEntity mBoss) ->
				new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
				0.4, 0.35, Material.BROWN_CONCRETE.createBlockData()).spawnAsEntityActive(boss)),
			new SpellBlockBreak(mBoss, 1, 3, 1, 8, false, true, false),
			new SpellShieldStun(30 * 20),
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				|| b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				|| b.getLocation().getBlock().getType() == Material.LAVA
				|| b.getLocation().getBlock().getType() == Material.WATER)
		);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() > 0 && mBoss instanceof Mob mob) {
			Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
			mob.setTarget(newTarget);
		}
	}
}
