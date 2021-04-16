package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.Crusade;

public class HolyJavelin extends Ability {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.0f);
	private static final double HITBOX_LENGTH = 0.75;
	private static final int RANGE = 12;
	private static final int UNDEAD_DAMAGE_1 = 18;
	private static final int UNDEAD_DAMAGE_2 = 24;
	private static final int DAMAGE_1 = 9;
	private static final int DAMAGE_2 = 12;
	private static final int FIRE_DURATION = 5 * 20;
	private static final int COOLDOWN = 12 * 20;
	private static final int DIVINE_JUSTICE_1_BONUS = 4;
	private static final double DIVINE_JUSTICE_2_SCALING = 0.15;
	private static final int DIVINE_JUSTICE_2_BONUS = 8;
	private static final double LUMINOUS_2_BONUS = 0.2;

	private final int mDamage;
	private final int mUndeadDamage;

	private double mBonusCritDamage = 0;
	private double mBonusLumDamage = 0.0;
	private double mBonusCrusadeDamage = 0.0;

	private Crusade mCrusade;
	private boolean mCountsHumanoids = false;
	private DivineJustice mDivineJustice;
	private LuminousInfusion mLuminousInfusion;

	public HolyJavelin(Plugin plugin, Player player) {
		super(plugin, player, "Holy Javelin");
		mInfo.mLinkedSpell = Spells.HOLY_JAVELIN;
		mInfo.mScoreboardId = "HolyJavelin";
		mInfo.mShorthandName = "HJ";
		mInfo.mDescriptions.add("Sprint left-clicking while not holding a pickaxe throws a piercing spear of light 12 blocks forward, dealing 18 damage to undead and 9 damage to all others. All hit enemies are set on fire for 5s. Cooldown: 12s");
		mInfo.mDescriptions.add("Damage is increased to 24 to undead and 12 to all others. If the melee attack that triggers this strikes an undead, any passive damage done from Divine Justice and Luminous Infusion is transmitted to all other targets struck by the Javelin.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mUndeadDamage = getAbilityScore() == 1 ? UNDEAD_DAMAGE_1 : UNDEAD_DAMAGE_2;

		// Needs to wait for the entire AbilityCollection to be initialized
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mDivineJustice = AbilityManager.getManager().getPlayerAbility(mPlayer, DivineJustice.class);
				mLuminousInfusion = AbilityManager.getManager().getPlayerAbility(mPlayer, LuminousInfusion.class);
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
				if (mCrusade != null) {
					mCountsHumanoids = mCrusade.getAbilityScore() == 2;
				}
				if (mDivineJustice != null) {
					mBonusCritDamage = DIVINE_JUSTICE_1_BONUS;
					if (mCrusade != null) {
						mBonusCrusadeDamage += mBonusCritDamage * 0.33;
					}
				}
				if (mLuminousInfusion != null) {
					mBonusLumDamage = mLuminousInfusion.getAbilityScore() == 1 ? 0 : LUMINOUS_2_BONUS * mUndeadDamage;
				}
			}
		});
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSprinting() && !mPlayer.isSneaking() && !InventoryUtils.isPickaxeItem(mainHand);
	}

	public void execute(double bonusDamage) {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 0.9f);
		Location playerLoc = mPlayer.getEyeLocation();
		Location location = playerLoc.clone();
		Vector increment = location.getDirection();
		world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add(increment), 10, 0, 0, 0, 0.125f);

		// Get a list of all the mobs this could possibly hit (that are within range of the player)
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(location, RANGE, mPlayer);
		BoundingBox box = BoundingBox.of(playerLoc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		for (int i = 0; i < RANGE; i++) {
			box.shift(increment);
			Location loc = box.getCenter().toLocation(world);
			world.spawnParticle(Particle.REDSTONE, loc, 22, 0.25, 0.25, 0.25, COLOR);
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0f, 0f, 0f, 0.025f);

			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isUndead(mob) || (mCountsHumanoids && EntityUtils.isHumanoid(mob))) {
						EntityUtils.damageEntity(mPlugin, mob, mUndeadDamage + bonusDamage, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
					} else {
						EntityUtils.damageEntity(mPlugin, mob, mDamage + bonusDamage, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
					}
					EntityUtils.applyFire(mPlugin, FIRE_DURATION, mob, mPlayer);
					iter.remove();
				}
			}

			if (loc.getBlock().getType().isSolid()) {
				loc.subtract(increment.multiply(0.5));
				world.spawnParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
				break;
			}
		}
		putOnCooldown();
	}

	@Override
	public void cast(Action action) {
		execute(0);
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			LivingEntity damagee = (LivingEntity) event.getEntity();
			if (EntityUtils.isUndead(damagee) || (mCountsHumanoids && EntityUtils.isHumanoid(damagee))) {
				double divineCritScaling = 0;
				if (mDivineJustice != null) {
					divineCritScaling = mDivineJustice.getAbilityScore() > 1 ? DIVINE_JUSTICE_2_SCALING : 0;
				}
				execute(((double) (PlayerUtils.isCritical(mPlayer) ? mBonusCritDamage + mBonusCrusadeDamage : 0) + mBonusLumDamage) * (PlayerUtils.isCritical(mPlayer) ? 1 + divineCritScaling : 1) + (mUndeadDamage * (PlayerUtils.isCritical(mPlayer) ? divineCritScaling : 0)));
			} else {
				execute(0);
			}
		}
		return true;
	}

}
