/*package Evil_Code_ShipCraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Button;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

public class ShipLoader {
	private ShipCraft shipCraft;
	
	public ShipLoader(ShipCraft sc){shipCraft = sc;}
	
	// count collisions
	public Location[] getCollisions(ShipSchematic ship, List<Block> blocksToMove, Vector v){
		int moveX = v.getBlockX(), moveY = v.getBlockY(), moveZ = v.getBlockZ();
		Material[] ignoredMaterials = shipCraft.getIgnoredCollisionMaterialsByShipType(ship.type);
		World world = ship.world;
		
		//
		List<Location> collisions = new ArrayList<Location>();
		//
		boolean collision;
		for(Block b : blocksToMove){
			Block toBlock = world.getBlockAt(b.getX() + moveX, b.getY() + moveY, b.getZ() + moveZ);
			
			if(isFromMaterialList(toBlock.getType(), ignoredMaterials) == false){
				collision = true;
				Location cLoc = toBlock.getLocation();
				for(Block b2 : blocksToMove) if(b2.getLocation().distance(cLoc) == 0) collision = false;
				if(collision) collisions.add(cLoc);
			}
		}
		return collisions.toArray(new Location[]{});
	}
	
	// count collisions
	public Location[] getCollisions(ShipSchematic ship, List<Block> blocksToMove, int angle){
		Material[] ignoredMaterials = shipCraft.getIgnoredCollisionMaterialsByShipType(ship.type);
		World world = ship.world;
		List<Location> collisions = new ArrayList<Location>();
		int centX = ship.controlBlock.getX(), centZ = ship.controlBlock.getZ();
		
		//------------------------ Get the degree of the turn ------------------------//		
		int sign = 1;
		if	   ((angle-90) % 360 == 0)sign = 1;//turning left
		else if((angle+90) % 360 == 0)sign =-1;//turing right
		else if(angle % 180 == 0)sign = 0;//turning 180
		//----------------------------------------------------------------------------//
		
		boolean collision;
		for(Block b : blocksToMove){
			int x = centX + sign*(b.getZ() - centZ);
			int z = centZ + sign*(b.getX() - centX);
			if(sign == 0) x = 2*centX-x; z = 2*centZ-z;
			Block toBlock = world.getBlockAt(x, b.getY(), z);
			
			if(isFromMaterialList(toBlock.getType(), ignoredMaterials) == false){
				collision = true;
				Location cLoc = toBlock.getLocation();
				for(Block b2 : blocksToMove) if(b2.getLocation().distance(cLoc) == 0) collision = false;
				if(collision) collisions.add(cLoc);
			}
		}
		return collisions.toArray(new Location[]{});
	}
	
	//WARNING:
	// Do collisions damage/action before using the below method to move the ship, otherwise blocks may be overwritten!
	@SuppressWarnings("deprecation")
	public List<Block> move(ShipSchematic ship, List<Block> blocksToMove, Vector v){
		int moveX = v.getBlockX(), moveY = v.getBlockY(), moveZ = v.getBlockZ();
		List<Block> newBlocks = new ArrayList<Block>();
		
		//convert to BlockState to prevent data resetting
		List<BlockState> blockData = new ArrayList<BlockState>();
		for(Block b : blocksToMove) blockData.add(b.getState());
				
		for(BlockState oldBlock : blockData) oldBlock.getBlock().setType(Material.AIR);
		
		//loop though the 'BlockState's and move the blocks according to the vector.
		for(BlockState oldBlock : blockData){
			Block newBlock = ship.world.getBlockAt(oldBlock.getX() + moveX, oldBlock.getY() + moveY, oldBlock.getZ() + moveZ);
			newBlock.setType(oldBlock.getType());
			//
			newBlock.setData(oldBlock.getRawData());
			MaterialData oldData = oldBlock.getData();
			newBlock.getState().setData(oldData);
			//
			if(newBlock.getType() == Material.STONE_BUTTON || newBlock.getType() == Material.WOOD_BUTTON){
				Button button = (Button) newBlock.getState().getData();
				button.setPowered(false);
				newBlock.getState().setData(button);
			}
			if(oldBlock instanceof InventoryHolder){
		//		InventoryHolder ih = (InventoryHolder) oldBlock;
		//		InventoryHolder newIh = (InventoryHolder) newBlock.getState();
		//		newIh.getInventory().setContents(ih.getInventory().getContents());
			}
			newBlock.getState().update();
			newBlocks.add(newBlock);
		}
		// return the new list of blocks composing the ship
		return newBlocks;
	}
	
	public List<Block> rotate(ShipSchematic ship, List<Block> blocksToMove, int angle){
		int sign = 1;
		if	   ((angle-90) % 360 == 0){sign = 1;angle = 90;}//turning left
		else if((angle+90) % 360 == 0){sign =-1;angle =-90;}//turing right
		else if(angle % 180 == 0){sign = 0; angle = 180;}//turning around
		
		int centX = ship.controlBlock.getX(), centZ = ship.controlBlock.getZ();
		List<Block> newBlocks = new ArrayList<Block>();
		BlockFace oldDirection = ship.directionFacing;
		ship.directionFacing = getNewDirection(oldDirection, angle);
		shipCraft.getLogger().info("New direction: "+ship.directionFacing.toString());
		if(ship.directionFacing == oldDirection) return null;
		
		//convert to BlockState to prevent data resetting
		List<BlockState> blockData = new ArrayList<BlockState>();
		for(Block b : blocksToMove) blockData.add(b.getState());
				
		for(BlockState oldBlock : blockData) oldBlock.getBlock().setType(Material.AIR);
		
		//loop though the 'BlockState's and move the blocks according to the vector.
		for(BlockState oldBlock : blockData){
			int x = centX + sign*(oldBlock.getZ() - centZ);
			int z = centZ + sign*(oldBlock.getX() - centX);
			if(sign == 0) x = 2*centX - x; z = 2*centZ - z;
			Block newBlock = ship.world.getBlockAt(x, oldBlock.getY(), z);
			//
			newBlock.setType(oldBlock.getType());
			newBlock.getState().setType(oldBlock.getType());
			//
			MaterialData oldData = oldBlock.getData();
			newBlock.getState().setData(oldData);
			//
			if(newBlock.getType() == Material.STONE_BUTTON || newBlock.getType() == Material.WOOD_BUTTON){
				Button button = (Button) newBlock.getState().getData();
				button.setPowered(false);
				newBlock.getState().setData(button);
			}
			if(oldBlock instanceof Directional){
				shipCraft.getLogger().info("directional block");
				Directional ddata = (Directional) oldBlock;
				ddata.setFacingDirection(getNewDirection(ddata.getFacing(), angle));
				newBlock.getState().setData((MaterialData) ddata);
				newBlock.getState().update();
			}
			if(oldBlock instanceof InventoryHolder){
//				shipCraft.getLogger().info("container block");
//				InventoryHolder ih = (InventoryHolder) oldBlock;
//				InventoryHolder newIh = (InventoryHolder) newBlock.getState();
//				newIh.getInventory().setContents(ih.getInventory().getContents());
			}
			newBlock.getState().update();
			newBlocks.add(newBlock);
		}
		// return the new list of blocks composing the ship
		return newBlocks;
	}
	
	public BlockFace getNewDirection(BlockFace bf, int angle){
		if(angle == 180) return bf.getOppositeFace();
		
		else if(angle ==90){//turning left
			if(bf == BlockFace.NORTH) return BlockFace.WEST;
			else if(bf == BlockFace.WEST) return BlockFace.SOUTH;
			else if(bf == BlockFace.SOUTH) return BlockFace.EAST;
			else if(bf == BlockFace.EAST) return BlockFace.NORTH;
		}
		else if(angle ==-90){//turning right
			if(bf == BlockFace.NORTH) return BlockFace.EAST;
			else if(bf == BlockFace.EAST) return BlockFace.SOUTH;
			else if(bf == BlockFace.SOUTH) return BlockFace.WEST;
			else if(bf == BlockFace.WEST) return BlockFace.NORTH;
		}
		//if turn is not divisible by 90, or if it is a 360 degree turn, then return input
		shipCraft.getLogger().info("Block not rotated, block face: "+bf);
		return bf;
	}
	
	@SuppressWarnings("deprecation")
	public List<Block> getBlocksInShip_old(ShipSchematic ship){
		List<Block> blockList = new ArrayList<Block>();
		Material[] ignoredTypes = shipCraft.getIgnoredCountMaterialsByShipType(ship.type);
		
		shipCraft.getLogger().info("ship saveblocklist size: "+ship.data.length);
		// Load all the blocks in the data list
		for(int i = 0; i < ship.data.length; i++){
			if(blockList.size() > shipCraft.maxShipSize)return null;
			//
			BlockData bd = ship.data[i];
			Block b = ship.world.getBlockAt(bd.x, bd.y, bd.z);
			blockList.add(b);
			
			// Update the blockdata list if it doesn't match the block actually there
			// (so long as the block actually there isn't air)
			String name = b.getType().name();
			if(name.equals(bd.id.split(":")[0]) == false && name != Material.AIR.name()){
				if(b.getData() != 0) name += ":"+b.getData();
				ship.data[i] = new BlockData(name, bd.x, bd.y, bd.z);
			}
		}
		
		//Loop through undetermined blocks to check for any to add
		List<Block> blocksToAdd = new ArrayList<Block>();
		List<Block> uncounted = new ArrayList<Block>();
		List<Block> total = new ArrayList<Block>();
		int extraBlocks = 0;
		int maxLoad = ship.data.length*2/3;
		if(maxLoad > 800) maxLoad = 800;
		int maxNumOfBlocksInSeperateHunk = 100;
		do{
			blocksToAdd.clear();
			for(Block block : blockList){
				total.addAll(blockList);
				total.addAll(blocksToAdd);
				total.addAll(uncounted);
				//--------------- Look for new blocks to add ---------------//
				List<Block> newBlocks = new ArrayList<Block>();
				newBlocks.addAll(total);
				newBlocks = getAllConnectedBlocks_old(block, ignoredTypes, newBlocks, 0, maxNumOfBlocksInSeperateHunk
						maxLoad-extraBlocks-blocksToAdd.size());
				newBlocks.removeAll(total);
				if(newBlocks.size() + blocksToAdd.size() + extraBlocks < maxLoad) blocksToAdd.addAll(newBlocks);
				else uncounted.addAll(newBlocks);
				//----------------------------------------------------------//
				total.clear();
			}
			//Bring up to 2/3 extra stuff along
			if(extraBlocks + blocksToAdd.size() <= maxLoad){
				blockList.addAll(blocksToAdd);
				extraBlocks += blocksToAdd.size();
			}
		}while(blocksToAdd.size() > 0 && blockList.size() < shipCraft.maxShipSize);
		
		shipCraft.getLogger().info("ship totalblocklist size: "+blockList.size());
		// Return all blocks determined to be part of the ship
		return blockList;
	}
	
	@SuppressWarnings("deprecation")
	public List<Block> getBlocksInShip(ShipSchematic ship){
		List<Block> blockList = new ArrayList<Block>();
		Material[] ignoredTypes = shipCraft.getIgnoredCountMaterialsByShipType(ship.type);
		
		shipCraft.getLogger().info("ship saveblocklist size: "+ship.data.length);
		// Load all the blocks in the data list
		for(int i = 0; i < ship.data.length; i++){
			if(blockList.size() > shipCraft.maxShipSize)return null;
			//
			BlockData bd = ship.data[i];
			Block b = ship.world.getBlockAt(bd.x, bd.y, bd.z);
			blockList.add(b);
			
			// Update the blockdata list if it doesn't match the block actually there
			// (so long as the block actually there isn't air)
			String name = b.getType().name();
			if(name.equals(bd.id.split(":")[0]) == false && name != Material.AIR.name()){
				if(b.getData() != 0) name += ":"+b.getData();
				ship.data[i] = new BlockData(name, bd.x, bd.y, bd.z);
			}
		}
		
		//Loop through undetermined blocks to check for any to add
		List<Block> blocksToAdd = new ArrayList<Block>();
		List<Block> uncounted = new ArrayList<Block>();
		List<Block> total = new ArrayList<Block>();
		int extraBlocks = 0;
		int maxLoad = ship.data.length*2/3;
		if(maxLoad > 800) maxLoad = 800;
		int maxNumOfBlocksInSeperateHunk = 100;
		do{
			blocksToAdd.clear();
			for(Block block : blockList){
				total.addAll(blockList);
				total.addAll(blocksToAdd);
				total.addAll(uncounted);
				//--------------- Look for new blocks to add ---------------//
				List<Block> newBlocks = new ArrayList<Block>();
				newBlocks.addAll(total);
				
				// ================ Get all connectd blocks ================
				AtomicReference<List<Block>> ref = new AtomicReference<List<Block>>(newBlocks);
				getAllConnectedBlocks(block, ignoredTypes, ref, 0, maxNumOfBlocksInSeperateHunk
						/*maxLoad-extraBlocks-blocksToAdd.size());
				//==========================================================
				
				newBlocks.removeAll(total);
				if(newBlocks.size() + blocksToAdd.size() + extraBlocks < maxLoad) blocksToAdd.addAll(newBlocks);
				else uncounted.addAll(newBlocks);
				//----------------------------------------------------------//
				total.clear();
			}
			//Bring up to 2/3 extra stuff along
			if(extraBlocks + blocksToAdd.size() <= maxLoad){
				blockList.addAll(blocksToAdd);
				extraBlocks += blocksToAdd.size();
			}
		}while(blocksToAdd.size() > 0 && blockList.size() < shipCraft.maxShipSize);
		
		shipCraft.getLogger().info("ship totalblocklist size: "+blockList.size());
		// Return all blocks determined to be part of the ship
		return blockList;
	}
	
	private void getAllConnectedBlocks(Block startBlock, Material[] ignoredTypes, AtomicReference<List<Block>> ref,
			int stackDepth, int maxBlocks){
		if(stackDepth > maxBlocks){ref.get().clear(); return;}

		World world = startBlock.getWorld();
		for(int x = startBlock.getX()-1; x <= startBlock.getX()+1; x++){
			for(int y = startBlock.getY()-1; y <= startBlock.getY()+1; y++){
				for(int z = startBlock.getZ()-1; z <= startBlock.getZ()+1; z++){
					Block b = world.getBlockAt(x, y, z);

					if(b != null && isFromMaterialList(b.getType(), ignoredTypes) == false && ref.get().contains(b) == false){

						ref.get().add(b);
						try{getAllConnectedBlocks(b, ignoredTypes, ref, stackDepth+1, maxBlocks);}
						catch(StackOverflowError error){ref.get().clear(); return;}
						//Escape line for when running into infinite attached blocks (the ground)
						if(ref.get().isEmpty()) return;
					}
				}
			}
		}
	}
	
	private List<Block> getAllConnectedBlocks_old(Block startBlock, Material[] ignoredTypes, List<Block> blockList,
																								int stackDepth, int maxBlocks){
		if(stackDepth > maxBlocks){blockList.clear(); return blockList;}
		
		World world = startBlock.getWorld();
		for(int x = startBlock.getX()-1; x <= startBlock.getX()+1; x++){
			for(int y = startBlock.getY()-1; y <= startBlock.getY()+1; y++){
				for(int z = startBlock.getZ()-1; z <= startBlock.getZ()+1; z++){
					Block b = world.getBlockAt(x, y, z);
					
					if(b != null && isFromMaterialList(b.getType(), ignoredTypes) == false && blockList.contains(b) == false){
						
						blockList.add(b);
						try{blockList = getAllConnectedBlocks_old(b, ignoredTypes, blockList, stackDepth+1, maxBlocks);}
						catch(StackOverflowError error){blockList.clear(); return blockList;}
						//Escape line for when running into infinite attached blocks (the ground)
						if(blockList.isEmpty()) return blockList;
					}
				}
			}
		}
		return blockList;
	}
	// ------------------------------------------------------------------------------------------------------------------------------
	
	public boolean isFromMaterialList(Material mat, Material[] materials){
		for(Material listMat : materials) if(mat == listMat)return true;
		return false;
	}
}
*/