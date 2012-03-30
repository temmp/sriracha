package sriracha.frontend.android.model;

import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import sriracha.frontend.*;
import sriracha.frontend.android.*;
import sriracha.frontend.model.*;

import java.util.*;

abstract public class CircuitElementView extends ImageView implements View.OnTouchListener, View.OnLongClickListener
{
    private static final int[] STATE_DRAGGABLE = {R.attr.state_draggable};
    private static final int[] STATE_SELECTED = {R.attr.state_selected};

    private static final int INVALID_POINTER_ID = -1;

    private CircuitElement element;
    private CircuitElementPortView ports[];

    private boolean isDraggable;
    private boolean isElementSelected;

    private OnElementClickListener onElementClickListener;
    private OnDrawListener onDrawListener;

    private float positionX;
    private float positionY;
    private float orientation;

    private float touchDownDeltaX;
    private float touchDownDeltaY;
    private float touchDownRawX;
    private float touchDownRawY;

    private int activePointerId = INVALID_POINTER_ID;
    private int possibleClickPointerId = INVALID_POINTER_ID;

    abstract public int getDrawableId();

    abstract public CircuitElementPortView[] getElementPorts();

    public CircuitElementView(Context context, CircuitElement element, float positionX, float positionY)
    {
        super(context);

        setBackgroundResource(R.drawable.circuitelement_background);
        setImageResource(getDrawableId());

        this.element = element;
        this.positionX = positionX;
        this.positionY = positionY;
        ports = getElementPorts();

        setOnTouchListener(this);
        setOnLongClickListener(this);
    }

    public CircuitElement getElement()
    {
        return element;
    }

    public float getPositionX() { return positionX; }
    public float getPositionY() { return positionY; }

    public float getOrientation() { return orientation; }
    public void setOrientation(float orientation)
    {
        this.orientation = orientation;
        invalidate();
    }
    public void rotate(int degrees)
    {
        setOrientation((orientation + degrees) % 360);
    }

    public CircuitElementPortView getClosestPort(float x, float y)
    {
        CircuitElementPortView closestPort = null;
        float closestDistance = 0;
        for (CircuitElementPortView port : ports)
        {
            float portX = port.getRelativeX();
            float portY = port.getRelativeY();
            float distance = new PointF(portX - x, portY - y).length();
            if (closestPort == null || distance < closestDistance)
            {
                closestDistance = distance;
                closestPort = port;
            }
        }
        return closestPort;
    }

    public boolean isDraggable() { return isDraggable; }
    public void setDraggable(boolean draggable)
    {
        isDraggable = draggable;
        if (!isDraggable)
            activePointerId = INVALID_POINTER_ID;
        refreshDrawableState();
    }

    public boolean isElementSelected() { return isElementSelected; }
    public void setElementSelected(boolean elementSelected)
    {
        isElementSelected = elementSelected;
        refreshDrawableState();
    }

    public void setOnElementClickListener(OnElementClickListener onElementClickListener)
    {
        this.onElementClickListener = onElementClickListener;
    }

    public void setOnDrawListener(OnDrawListener onDrawListener)
    {
        this.onDrawListener = onDrawListener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:
            {
                touchDownDeltaX = motionEvent.getX();
                touchDownDeltaY = motionEvent.getY();

                touchDownRawX = motionEvent.getRawX();
                touchDownRawY = motionEvent.getRawY();

                // Save the ID of this pointer
                activePointerId = motionEvent.getPointerId(0);
                possibleClickPointerId = activePointerId;

                if (!isDraggable())
                    return false;

                break;
            }

            case MotionEvent.ACTION_MOVE:
            {
                float distance = new PointF(motionEvent.getRawX() - touchDownRawX, motionEvent.getRawY() - touchDownRawY).length();
                if (distance > 8)
                    possibleClickPointerId = INVALID_POINTER_ID;

                if (!isDraggable())
                    return false;

                if (activePointerId == INVALID_POINTER_ID)
                    break;

                // Find the index of the active pointer and fetch its position
                int pointerIndex = motionEvent.findPointerIndex(activePointerId);

                // We record the segment verticalities before moving the element, otherwise the crookedness of the
                // segment prevents proper verticality measurement.
                ArrayList<Boolean> segmentVerticalities = new ArrayList<Boolean>();
                for (CircuitElementPortView port : ports)
                {
                    for (WireSegment segment : port.getSegments())
                        segmentVerticalities.add(segment.isVertical());
                }

                positionX = CircuitDesigner.snap(positionX + motionEvent.getX(pointerIndex) - touchDownDeltaX);
                positionY = CircuitDesigner.snap(positionY + motionEvent.getY(pointerIndex) - touchDownDeltaY);

                int i = 0;
                for (CircuitElementPortView port : ports)
                {
                    for (WireSegment segment : port.getSegments())
                    {
                        if (segmentVerticalities.get(i++))
                            segment.moveX(port.getX(), segment.otherEnd(port));
                        else
                            segment.moveY(port.getY(), segment.otherEnd(port));
                    }
                }

                updatePosition();
                break;
            }

            case MotionEvent.ACTION_UP:
                setDraggable(false);
                if (possibleClickPointerId != INVALID_POINTER_ID)
                {
                    possibleClickPointerId = INVALID_POINTER_ID;
                    onClick(motionEvent.getX(), motionEvent.getY());
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                setDraggable(false);
                break;
        }

        return true;
    }

    public void onClick(float x, float y)
    {
        if (onElementClickListener != null)
            onElementClickListener.onElementClick(this, x, y);
    }

    @Override
    public boolean onLongClick(View view)
    {
        if (activePointerId == INVALID_POINTER_ID)
            return false;

        setDraggable(true);
        setElementSelected(false);
        return false;
    }

    public void updatePosition()
    {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
        params.leftMargin = (int) positionX;
        params.topMargin = (int) positionY;
        setLayoutParams(params);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace)
    {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
        if (isDraggable())
            mergeDrawableStates(drawableState, STATE_DRAGGABLE);
        if (isElementSelected())
            mergeDrawableStates(drawableState, STATE_SELECTED);
        return drawableState;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.rotate(orientation, getWidth() / 2, getHeight() / 2);
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        canvas.drawText(element.getName(), 5, 14, paint);

        if (onDrawListener != null)
            onDrawListener.onDraw(canvas);
    }


    public interface OnElementClickListener
    {
        public void onElementClick(View view, float x, float y);
    }

    public interface OnDrawListener
    {
        public void onDraw(Canvas canvas);
    }
}