package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ManaLanceCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class ManaLance extends MultipleChargeAbility {

	private static final float DAMAGE_1 = 6.0f;
	private static final float DAMAGE_2 = 7.0f;
	private static final int COOLDOWN_1 = 5 * 20;
	private static final int COOLDOWN_2 = 3 * 20;
	private static final int RANGE = 8;

	public static final String CHARM_DAMAGE = "Mana Lance Damage";
	public static final String CHARM_COOLDOWN = "Mana Lance Cooldown";
	public static final String CHARM_RANGE = "Mana Lance Range";
	public static final String CHARM_CHARGES = "Mana Lance Charge";

	private float mDamage;
	private int mLastCastTicks = 0;

	private final ManaLanceCS mCosmetic;

	public ManaLance(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Mana Lance");
		mInfo.mLinkedSpell = ClassAbility.MANA_LANCE;
		mInfo.mScoreboardId = "ManaLance";
		mInfo.mShorthandName = "ML";
		mInfo.mDescriptions.add(
			String.format("Right clicking with a wand fires forth a piercing beam of Mana going %s blocks, dealing %s arcane magic damage to enemies in the path of the beam. This beam will not go through solid blocks. Cooldown: %ss.",
				RANGE,
				(int) DAMAGE_1,
				COOLDOWN_1 / 20
			));
		mInfo.mDescriptions.add(
			String.format("The beam instead deals %s damage. Cooldown: %ss.",
				(int) DAMAGE_2,
				COOLDOWN_2 / 20));
		mInfo.mDescriptions.add("Mana lance now has two charges.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, isLevelOne() ? COOLDOWN_1 : COOLDOWN_2);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.TRIDENT, 1);

		mMaxCharges = (isEnhanced() ? 2 : 1) + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mInfo.mIgnoreCooldown = true;
		mCharges = getTrackedCharges();
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ManaLanceCS(), ManaLanceCS.SKIN_LIST);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null || mPlayer.isSneaking() || !ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}

		int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		float damage = mDamage;
		damage = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		damage = SpellPower.getSpellDamage(mPlugin, mPlayer, damage);

		Location loc = mPlayer.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);
		Vector dir = loc.getDirection();
		box.shift(dir);
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 10, mPlayer);
		World world = mPlayer.getWorld();

		Location endLoc = loc;
		int i = 0;
		boolean hit = false;
		int range = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RANGE);
		while (i < range) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			endLoc = bLoc;

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(dir.multiply(0.5));
				mCosmetic.lanceHitBlock(mPlayer, bLoc, world);
				break;
			}
			Iterator<LivingEntity> iter = mobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (box.overlaps(mob.getBoundingBox())) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true);
					MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f, true);
					iter.remove();
					if (!hit) {
						mCosmetic.lanceHit(bLoc, mPlayer);
						hit = true;
					}
				}
			}
			i++;
		}

		mCosmetic.lanceParticle(mPlayer, loc, endLoc, i, range);
		mCosmetic.lanceSound(world, mPlayer);
	}

	@Override
	public boolean incrementCharge() {
		return super.incrementCharge();
	}
}
