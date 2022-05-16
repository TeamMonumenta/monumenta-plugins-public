package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class ManaLance extends MultipleChargeAbility {

	public static final String CHARM_DAMAGE = "Mana Lance Damage";
	public static final String CHARM_COOLDOWN = "Mana Lance Cooldown";
	public static final String CHARM_RANGE = "Mana Lance Range";
	public static final String CHARM_CHARGES = "Mana Lance Charge";
	private static final float DAMAGE_1 = 6.0f;
	private static final float DAMAGE_2 = 7.0f;
	private static final int COOLDOWN_1 = 5 * 20;
	private static final int COOLDOWN_2 = 3 * 20;
	private static final int RANGE = 8;
	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	private float mDamage;
	private int mLastCastTicks = 0;

	public ManaLance(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Mana Lance");
		mInfo.mLinkedSpell = ClassAbility.MANA_LANCE;
		mInfo.mScoreboardId = "ManaLance";
		mInfo.mShorthandName = "ML";
		mInfo.mDescriptions.add("Right clicking with a wand fires forth a piercing beam of Mana going 8 blocks, dealing 6 magic damage to enemies in the path of the beam. This beam will not go through solid blocks. Cooldown: 5s.");
		mInfo.mDescriptions.add("The beam instead deals 7 damage. Cooldown: 3s.");
		mInfo.mDescriptions.add("Mana lance now has two charges.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, isLevelOne() ? COOLDOWN_1 : COOLDOWN_2);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.TRIDENT, 1);
		mMaxCharges = (isEnhanced() ? 2 : 1) + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mInfo.mIgnoreCooldown = true;
		mCharges = getTrackedCharges();
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}

		float damage = mDamage;
		damage = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		damage = SpellPower.getSpellDamage(mPlugin, mPlayer, damage);
		int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();

		Location endLoc = loc;
		int range = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RANGE);
		for (int i = 0; i < range; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			endLoc = bLoc;

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				new PartialParticle(Particle.CLOUD, bLoc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
				world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f, true);
					iter.remove();
				}
			}
		}

		new PPLine(Particle.EXPLOSION_NORMAL, loc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.025).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, loc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(MANA_LANCE_COLOR).spawnAsPlayerActive(mPlayer);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
	}

	@Override
	public boolean runCheck() {
		if (mPlayer == null) {
			return false;
		}
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return !mPlayer.isSneaking() && ItemUtils.isWand(mainHand);
	}

}
