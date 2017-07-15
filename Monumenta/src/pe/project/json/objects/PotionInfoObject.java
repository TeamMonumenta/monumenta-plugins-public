package pe.project.json.objects;

import org.bukkit.potion.PotionEffectType;

import pe.project.utils.PotionUtils.PotionInfo;

public class PotionInfoObject {
	public String type;
	public int duration;
	public int amplifier;
	public boolean ambient;
	public boolean showParticles;
	
	public PotionInfoObject(String _type, int _duration, int _amplifier, boolean _ambient, boolean _showParticles) {
		type = _type;
		duration = _duration;
		amplifier = _amplifier;
		ambient = _ambient;
		showParticles = _showParticles;
	}
	
	public PotionInfo convtertToPotionInfo() {
		PotionInfo info = new PotionInfo();
		
		info.type = PotionEffectType.getByName(type);
		info.duration = duration;
		info.amplifier = amplifier;
		info.ambient = ambient;
		info.showParticles = showParticles;
		
		return info;
	}
}
