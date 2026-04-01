package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Frozen;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class IcicleBurst extends Ability {
	private static final String SCOREBOARD = "IcicleBurst";
	private static final int POINT_COST = 5;
	private static final double DIRECT_ATTACK_CHANCE = 0.45;
	private static final double INDIRECT_CHANCE = 0.25;
	private static final double DAMAGE = 10;
	private static final double RADIUS = 3;
	private static final int FREEZE_DURATION = 50;
	private static final double VULN_AMOUNT = 0.3;
	private static final int VULN_DURATION = 5 * 20;
	public static final Color TIP_COLOR = Color.fromRGB(184, 216, 242);
	public static final Color BASE_COLOR = Color.fromRGB(95, 159, 212);

	public static final AbilityInfo<IcicleBurst> INFO =
		new SnowPerkGui.SnowPerkInfo<>(IcicleBurst.class, "Icicle Burst", IcicleBurst::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.linkedSpell(ClassAbility.ICICLE_BURST)
			.displayItem(Material.BLUE_ICE)
			.description(getDescription());

	public IcicleBurst(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity entity = event.getEntity();
		if (EntityUtils.isHostileMob(entity) && !ScoreboardUtils.checkTag(entity, AbilityUtils.IGNORE_TAG)) {
			EntityDamageEvent damageEvent = entity.getLastDamageCause();
			double chance;
			if (damageEvent != null && damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
				chance = DIRECT_ATTACK_CHANCE;
			} else {
				chance = INDIRECT_CHANCE;
			}
			if (FastUtils.RANDOM.nextDouble() < chance) {
				Location loc = LocationUtils.getEntityCenter(entity);
				World world = loc.getWorld();

				List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, RADIUS).getHitMobs();
				mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
				for (LivingEntity mob : mobs) {
					DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.OTHER, DAMAGE, mInfo.getLinkedSpell(), true);
					EntityUtils.applyFreeze(mPlugin, FREEZE_DURATION, mob);
					EntityUtils.applyVulnerability(mPlugin, VULN_DURATION, VULN_AMOUNT, mob);
				}

				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 1.15f);
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.85f);
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 1.3f);
				for (int i = 0; i < 8; i++) {
					Vector dir = VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2));
					dir.setY(Math.abs(dir.getY()));
					ParticleUtils.drawParticleLineSlash(loc.clone().add(dir), dir, 0, 2.25, 0.1, 3, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
						new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0, 0, 0, 0, new Particle.DustOptions(
							ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, endProgress), 1.5f - (float) (endProgress * 1.3)))
							.spawnAsPlayerActive(mPlayer));
				}
			}
		}
	}

	public static Description<IcicleBurst> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Killing a mob has a chance to create an")
			.addLine("icy explosion that deals %d damage and").statValues(stat(DAMAGE))
			.addLine("and *Freezes* mobs within %d blocks for %t.").styles(Frozen.FROZEN_COLOR).statValues(stat(RADIUS), stat(FREEZE_DURATION))
			.addLine()
			.addLine("Mobs hit by this ability are afflicted with")
			.addLine("%p *Vulnerability* for %t.").styles(DescriptionUtils.WHITE).statValues(stat(VULN_AMOUNT), stat(VULN_DURATION))
			.addLine()
			.addLine("Explosions are more likely to happen with")
			.addLine("direct attack kills.")
			.addLine()
			.addStat("Explosion Chance: %p *if killed by* (m)").styles(DescriptionUtils.GREY).statValues(stat(DIRECT_ATTACK_CHANCE))
			.addStat("Explosion Chance: %p *if killed by non-*(m)").styles(DescriptionUtils.GREY).statValues(stat(INDIRECT_CHANCE))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
