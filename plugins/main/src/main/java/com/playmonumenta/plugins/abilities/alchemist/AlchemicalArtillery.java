package com.playmonumenta.plugins.abilities.alchemist;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * Alchemical Artillery: Left click with a bow to prime it with an alchemist potion.
 * Shooting the bow in the next 5 seconds consumes 2 / 1 potions.
 * When the arrow hits an enemy, the potion is applied in a 3 / 5 block radius.
 */

public class AlchemicalArtillery extends Ability {
	private static final String ALCHEMICAL_ARTILLERY_METAKEY = "AlchemicalArtilleryArrowGotTheDankPot";
	private static final int ALCHEMICAL_ARTILLERY_1_RADIUS = 3;
	private static final int ALCHEMICAL_ARTILLERY_2_RADIUS = 5;
	private static final int ALCHEMICAL_ARTILLERY_1_COST = 2;
	private static final int ALCHEMICAL_ARTILLERY_2_COST = 1;

	private int mRadius;
	private int mCost;

	public AlchemicalArtillery(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.ALCHEMICAL_ARTILLERY;
		mInfo.scoreboardId = "Artillery";
		mInfo.cooldown = 0;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mRadius = getAbilityScore() == 1 ? ALCHEMICAL_ARTILLERY_1_RADIUS : ALCHEMICAL_ARTILLERY_2_RADIUS;
		mCost = getAbilityScore() == 1 ? ALCHEMICAL_ARTILLERY_1_COST : ALCHEMICAL_ARTILLERY_2_COST;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.hasMetadata(ALCHEMICAL_ARTILLERY_METAKEY)) {
			Location loc = damagee.getLocation().add(0, 0.5, 0);
			mWorld.spawnParticle(Particle.SPELL_MOB, loc, 10 * (int) Math.pow(mRadius, 2), mRadius, 0, mRadius, 0);
			mWorld.spawnParticle(Particle.FLAME, loc, 3 * (int) Math.pow(mRadius, 2), 0, 0, 0, 0.3);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 5 * (int) Math.pow(mRadius, 2), 0, 0, 0, 0.5);
			mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1);
			mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1);

			BrutalAlchemy ba = (BrutalAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class);
			GruesomeAlchemy ga = (GruesomeAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, GruesomeAlchemy.class);
			NightmarishAlchemy na = (NightmarishAlchemy) AbilityManager.getManager().getPlayerAbility(mPlayer, NightmarishAlchemy.class);
			InvigoratingOdor io = (InvigoratingOdor) AbilityManager.getManager().getPlayerAbility(mPlayer, InvigoratingOdor.class);
			int baScore = ScoreboardUtils.getScoreboardValue(mPlayer, "BrutalAlchemy");
			int gaScore = ScoreboardUtils.getScoreboardValue(mPlayer, "GruesomeAlchemy");
			int naScore = ScoreboardUtils.getScoreboardValue(mPlayer, "Nightmarish");
			int ioScore = ScoreboardUtils.getScoreboardValue(mPlayer, "InvigoratingOdor");

			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(arrow.getLocation(), mRadius);
			int size = mobs.size();
			boolean guaranteedApplicationApplied = false;

			for (LivingEntity mob : mobs) {
				if (ba != null) {
					ba.apply(mPlugin, mPlayer, mob, baScore);
				}
				if (ga != null) {
					ga.apply(mPlugin, mPlayer, mob, gaScore);
				}
				if (na != null) {
					guaranteedApplicationApplied = na.apply(mRandom, mPlugin, mPlayer, mob, naScore, size, guaranteedApplicationApplied);
				}
				if (io != null) {
					io.apply(mPlugin, mPlayer, mob, ioScore);
				}
			}
			if (io != null) {
				for (Player player : PlayerUtils.getNearbyPlayers(arrow.getLocation(), mRadius)) {
					io.apply(mPlugin, mPlayer, player, ioScore);
				}
			}
		}

		return true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (mPlayer.isSneaking()) {
			ItemStack potionStack = null;
			int potionCount = 0;
			for (ItemStack item : mPlayer.getInventory().getContents()) {
				if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
					potionStack = item;
					potionCount = item.getAmount();
					break;
				}
			}

			if (potionCount >= mCost) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FIREWORKS_SPARK);
				arrow.setMetadata(ALCHEMICAL_ARTILLERY_METAKEY, new FixedMetadataValue(mPlugin, mRadius));
				potionStack.setAmount(potionCount - mCost);
			}
		}

		return true;
	}

}
