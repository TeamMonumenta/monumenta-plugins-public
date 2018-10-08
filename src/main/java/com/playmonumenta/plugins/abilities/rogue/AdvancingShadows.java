package com.playmonumenta.plugins.abilities.rogue;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class AdvancingShadows extends Ability {

	private String owner = "none";
	private int id = new Random().nextInt(10);
	private LivingEntity target = null;

	private static final int ADVANCING_SHADOWS_RANGE_1 = 11;
	private static final int ADVANCING_SHADOWS_RANGE_2 = 16;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED = 0.5f;
	private static final float ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE = 4;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int ADVANCING_SHADOWS_STRENGTH_DURATION = 5 * 20;
	private static final int ADVANCING_SHADOWS_STRENGTH_EFFECT_LEVEL = 1;
	private static final int ADVANCING_SHADOWS_COOLDOWN = 20 * 20;

	@Override
	public boolean cast(Player player) {
		owner = player.getName();
		Bukkit.broadcastMessage("ability belongs to " + owner + " with abilityid " + id);
		
		LivingEntity entity = target;
		if (entity != null) {
			int advancingShadows = getAbilityScore(player);
			Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), player.getLocation());
			Location loc = player.getLocation();
			while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
				loc.add(dir);
				if (loc.distance(entity.getLocation()) < ADVANCING_SHADOWS_OFFSET) {
					double multiplier = ADVANCING_SHADOWS_OFFSET - loc.distance(entity.getLocation());
					loc.subtract(dir.clone().multiply(multiplier));
					break;
				}
			}
			loc.add(0, 1, 0);

			// Just in case the player's teleportation loc is in a block.
			while (loc.getBlock().getType().isSolid()) {
				loc.subtract(dir.clone().multiply(1.15));
			}
			mWorld.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.1, 0), 50, 0, 0.5, 0, 1.0);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1.1, 0), 12, 0, 0.5, 0, 0.05);
			mWorld.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

			player.teleport(loc);

			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
					new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ADVANCING_SHADOWS_STRENGTH_DURATION,
							ADVANCING_SHADOWS_STRENGTH_EFFECT_LEVEL, true, false));

			if (advancingShadows > 1) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(),
						ADVANCING_SHADOWS_AOE_KNOCKBACKS_RANGE)) {
					MovementUtils.KnockAway(entity, mob, ADVANCING_SHADOWS_AOE_KNOCKBACKS_SPEED);
				}
			}

			mWorld.spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1.1, 0), 50, 0, 0.5, 0, 1.0);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1.1, 0), 12, 0, 0.5, 0, 0.05);
			mWorld.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
			target = null;
			putOnCooldown(player);
			return true;
		}
		return false;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 4;
		info.specId = -1;
		info.linkedSpell = Spells.ADVANCING_SHADOWS;
		info.scoreboardId = "AdvancingShadows";
		info.cooldown = ADVANCING_SHADOWS_COOLDOWN;
		info.trigger = AbilityTrigger.RIGHT_CLICK;
		return info;
	}

	@Override
	public boolean runCheck(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
			if (!player.isSneaking()) {
				int advancingShadows = getAbilityScore(player);
				int range = (advancingShadows == 1) ? ADVANCING_SHADOWS_RANGE_1 : ADVANCING_SHADOWS_RANGE_2;

				// Basically makes sure if the target is in LoS and if there is
				// a path.
				Location eyeLoc = player.getEyeLocation();
				Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), range);
				ray.throughBlocks = false;
				ray.throughNonOccluding = false;
				RaycastData data = ray.shootRaycast();

				List<LivingEntity> rayEntities = data.getEntities();
				if (rayEntities != null && !rayEntities.isEmpty()) {
					target = rayEntities.get(0);
					if (target != null && !target.isDead() && EntityUtils.isHostileMob(target))
						return true;
				}
			}
		}
		return false;
	}

}
