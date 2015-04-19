package nodagumi.ananPJ.NetworkParts.Pollution;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;	// tkokada

import javax.media.j3d.Appearance;
import javax.media.j3d.BadTransformException;
import javax.media.j3d.ExponentialFog;
import javax.media.j3d.Fog;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import nodagumi.ananPJ.NetworkParts.MapPartGroup;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Sphere;

public class PollutedAreaPoint extends PollutedArea {

	Vector3d point;
	int roomId;
	boolean view;
	public PollutedAreaPoint(int id, int _room_id, Vector3d _point) {
		super(id);
		
		point = _point;
		roomId = _room_id;
		view = false;
	}
	//PROTOTYPE
	public Vector3d getPoint3d(){
		return point;
	}
	@Override
	public boolean contains(Vector3f point) {
		return false;
	}

	@Override
	public boolean contains(Point2D point) {
		return false;
	}

    @Override
    public boolean intersectsLine(Line2D line) {
        return false;
    }

	@Override
	public void draw(Graphics2D g, boolean experiment) {
		g.setColor(Color.BLACK);

		g.drawString(this.getTagString(),
				(float)point.x,
				(float)point.y);
	}

	@Override
	public TransformGroup get3DShape(Appearance app) {
		Transform3D trans3d = new Transform3D();

		Vector3d p = new Vector3d(point);
		p.z = point.z / ((MapPartGroup)getParent()).getScale();;
		trans3d.setTranslation(p);

		TransformGroup pollutionTransforms = null;
		try {
			pollutionTransforms = new TransformGroup(trans3d);
		} catch (BadTransformException e){
			pollutionTransforms = new TransformGroup();
			return null;
		}
		//Case Sphere
		/*
		Sphere sphere = new Sphere(3.0f, app);	
		pollutionTransforms.addChild(sphere);
		*/
		//Case Box
		Box box = new Box (8.0f,8.0f,8.0f,app);
		pollutionTransforms.addChild(box);		
		return pollutionTransforms;
	}

	@Override
	public double getDensity() {
		Object o = getUserObject();
		if (o != null && o instanceof Double) {
			double d = ((Double)o).doubleValue();
			return d;
		}
		return 0.0;
	}

	@Override
	public Shape getShape() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override // tkokada
	public ArrayList<Point2D> getAllVertices() {
		return null;
	}
	
	@Override // tkokada
	public double getAngle() {
		double d = Double.NaN;
		return d;
	}

	@Override
	public NType getNodeType() {
		return NType.ROOM;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public double distance(Vector3f _point) {
		return Math.sqrt((point.getX() - _point.getX()) * (point.getX() - _point.getX())
				+ (point.getY() - _point.getY()) * (point.getY() - _point.getY())
				+ (point.getZ() - _point.getZ()) * (point.getZ() - _point.getZ()));
	}
	@Override
	public boolean getContactOfAgents() {return view;}
	@Override
	public void setContactOfAgents(boolean _view) {view = _view;}
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
