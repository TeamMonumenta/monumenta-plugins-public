package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.VoodooBondsOtherPlayer;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;



public class VoodooBonds extends Ability {

	private static final int COOLDOWN_1 = 22 * 20;
	private static final int COOLDOWN_2 = 12 * 20;
	private static final int ACTIVE_RADIUS = 8;
	private static final int PASSIVE_RADIUS = 3;
	private static final double DAMAGE_1 = 0.2;
	private static final double DAMAGE_2 = 0.3;

	private final double mDamage;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public VoodooBonds(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Voodoo Bonds");
		mInfo.mLinkedSpell = ClassAbility.VOODOO_BONDS;
		mInfo.mScoreboardId = "VoodooBonds";
		mInfo.mShorthandName = "VB";
		mInfo.mDescriptions.add("Melee strikes to a mob apply 20% of the damage to all mobs of the same type within 3 blocks. Additionally, Right-click while sneaking and looking down to cast a protective spell on all players within an 8 block radius. The next hit every player (including the Reaper) takes has all damage ignored (or 50% if attack is from a Boss), but that damage will transfer to the Reaper in 5s unless it is passed on again. Passing that damage requires a melee strike, in which 33% of the initial damage blocked is added to the damage of the strike (Bosses are immune to this bonus). The damage directed to the Reaper is calculated by the percentage of health the initial hit would have taken from that player, and can never kill you, only leave you at 1 HP. Cooldown: 22s.");
		mInfo.mDescriptions.add("The passive damage on similar mobs is increased to 30%, the duration before damage transfer increases to 7s, the on-hit damage when passing a hit increases to 66% of the blocked damage, and the cooldown is reduced to 12s.");
		mInfo.mCooldown = isLevelOne() ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.JACK_O_LANTERN, 1);
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public boolean runCheck() {
		return ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public void cast(Action action) {
		if (!mPlayer.isSneaking() || mPlayer.getLocation().getPitch() < 50 || (isTimerActive() && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell))) {
			return;
		}

		World world = mPlayer.getWorld();
		//new sound
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.3f, 0.75f);
		putOnCooldown();
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 0.25;
				for (int i = 0; i < 36; i += 1) {
					mRotation += 10;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					//new particles
					world.spawnParticle(Particle.SPELL_WITCH, mLoc, 1, 0.15, 0.15, 0.15, 0);
					world.spawnParticle(Particle.REDSTONE, mLoc, 1, 0.15, 0.15, 0.15, 0, COLOR);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);

				}
				if (mRadius >= ACTIVE_RADIUS) {
					this.cancel();
				}

			}
		}.runTaskTimer(mPlugin, 0, 1);
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), ACTIVE_RADIUS, true)) {
			//better effects
			p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 0.75f);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01);
			mPlugin.mEffectManager.addEffect(p, "VoodooBondsEffect",
					new VoodooBondsOtherPlayer(mInfo.mCooldown, mPlayer, mPlugin));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && mPlayer != null) {
			EntityType type = enemy.getType();
			Location loc = enemy.getLocation();
			World world = loc.getWorld();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), PASSIVE_RADIUS, mPlayer)) {
				if (mob.getType().equals(type) && mob != enemy) {
					Location mLoc = mob.getLocation();
					DamageUtils.damage(mPlayer, mob, DamageType.OTHER, event.getDamage() * mDamage, mInfo.mLinkedSpell, true);
					world.spawnParticle(Particle.SPELL_WITCH, mLoc, 30, 0.5, 0.5, 0.5, 0.001);
					world.spawnParticle(Particle.REDSTONE, mLoc, 30, 0.5, 0.5, 0.5, 0, COLOR);
				}
			}
			return true;
		}
		return false;
	}
}
