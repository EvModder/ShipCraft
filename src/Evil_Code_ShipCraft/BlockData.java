package Evil_Code_ShipCraft;

import org.bukkit.Material;

public class BlockData {
	final int x, y, z;
	final String id;
	final Material mat;
	
	public BlockData(String id, int x, int y, int z){
		this.x = x; this.y = y; this.z = z;
		this.id = id;
		
		if(id.contains(":"))mat = Material.getMaterial(id.split(":")[0]);
		else mat = Material.getMaterial(id);
	}
}
