package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit.LIMITSENUM;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.PLAYERFILTER;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;

//generalized class for all bosses with laser
public class LaserBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_laser";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;

		@BossParam(help = "not written")
		public int DETECTION = 30;

		@BossParam(help = "not written")
		public int DELAY = 5 * 20;

		@BossParam(help = "not written")
		public int DURATION = 5 * 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 12 * 20;

		@BossParam(help = "When false, laser attack can go through blocks.")
		public boolean CAN_BLOCK = true;

		@BossParam(help = "whether or not the laser will break blocks around where it hits")
		public boolean BREAK_BLOCKS = false;

		@BossParam(help = "if breakblocks = true, this is the radius of the blocks that will be deleted")
		public int BLOCK_BREAK_RADIUS = 3;

		@BossParam(help = "not written")
		public boolean CAN_MOVE = false;

		@BossParam(help = "not written", deprecated = true)
		public boolean SINGLE_TARGET = false;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Effects apply to player after the laser end")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "LOS name of the mob at the location of the laser's end")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.EMPTY;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		//particle & sound used!
		@BossParam(help = "Sound used each tick on each player")
		public SoundsList SOUND_TICKS = SoundsList.fromString("[(ENTITY_SHULKER_BULLET_HIT)]");

		@BossParam(help = "Particle used for the laser")
		public ParticlesList PARTICLE_LASER = ParticlesList.fromString("[(CRIT,1),(CRIT_MAGIC,1)]");

		@BossParam(help = "Particle used when the cast is over")
		public ParticlesList PARTICLE_END = ParticlesList.fromString("[(EXPLOSION_NORMAL,35)]");

		@BossParam(help = "Sound used when the cast is over")
		public SoundsList SOUND_END = SoundsList.fromString("[(ENTITY_DRAGON_FIREBALL_EXPLODE,0.6,1.5)]");

		@BossParam(help = "not written")
		public int PARTICLE_FREQUENCY = 1;
		@BossParam(help = "not written")
		public int PARTICLE_CHANCE = 6;
		@BossParam(help = "If the player exceeds this range, the laser gets cancelled. If 0, laser has no max range.")
		public int MAX_RANGE = 0;
	}

	public LaserBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, p.SINGLE_TARGET ? new Limit(1) : new Limit(LIMITSENUM.ALL), List.of(PLAYERFILTER.HAS_LINEOFSIGHT));
			//by default LaserBoss don't take player in stealth and need LINEOFSIGHT to cast.
		}
		Spell spell = new SpellBaseLaser(plugin, boss, p.DURATION, false, p.COOLDOWN,
			() -> {
				return p.TARGETS.getTargetsList(mBoss);
			},
			// Tick action per player
			(LivingEntity target, int ticks, boolean blocked) -> {

				p.SOUND_TICKS.play(target.getLocation(), 0.8f, 0.5f + (ticks / 80f) * 1.5f);
				p.SOUND_TICKS.play(boss.getLocation(), 0.8f, 0.5f + (ticks / 80f) * 1.5f);

				if (ticks == 0 && !p.CAN_MOVE) {
					Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
						new BaseMovementSpeedModifyEffect(p.DURATION, -0.75));
				}
			},
			// Particles generated by the laser
			(Location loc) -> {
				p.PARTICLE_LASER.spawn(boss, loc, 0.03, 0.03, 0.03, 0.5d);
			},
			p.PARTICLE_FREQUENCY,
			p.PARTICLE_CHANCE,
			// Damage generated at the end of the attack
			(LivingEntity target, Location loc, boolean blocked) -> {
				p.SOUND_END.play(loc, 0.6f, 1.5f);
				p.PARTICLE_END.spawn(boss, loc, 0, 0, 0, 0.25);
				if (!ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.RESIST_5) || ZoneUtils.hasZoneProperty(mBoss.getLocation(), ZoneUtils.ZoneProperty.BLITZ)) {
					Entity spawn = p.SPAWNED_MOB_POOL.spawn(loc);
					if (spawn != null) {
						summonPlugins(spawn);
					}
				}


				if (p.CAN_BLOCK) {
					if (blocked) {
						breakBlocks(loc, p.BLOCK_BREAK_RADIUS);
						return;
					}
				}

				if (target != null) {
					if (p.DAMAGE > 0) {
						BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
					}

					if (p.DAMAGE_PERCENTAGE > 0.0) {
						BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, mBoss.getLocation(), p.SPELL_NAME);
					}

					p.EFFECTS.apply(target, mBoss);
				}


			},
			p.MAX_RANGE);

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}

	private void breakBlocks(Location l, int radius) {
		final EnumSet<Material> mIgnoredMats = EnumSet.of(
			Material.AIR,
			Material.CAVE_AIR,
			Material.VOID_AIR,
			Material.COMMAND_BLOCK,
			Material.CHAIN_COMMAND_BLOCK,
			Material.REPEATING_COMMAND_BLOCK,
			Material.BEDROCK,
			Material.BARRIER,
			Material.SPAWNER,
			Material.WATER,
			Material.LAVA,
			Material.END_PORTAL
		);

		List<Block> badBlockList = new ArrayList<>();
		Location testloc = l.clone();
		for (int x = -radius; x <= radius; x++) {
			testloc.setX(l.getX() + x);
			for (int z = -radius; z <= radius; z++) {
				testloc.setZ(l.getZ() + z);
				for (int y = -radius; y <= radius; y++) {
					testloc.setY(l.getY() + y + 0.2);

					Block block = testloc.getBlock();
					if (!mIgnoredMats.contains(block.getType())) {
						badBlockList.add(block);
					}
				}
			}
		}

		/* If there are any blocks, destroy all blocking blocks */
		if (badBlockList.size() > 0) {

			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mBoss, l, badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				if (block.getState() instanceof Container) {
					block.breakNaturally();
				} else {
					block.setType(Material.AIR);
				}
			}
		}
	}

	public void summonPlugins(Entity summon) {

	}
}

