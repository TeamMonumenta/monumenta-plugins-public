package pe.project;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.point.Point;

public class Constants {
	public static final int TICKS_PER_SECOND = 20;
	public static final int HALF_TICKS_PER_SECOND = (int)(TICKS_PER_SECOND / 2.0);
	public static final int QUARTER_TICKS_PER_SECOND = (int)(HALF_TICKS_PER_SECOND / 2.0);
	
	public static final int TWO_MINUTES = TICKS_PER_SECOND * 60 * 2;
	public static final int THIRTY_SECONDS = TICKS_PER_SECOND * 30;
	
	public static final Point SPAWN_POINT = new Point(-734, 107, 50);
	public static final Point RESET_POINT = new Point(-1450, 237, -1498);
	
	public static final PotionEffect CAPITAL_SPEED_EFFECT = new PotionEffect(PotionEffectType.SPEED, TICKS_PER_SECOND, 1, true, false);
	public static final PotionEffect CITY_SATURATION_EFFECT = new PotionEffect(PotionEffectType.SATURATION, TICKS_PER_SECOND, 1, true, false);
	public static final PotionEffect CITY_RESISTENCE_EFFECT = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, TICKS_PER_SECOND, 4, true, false);
}
