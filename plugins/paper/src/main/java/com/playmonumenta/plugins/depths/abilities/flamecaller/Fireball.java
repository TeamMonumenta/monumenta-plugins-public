package com.playmonumenta.plugins.depths.abilities.flamecaller;

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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Fireball extends DepthsAbility {

	public static final String ABILITY_NAME = "Fireball";
	private static final int COOLDOWN = 6 * 20;
	private static final int DISTANCE = 10;
	private static final double[] DAMAGE = {8, 10, 12, 14, 16, 20};
	private static final int RADIUS = 3;
	private static final int FIRE_TICKS = 3 * 20;

	public static final DepthsAbilityInfo<Fireball> INFO =
		new DepthsAbilityInfo<>(Fireball.class, ABILITY_NAME, Fireball::new, DepthsTree.FLAMECALLER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.FIREBALL)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Fireball::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.FIREWORK_STAR))
			.descriptions(Fireball::getDescription, MAX_RARITY);

	public Fireball(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		new PartialParticle(Particle.FLAME, mPlayer.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
		Vector dir = loc.getDirection().normalize();
		for (int i = 0; i < DISTANCE; i++) {
			loc.add(dir);

			if (loc.getBlock().getType().isSolid() || EntityUtils.getNearbyMobs(loc, 1).size() > 0) {
				explode(loc);

				return;
			}
		}

		explode(loc);
	}

	private void explode(Location loc) {
		World world = loc.getWorld();

		new PartialParticle(Particle.EXPLOSION_HUGE, loc, 1, 0, 0, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 1.5, 1.5, 1.5, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, 25, 1.5, 1.5, 1.5, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 1);

		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
			EntityUtils.applyFire(mPlugin, FIRE_TICKS, e, mPlayer);
			DamageUtils.damage(mPlayer, e, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());
		}
	}

	private static String getDescription(int rarity) {
		return "Right click to summon a " + RADIUS + " block radius fireball at the location you are looking, up to " + DISTANCE + " blocks away. The fireball deals " + DepthsUtils.getRarityColor(rarity) + (float) DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage and sets enemies ablaze for " + FIRE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}
}
