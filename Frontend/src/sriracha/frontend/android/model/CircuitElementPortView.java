package sriracha.frontend.android.model;

import android.graphics.*;
import sriracha.frontend.android.*;

import java.util.*;

public class CircuitElementPortView implements IWireIntersection
{
    private CircuitElementView element;
    private ArrayList<WireSegment> segments = new ArrayList<WireSegment>(4);

    private float positionX;
    private float positionY;

    public CircuitElementPortView(CircuitElementView element, float positionX, float positionY)
    {
        this.element = element;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public CircuitElementView getElement() { return element; }

    public float getUntransformedPositionX() { return positionX; }
    public float getUntransformedPositionY() { return positionY; }

    public float[] getTransformedPosition()
    {
        return transformPosition(element.getOrientation());
    }

    public float[] transformPosition(float degrees)
    {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);
        float[] transformed = new float[2];
        matrix.mapPoints(transformed, new float[]{positionX, positionY});
        return transformed;
    }
    
    public float getRelativeX()
    {
        return element.getWidth() / 2 + element.getWidth() * getTransformedPosition()[0];
    }

    public float getRelativeY()
    {
        return element.getHeight() / 2 + element.getHeight() * getTransformedPosition()[1];
    }

    @Override
    public int getX()
    {
        return (int) (element.getPositionX() + element.getWidth() / 2 + element.getWidth() * getTransformedPosition()[0]);
    }

    @Override
    public int getY()
    {
        return (int) (element.getPositionY() + element.getHeight() / 2 + element.getHeight() * getTransformedPosition()[1]);
    }

    @Override
    public void addSegment(WireSegment segment)
    {
        segments.add(segment);
    }

    @Override
    public void replaceSegment(WireSegment oldSegment, WireSegment newSegment)
    {
        if (!segments.remove(oldSegment))
            throw new IllegalArgumentException("Segment not found in collection");
        segments.add(newSegment);
    }

    @Override
    public void removeSegment(WireSegment segment)
    {
        segments.remove(segment);
    }

    @Override
    public ArrayList<WireSegment> getSegments()
    {
        return segments;
    }

    @Override
    public boolean duplicateOnMove(WireSegment segment)
    {
        return true;
    }
    @Override
    public WireIntersection duplicate(WireSegment segment, WireManager wireManager)
    {
        WireIntersection newIntersection = new WireIntersection(getX(), getY());
        wireManager.addIntersection(newIntersection);

        // Connect the segment that's being moved to the new node.
        segment.replaceIntersection(this, newIntersection);
        newIntersection.addSegment(segment);

        // Connect the old node and the new node with a brand new segment
        WireSegment newSegment = new WireSegment(segment.getContext(), wireManager, this, newIntersection);
        wireManager.addSegment(newSegment);

        newIntersection.addSegment(newSegment);
        this.replaceSegment(segment, newSegment);

        return newIntersection;
    }

    @Override
    public String toString()
    {
        return "CEPV - " + (segments.size() > 0 ? segments.get(0).toString() : "()");
    }
}
