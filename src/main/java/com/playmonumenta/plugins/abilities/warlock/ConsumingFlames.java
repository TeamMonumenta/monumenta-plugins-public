package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class ConsumingFlames extends Ability {

	private static final int CONSUMING_FLAMES_1_RADIUS = 5;
	private static final int CONSUMING_FLAMES_2_RADIUS = 7;
	private static final int CONSUMING_FLAMES_DAMAGE = 1;
	private static final int CONSUMING_FLAMES_DURATION = 7 * 20;
	private static final int CONSUMING_FLAMES_COOLDOWN = 10 * 20;

	public ConsumingFlames(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "ConsumingFlames";
		mInfo.linkedSpell = Spells.CONSUMING_FLAMES;
		mInfo.cooldown = CONSUMING_FLAMES_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		Player player = mPlayer;
		Location loc = player.getLocation();
		World world = player.getWorld();
		int consumingFlames = getAbilityScore();

		world.spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 60, 1.75, 0.75, 1.75, 0.0);
		world.playSound(loc, Sound.ENTITY_MAGMA_CUBE_SQUISH, 1.0f, 0.66f);

		boolean effect = false;
		int radius = (consumingFlames == 1) ? CONSUMING_FLAMES_1_RADIUS : CONSUMING_FLAMES_2_RADIUS;
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), radius, mPlayer)) {
			PotionUtils.applyPotion(player, mob, new PotionEffect(PotionEffectType.WEAKNESS, CONSUMING_FLAMES_DURATION, 0, false, true));
			mob.setFireTicks(CONSUMING_FLAMES_DURATION);

			EntityUtils.damageEntity(mPlugin, mob, CONSUMING_FLAMES_DAMAGE, player);
			effect = true;
		}

		if (consumingFlames > 1 && effect) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, CONSUMING_FLAMES_DURATION, 0, false, true));
		}

		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
