package sriracha.frontend.android.results;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import sriracha.frontend.android.EpicTouchListener;
import sriracha.frontend.android.results.functions.Function;
import sriracha.frontend.resultdata.Plot;

import java.util.ArrayList;
import java.util.List;


public class Graph extends FrameLayout {

    Axis yAxis;

    Axis xAxis;

    private boolean deferInvalidate = false;


    private List<PlotView> plots;

    public Graph(Context context) {
        super(context);
        init();

    }


    public Graph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Graph(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Measures the graph, makes 2 passes on Axes in order to determine relative placement
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //get available width and height
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        //measure spec for first pass on axes in order to determine their cross section sizes and relative placement
        int startWSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
        int startHSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        //for first pass we assume axis range is entire graph is no snapping
        int xrange = width, yrange = height;
        //tell axes about their available range so they can estimate pixel to coordinate mapping
        xAxis.setPixelRange(xrange);
        yAxis.setPixelRange(yrange);

        //tell axes about where they intersect the other axis so that they can estimate label size and position
        //also takes care of counting and filling labels
        xAxis.preMeasure(yAxis.pixelsFromCoordinate(0), height);
        yAxis.preMeasure(xAxis.pixelsFromCoordinate(0), width);

        //do first measure pass
        yAxis.measure(startWSpec, startHSpec);
        xAxis.measure(startWSpec, startHSpec);

        //now find out how large the cross section of each axis is, (from start of label to where axis line is drawn)
        // still only an estimate because we cant be 100% sure of label contents 
        int yAxisOffset = yAxis.getMeasuredWidth() - Axis.lineSpace / 2 + 1;
        int xAxisOffset = xAxis.getMeasuredHeight() - Axis.lineSpace / 2 + 1;

        //tell them about their pairs offset so they can tell if the other is snapped to start
        // in which case add a label at the very start of the axis. (this also ensures the first label on logscale is always present)
        yAxis.setPairedXSize(xAxisOffset);
        xAxis.setPairedXSize(yAxisOffset);

        //get estimates for desired axis intersect positions.
        float x0 = xAxis.pixelsFromCoordinate(0);
        float y0 = yAxis.pixelsFromCoordinate(0);

        //will be set true if any of the axes are snapped to an edge
       // boolean isAnySnapped = false;
        
        //if the y axis will end up aligned to the start or end then it is considered snapped and we
        //reduce the range of the xAxis so it no longer overlaps with the yAxis
        if (x0 < yAxisOffset || x0 > width - yAxisOffset) {
            xrange -= yAxisOffset;
            xAxis.setPixelRange(xrange);
            xAxis.preMeasure(y0, height);
          //  isAnySnapped = true;
        }

        //Similarly, if the x axis will end up aligned to the start or end then it is considered snapped and we
        //reduce the range of the yAxis so it no longer overlaps with the xAxis
        if (y0 < xAxisOffset || y0 > height - xAxisOffset) {
            yrange -= xAxisOffset;
            yAxis.setPixelRange(yrange);
            yAxis.preMeasure(x0, width);
          //  isAnySnapped = true;
        }
        
      //  if(isAnySnapped)
       // {
            //tell axes about where they really intersect the other axis so that they can finalize counting and filling labels
        //    xAxis.preMeasure(yAxis.pixelsFromCoordinate(0), height);
         //   yAxis.preMeasure(xAxis.pixelsFromCoordinate(0), width);
       // }

        //create internal size specs for plot children and axes with correct x and y ranges
        int internalWidthSpec = MeasureSpec.makeMeasureSpec(xrange, MeasureSpec.AT_MOST);
        int internalHeightSpec = MeasureSpec.makeMeasureSpec(yrange, MeasureSpec.AT_MOST);

        //measure all children with internal spec including axes since they their pixel 
        // ranges might have changed as a result of snapping
        for(int i =0; i< getChildCount(); i++)
        {
            getChildAt(i).measure(internalWidthSpec, internalHeightSpec);
        }


        //graph takes all initial available size.
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        //distance from axis to far ege of axis, or width of labels + linespace /2
        int yAxisOffset = yAxis.getMeasuredWidth() - Axis.lineSpace /2 + 1;
        int xAxisOffset = xAxis.getMeasuredHeight() - Axis.lineSpace /2 + 1;

        // the pixels where x and y are = 0 (for placing the paired axis)
        float x0 = xAxis.pixelsFromCoordinate(0);
        float y0 = yAxis.pixelsFromCoordinate(0);

        //internal edges of the graph (everything if no edges are snapped)
        //internal left edge is 0 unless the y axis is snapped to the left
        int iLeft = x0 < yAxisOffset ? yAxisOffset : 0;
        //internal right edge is 0 unless y axis is snapped to the right
        int iRight = x0 > yAxisOffset && xAxis.getMeasuredWidth() < width ? width - yAxisOffset : width;
        int iTop = y0 < xAxisOffset ? xAxisOffset : 0;
        int iBottom = y0 > xAxisOffset && yAxis.getMeasuredHeight() < height ? height - xAxisOffset : height;

        int yleft = iLeft != 0 ? 0 : (int) yAxis.getEdgeOffset();

        yAxis.layout(yleft, iTop, yleft + yAxis.getMeasuredWidth(), iBottom);

        int xtop = iTop == 0 ? (int) xAxis.getEdgeOffset() : 0;

        xAxis.layout(iLeft, xtop, iRight, xtop + xAxis.getMeasuredHeight());

        for (PlotView pv : plots) {
            pv.layout(iLeft, iTop, iRight, iBottom);
        }

    }


