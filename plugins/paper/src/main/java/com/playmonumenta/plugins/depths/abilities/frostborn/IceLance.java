package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class IceLance extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Lance";
	public static final double[] DAMAGE = {12.5, 15.0, 17.5, 20.0, 22.5, 27.5};
	private static final Particle.DustOptions ICE_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(194, 224, 249), 1.0f);
	private static final int COOLDOWN = 6 * 20;
	private static final int DURATION = 2 * 20;
	private static final double AMPLIFIER = 0.2;
	private static final int RANGE = 8;
	public static final int ICE_TICKS = 6 * 20;

	public static final DepthsAbilityInfo<IceLance> INFO =
		new DepthsAbilityInfo<>(IceLance.class, ABILITY_NAME, IceLance::new, DepthsTree.FROSTBORN, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.ICE_LANCE)
			.cooldown(COOLDOWN)
			.actionBarColor(TextColor.color(194, 224, 249))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IceLance::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false), HOLDING_WEAPON_RESTRICTION))
			.displayItem(Material.SNOWBALL)
			.descriptions(IceLance::getDescription);

	public IceLance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location startLoc = mPlayer.getEyeLocation();
		World world = startLoc.getWorld();

		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, RANGE, loc -> {
			new PartialParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 1, 1.65f);
		});

		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
			EntityUtils.applySlow(mPlugin, DURATION, AMPLIFIER, mob);
			EntityUtils.applyWeaken(mPlugin, DURATION, AMPLIFIER, mob);
			MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f, true);
			if (mob.isDead() || mob.getHealth() <= 0) {
				Block deathSpot = mob.getLocation().add(0, -1, 0).getBlock();
				DepthsUtils.iceExposedBlock(deathSpot, ICE_TICKS, mPlayer);
				DepthsUtils.iceExposedBlock(deathSpot.getRelative(BlockFace.NORTH), ICE_TICKS, mPlayer);
				DepthsUtils.iceExposedBlock(deathSpot.getRelative(BlockFace.SOUTH), ICE_TICKS, mPlayer);
				DepthsUtils.iceExposedBlock(deathSpot.getRelative(BlockFace.WEST), ICE_TICKS, mPlayer);
				DepthsUtils.iceExposedBlock(deathSpot.getRelative(BlockFace.EAST), ICE_TICKS, mPlayer);
			}
		}

		new PartialParticle(Particle.EXPLOSION_NORMAL, startLoc, 10, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);

		new PPLine(Particle.EXPLOSION_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.025).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(ICE_LANCE_COLOR).spawnAsPlayerActive(mPlayer);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.BLOCKS, 1, 2.0f);
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right click to shoot an ice lance that travels " + RANGE + " blocks and pierces through mobs, dealing ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]), color))
			.append(Component.text(" magic damage and applying " + StringUtils.multiplierToPercentage(AMPLIFIER) + "% slowness and weaken for " + DURATION / 20 + " seconds. If the lance kills a mob, the floor under it will be frozen for " + ICE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}

}

