package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CrushingEarth extends DepthsAbility {

	public static final String ABILITY_NAME = "Crushing Earth";
	private static final int COOLDOWN = 20 * 8;
	private static final int[] DAMAGE = {8, 10, 12, 14, 16, 24};
	private static final int CAST_RANGE = 4;
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40, 50};

	public static final DepthsAbilityInfo<CrushingEarth> INFO =
		new DepthsAbilityInfo<>(CrushingEarth.class, ABILITY_NAME, CrushingEarth::new, DepthsTree.EARTHBOUND, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.CRUSHING_EARTH)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CrushingEarth::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.SHIELD))
			.descriptions(CrushingEarth::getDescription);

	public CrushingEarth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		Location eyeLoc = mPlayer.getEyeLocation();
		Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), CAST_RANGE);
		ray.mThroughBlocks = false;
		ray.mThroughNonOccluding = false;

		RaycastData data = ray.shootRaycast();

		List<LivingEntity> mobs = data.getEntities();
		if (mobs != null && !mobs.isEmpty()) {
			World world = mPlayer.getWorld();
			for (LivingEntity mob : mobs) {
				if (mob.isValid() && !mob.isDead() && EntityUtils.isHostileMob(mob)) {
					Location mobLoc = mob.getEyeLocation();
					new PartialParticle(Particle.CRIT, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, mobLoc, 50, 0, 0.25, 0, 0.25).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPIT, mobLoc, 5, 0.15, 0.5, 0.15, 0).spawnAsPlayerActive(mPlayer);
					world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5f, 0.5f);
					world.playSound(eyeLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 1.0f);

					EntityUtils.applyStun(mPlugin, STUN_DURATION[mRarity - 1], mob);
					DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, DAMAGE[mRarity - 1], mInfo.getLinkedSpell());

					putOnCooldown();
					break;
				}
			}
		}
	}


	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Right click while looking at an enemy within " + CAST_RANGE + " blocks to deal ")
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text(" melee damage and stun them for "))
			.append(Component.text(StringUtils.to2DP(STUN_DURATION[rarity - 1] / 20.0), color))
			.append(Component.text(" seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}
