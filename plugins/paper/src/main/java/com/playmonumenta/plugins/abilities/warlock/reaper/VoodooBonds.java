package com.playmonumenta.plugins.abilities.warlock.reaper;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.VoodooBondsOtherPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



public class VoodooBonds extends Ability {

	private static final int COOLDOWN = 25 * 20;
	private static final int ACTIVE_RADIUS = 8;
	private static final int PASSIVE_RADIUS = 3;
	private static final double DAMAGE_1 = 0.2;
	private static final double DAMAGE_2 = 0.3;

	private final double mDamage;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public VoodooBonds(Plugin plugin, Player player) {
		super(plugin, player, "Voodoo Bonds");
		mInfo.mLinkedSpell = ClassAbility.VOODOO_BONDS;
		mInfo.mScoreboardId = "VoodooBonds";
		mInfo.mShorthandName = "VB";
		mInfo.mDescriptions.add("Melee strikes to a mob apply 20% of the damage to all mobs of the same type within 3 blocks. Additionally, Right-click while looking down to cast a protective spell on all players within an 8 block radius. The next hit every player (including the Reaper) takes has all damage ignored (or 50% if attack is from a Boss), but that damage will transfer to the Reaper in 5s unless it is passed on again. Passing that damage requires a melee strike, in which 33% of the initial damage blocked is added to the damage of the strike (Bosses are immune to this bonus). The damage directed to the Reaper is calculated by the percentage of health the initial hit would have taken from that player, and can never kill you, only leave you at 1 HP. Cooldown: 15s.");
		mInfo.mDescriptions.add("The passive damage on similar mobs is increased to 30%, the duration before damage transfer increases to 7s, and the on-hit damage when passing a hit increases to 66% of the blocked damage.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public boolean runCheck() {
		return ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}

	@Override
	public void cast(Action action) {
		if (!mPlayer.isSneaking() || mPlayer.getLocation().getPitch() < 50) {
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
					new VoodooBondsOtherPlayer(COOLDOWN, mPlayer, mPlugin));
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity damagee = (LivingEntity) event.getEntity();
			EntityType type = damagee.getType();
			Location loc = damagee.getLocation();
			World world = loc.getWorld();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), PASSIVE_RADIUS, mPlayer)) {
				if (mob.getType().equals(type) && mob != damagee) {
					Location mLoc = mob.getLocation();
					Vector velocity = mob.getVelocity();
					EntityUtils.damageEntity(mPlugin, mob, event.getDamage() * mDamage, mPlayer, MagicType.DARK_MAGIC, true, mInfo.mLinkedSpell);
					mob.setVelocity(velocity);
					world.spawnParticle(Particle.SPELL_WITCH, mLoc, 30, 0.5, 0.5, 0.5, 0.001);
					world.spawnParticle(Particle.REDSTONE, mLoc, 30, 0.5, 0.5, 0.5, 0, COLOR);
				}
			}
		}
		return true;
	}
}