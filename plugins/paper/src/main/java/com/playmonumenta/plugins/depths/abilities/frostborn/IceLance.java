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
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class IceLance extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Lance";
	public static final double[] DAMAGE = {12.5, 15.0, 17.5, 20.0, 22.5, 27.5};
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(194, 224, 249), 1.0f);
	private static final int COOLDOWN = 6 * 20;
	private static final int DURATION = 2 * 20;
	private static final double AMPLIFIER = 0.2;
	private static final int RANGE = 8;
	private static final double HITBOX_SIZE = 0.75;
	public static final int ICE_TICKS = 6 * 20;

	public static final DepthsAbilityInfo<IceLance> INFO =
		new DepthsAbilityInfo<>(IceLance.class, ABILITY_NAME, IceLance::new, DepthsTree.FROSTBORN, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.ICE_LANCE)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IceLance::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.SNOWBALL))
			.descriptions(IceLance::getDescription);

	public IceLance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, HITBOX_SIZE, HITBOX_SIZE, HITBOX_SIZE);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 10, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);

		for (int i = 0; i < RANGE; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);

			new PartialParticle(Particle.EXPLOSION_NORMAL, bLoc, 2, 0.05, 0.05, 0.05, 0.025).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, bLoc, 18, 0.35, 0.35, 0.35, MANA_LANCE_COLOR).spawnAsPlayerActive(mPlayer);

			if (bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				new PartialParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
				world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.BLOCKS, 1, 1.65f);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
					EntityUtils.applySlow(mPlugin, DURATION, AMPLIFIER, mob);
					EntityUtils.applyWeaken(mPlugin, DURATION, AMPLIFIER, mob);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f, true);
					iter.remove();
					mobs.remove(mob);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						if (mob.isDead() || mob.getHealth() < 0) {
							Location deathSpot = mob.getLocation().add(0, -1, 0);
							DepthsUtils.iceExposedBlock(deathSpot.getBlock(), ICE_TICKS, mPlayer);
							DepthsUtils.iceExposedBlock(deathSpot.getBlock().getRelative(BlockFace.NORTH), ICE_TICKS, mPlayer);
							DepthsUtils.iceExposedBlock(deathSpot.getBlock().getRelative(BlockFace.SOUTH), ICE_TICKS, mPlayer);
							DepthsUtils.iceExposedBlock(deathSpot.getBlock().getRelative(BlockFace.WEST), ICE_TICKS, mPlayer);
							DepthsUtils.iceExposedBlock(deathSpot.getBlock().getRelative(BlockFace.EAST), ICE_TICKS, mPlayer);
						}
					}, 1);
				}
			}
		}

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.BLOCKS, 1, 2.0f);
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right click to shoot an ice lance that travels " + RANGE + " blocks and pierces through mobs, dealing ")
			.append(Component.text(StringUtils.to2DP(DAMAGE[rarity - 1]), color))
			.append(Component.text(" magic damage and applying " + StringUtils.multiplierToPercentage(AMPLIFIER) + "% slowness and weaken for " + DURATION / 20 + " seconds. If the lance kills a mob, the floor under it will be frozen for " + ICE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}

}