    private void init() {
        yAxis = new Axis(getContext());
        addView(yAxis, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        yAxis.setOrientation(LinearLayout.VERTICAL);
        xAxis = new Axis(getContext());
        addView(xAxis, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        xAxis.setOrientation(LinearLayout.HORIZONTAL);
        plots = new ArrayList<PlotView>();
        setOnTouchListener(new GraphGestureListener());


    }


    private class GraphGestureListener extends EpicTouchListener {

        @Override
        public boolean onSingleFingerMove(float distanceX, float distanceY) {

            // yAxis.pan(distanceY);
            // xAxis.pan(distanceX);

            double ymin = yAxis.coordinateFromPixel(yAxis.getHeight() - distanceY);
            double xmin = xAxis.coordinateFromPixel(-distanceX);


            double ymax = yAxis.coordinateFromPixel(-distanceY);
            double xmax = xAxis.coordinateFromPixel(xAxis.getWidth() - distanceX);
            
            //little hack to make sure there is no scaling going on during panning of a linear axis
            if(xAxis.getScaleType() == Axis.LINEARSCALE)
            {
                double minshift = xmin - xAxis.getMinValue();
                double maxshift = xmax - xAxis.getMaxValue();
                double avg = (minshift + maxshift) / 2;
                xmin = xAxis.getMinValue() + avg;
                xmax = xAxis.getMaxValue() + avg;
            }
            if(yAxis.getScaleType() == Axis.LINEARSCALE)
            {
                double minshift = ymin - yAxis.getMinValue();
                double maxshift = ymax - yAxis.getMaxValue();
                double avg = (minshift + maxshift) / 2;
                ymin = yAxis.getMinValue() + avg;
                ymax = yAxis.getMaxValue() + avg;
            }
            

            xAxis.setRange(xmin, xmax);
            yAxis.setRange(ymin, ymax);
            
            requestLayout();
            invalidate();
            return true;
        }


        @Override
        protected boolean onScale(float xFactor, float yFactor, float xCenter, float yCenter) {

            double xMinSize = xCenter/xAxis.getWidth();
            double xMaxSize = xAxis.getWidth() - xMinSize;
            double yMaxSize = yCenter/yAxis.getHeight();
            double yMinSize = yAxis.getHeight() - yMaxSize;

            double xMin = xAxis.coordinateFromPixel((float) (xCenter - xFactor * xMinSize));
            double xMax = xAxis.coordinateFromPixel((float) (xCenter + xFactor * xMaxSize));
            double yMin = xAxis.coordinateFromPixel((float) (yCenter + yFactor * yMinSize));
            double yMax = xAxis.coordinateFromPixel((float) (yCenter - yFactor * yMaxSize));

            xAxis.setRange(xMin, xMax);
            yAxis.setRange(yMin, yMax);


            requestLayout();
            invalidate();
            return true;
        }
    }

    public void beginEdit()
    {
        deferInvalidate = true;
    }

    public void endEdit()
    {
        deferInvalidate = false;
        requestLayout();
        invalidate();
    }


    public void addPlot(Plot plot, int color) {
        addPlot(plot, color, null);

    }

    public void addPlot(Plot plot, int color, Function f) {
        PlotView plotView = new PlotView(this, getContext());
        plotView.setColor(color);
        plotView.setPlot(plot);
        plotView.setFunc(f);
        plots.add(plotView);
        addView(plotView);
        
        if(!deferInvalidate)
        {
            requestLayout();
            invalidate();
        }

    }

    public void clearPlots() {
        for (PlotView pv : plots) {
            removeView(pv);
        }
        plots.clear();
    }


    public float[] pixelsFromCoords(double x, double y) {
        return new float[]{xAxis.pixelsFromCoordinate(x), yAxis.pixelsFromCoordinate(y)};
    }

    
    public void setXLogScale(boolean islogscale)
    {
        int newscale = islogscale? Axis.LOGSCALE : Axis.LINEARSCALE;
        if(xAxis.getScaleType() != newscale)
        {
            xAxis.setScaleType(newscale);
            if(!deferInvalidate)
            {
                requestLayout();
                invalidate();
            }
            
        }
    }

    public void setYLogScale(boolean islogscale)
    {
        int newscale = islogscale? Axis.LOGSCALE : Axis.LINEARSCALE;
        if(yAxis.getScaleType() != newscale)
        {
            yAxis.setScaleType(newscale);
            if(!deferInvalidate)
            {
                requestLayout();
                invalidate();
            }
            
        }
    }

    public void setYRange(double min, double max) {
        yAxis.setRange(min, max);
        if(!deferInvalidate)
        {
            requestLayout();
            invalidate();
        }
        
    }

    public void setXRange(double min, double max) {
        xAxis.setRange(min, max);
        if(!deferInvalidate)
        {
            requestLayout();
            invalidate();
        }
        
    }

    public double getYmin() {
        return yAxis.getMinValue();
    }

    public double getYmax() {
        return yAxis.getMaxValue();
    }

    public double getXmin() {
        return xAxis.getMinValue();
    }

    public double getXmax() {
        return xAxis.getMaxValue();
    }
}
