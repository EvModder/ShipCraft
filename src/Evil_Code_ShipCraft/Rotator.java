package Evil_Code_ShipCraft;

public class Rotator {
	int centerX, centerY;
	short sin, cos;
	
	public Rotator(int centerX, int centerY, double radianAngle){
		this.centerX = centerX;
		this.centerY = centerY;
		sin = (short) Math.sin(radianAngle);
		cos = (short) Math.cos(radianAngle);
	}
	
	public int[] rotatePoint(int x, int y){
		x -= centerX;
		y -= centerY;

		return new int[]{centerX + (x*cos-y*sin), centerY + (x*sin+y*cos)};
	}
	public double[] rotatePointExact(double x, double y){
		x -= centerX;
		y -= centerY;
		return new double[]{centerX + (x*cos-y*sin), centerY + (x*sin+y*cos)};
	}
	
	public static void main(String[] args) {
		Rotator instance = new Rotator(3, 3, Math.PI/2);
		
		int[] point = instance.rotatePoint(2, 2);
		System.out.println("Rotation of (2,2) = ("+point[0]+','+point[1]+')');
		
		point = instance.rotatePoint(4, 2);
		System.out.println("Rotation of (4,2) = ("+point[0]+','+point[1]+')');
		
		point = instance.rotatePoint(4, 4);
		System.out.println("Rotation of (4,4) = ("+point[0]+','+point[1]+')');
		
		point = instance.rotatePoint(2, 4);
		System.out.println("Rotation of (2,4) = ("+point[0]+','+point[1]+')');
	}
}
