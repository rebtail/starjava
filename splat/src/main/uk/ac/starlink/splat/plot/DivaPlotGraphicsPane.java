/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.CircleManipulator;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.PathManipulator;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionListener;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.BasicEllipse;
import diva.canvas.toolbox.LabelFigure;
import diva.canvas.toolbox.TypedDecorator;
import diva.util.java2d.Polyline2D;

import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.ast.Mapping;

/**
 * The pane for displaying any interactive graphics for a Plot that
 * should be resized.
 * <p>
 * Known figure types can be created and removed from the Pane.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot
 */
public class DivaPlotGraphicsPane
    extends GraphicsPane
{
    /**
     * The controller
     */
    private DivaController controller;

    /**
     * The interactor to give to all figures
     */
    private SelectionInteractor selectionInteractor;

    /**
     * The layer to draw all figures in
     */
    private FigureLayer figureLayer;

    /**
     * Create an XRangeFigure Figure. This is useful for selecting a
     * wavelength range.
     */
    public static final int XRANGE = 0;

    /**
     * Create a rectangle with interior vertical line. This is useful
     * for selecting a wavelength range with a single special interior
     * position (i.e. a spectral line).
     */
    public static final int CENTERED_XRANGE = 1;

    /**
     * Create a simple rectangle.
     */
    public static final int RECTANGLE = 3;

    /**
     * List of all figures.
     */
    private ArrayList figureList = new ArrayList();

    /**
     * DragRegion, used in addition to SelectionDragger. This is used
     * for implementing the interactive zoom functions.
     */
    private DragRegion dragRegion;

    /**
     *  Constructor.
     */
    public DivaPlotGraphicsPane()
    {
        super();

        // Get the figure layer
        figureLayer = getForegroundLayer();

        // Set the halo size used to select figures. Default is 0.5,
        // which means pointing to within a pixel, which is a bit
        // tight.
        figureLayer.setPickHalo( 2.0 );

        // Construct a simple controller and get the default selection
        // interactor.
        controller = new DivaController( this );
        selectionInteractor = controller.getSelectionInteractor();

	// Use a generic decorator that can be tuned to use
        // different actual manipulators according to the type of
        // figure. The default manipulator is a BoundsManipulator.
        Manipulator man = new BoundsManipulator();
        TypedDecorator decorator = new TypedDecorator( man );

        // Tell the controller to use this decorator for deciding how
        // to wrap selected figures.
        selectionInteractor.setPrototypeDecorator( decorator );

        //  Set manipulators for each figure type, if required.
        man = new RangeManipulator();
        decorator.addDecorator( XRangeFigure.class, man );

        man = new PathManipulator();
        decorator.addDecorator( PolylineFigure.class, man );

        man = new InterpolatedCurveManipulator();
        decorator.addDecorator( InterpolatedCurveFigure.class, man );

        man = new CircleManipulator();
        decorator.addDecorator( BasicEllipse.class, man );

        // Add the additional drag region interactor to work with
        // mouse button 2.
        MouseFilter mFilter = new MouseFilter( InputEvent.BUTTON2_MASK|
                                               InputEvent.BUTTON3_MASK );
        dragRegion = new DragRegion( this );
        dragRegion.setSelectionFilter( mFilter );
    }

    /**
     *  Create and add a figure to the canvas.
     *
     *  @param type the type of figure to create.
     *  @param props the initial properties of the figure.
     */
    public Figure addFigure( int type, FigureProps props )
    {
        Figure newFigure = null;
        switch ( type ) {
           case XRANGE:
               newFigure = createXRange( props );
               break;
           case CENTERED_XRANGE:
               newFigure = createXRangeWithFeature( props );
               break;
           case RECTANGLE:
               newFigure = createRectangle( props );
               break;
        }
        recordFigure( newFigure );

        return newFigure;
    }

    /**
     * Make and return a figure with the given shape, fill, outline and
     * line width. The shape is expected to be in screen coordinates.
     *
     * @param shape the shape to draw
     * @param fill the paint to use to fill the shape
     * @param outline the paint to use for the outline
     * @param lineWidth the width of the shape lines in pixels
     *
     * @return the handle for  the figure
     */
    public Figure makeFigure( Shape shape, Paint fill, Paint outline, 
                              float lineWidth )
    {
        Figure newFigure = null;
        if ( shape instanceof InterpolatedCurve2D ) {
            newFigure = 
                new InterpolatedCurveFigure( (InterpolatedCurve2D) shape,
                                             outline, lineWidth );
        }
        else if ( shape instanceof Polyline2D ) {
            newFigure = new PathPlotFigure( shape, outline, lineWidth );
        }
        else {
            newFigure = new BasicPlotFigure( shape, fill, outline, lineWidth );
        }
        recordFigure( newFigure );
        return newFigure;
    }

    /**
     * Lower a Figure to the bottom of the ZList.
     */
    public void lowerFigure( Figure figure )
    {
        figureLayer.setIndex( 0, figure );
    }

    /**
     * Raise a figure to the front of the ZList.
     */
    public void raiseFigure( Figure figure ) 
    {
        figureLayer.setIndex( figureLayer.getFigureCount(), figure );
    }

    /**
     *  Get the selection interactor.
     */
    public SelectionInteractor getSelectionInteractor()
    {
        return selectionInteractor;
    }

    /**
     *  Get the controller.
     */
    public DivaController getController()
    {
        return controller;
    }

    /**
     *  Add a listener for any SelectionEvents.
     */
    public void addSelectionListener( SelectionListener l )
    {
        selectionInteractor.getSelectionModel().addSelectionListener(l);
    }

    /**
     *  Get a list of the currently selected Figures.
     */
    public Object[] getSelectionAsArray() 
    {
        return selectionInteractor.getSelectionModel().getSelectionAsArray();
    }

    /**
     * Clear the selection.
     */
    public void clearSelection()
    {
        selectionInteractor.getSelectionModel().clearSelection();
    }

    /**
     * Select the given figure.
     */
    public void select( Figure figure )
    {
        Interactor i = figure.getInteractor();
        if ( i instanceof SelectionInteractor ) {
            SelectionInteractor si = (SelectionInteractor) i;
            si.getSelectionModel().addSelection( figure );
        }
    }

    /**
     *  Get the figure layer that we draw into.
     */
    public FigureLayer getFigureLayer()
    {
        return figureLayer;
    }

    /**
     *  Add a FigureListener to the DragRegion used for interacting
     *  with figures.
     */
    public void addFigureDraggerListener( FigureListener l )
    {
        controller.getSelectionDragger().addListener( l );
    }

    /**
     *  Remove a FigureListener to the DragRegion used for interacting
     *  with figures.
     */
    public void removeFigureDraggerListener( FigureListener l )
    {
        controller.getSelectionDragger().removeListener( l );
    }

    /**
     *  Add a FigureListener to the DragRegion used for non-figure
     *  selection work.
     */
    public void addZoomDraggerListener( FigureListener l )
    {
        dragRegion.addListener( l );
    }

    /**
     *  Remove a FigureListener from the DragRegion used for non-figure
     *  selection work.
     */
    public void removeZoomDraggerListener( FigureListener l )
    {
        dragRegion.removeListener( l );
    }

    /**
     *  Create a new XRangeFigure.
     */
    protected Figure createXRange( FigureProps props )
    {
        return new XRangeFigure( props.getXCoordinate(),
                                 props.getYCoordinate(),
                                 props.getXLength(),
                                 props.getYLength(),
                                 props.getMainColour() );
    }

    /**
     *  Create a new XRangeWithFeatureFigure.
     */
    protected Figure createXRangeWithFeature( FigureProps props )
    {
        return (Figure)
            new XRangeWithFeatureFigure( props.getXCoordinate(),
                                         props.getYCoordinate(),
                                         props.getXSecondary(),
                                         props.getXLength(),
                                         props.getYLength(),
                                         props.getMainColour(),
                                         props.getSecondaryColour() );
    }

    /**
     *  Create a new PlotRectangle.
     */
    protected Figure createRectangle( FigureProps props )
    {
        return new PlotRectangle( props.getXCoordinate(),
                                  props.getYCoordinate(),
                                  props.getXLength(),
                                  props.getYLength(),
                                  props.getMainColour() );
    }

    /**
     *  Create a new PlotLabelFigure.
     */
    protected Figure createLabelFigure( double x, double y,
                                        String text, Paint outline,
                                        Font font )
    {
        PlotLabelFigure label = new PlotLabelFigure( text, font );
        label.setFillPaint( outline );
        label.translateTo( x, y );
        return label;
    }

    /**
     * Record the creation of a new figure.
     */
    protected void recordFigure( Figure newFigure )
    {
        figureLayer.add( newFigure );
        newFigure.setInteractor( selectionInteractor );
        figureList.add( newFigure );
    }

    /**
     *  Remove a Figure.
     *
     *  @param figure the figure to remove.
     */
    public void removeFigure( Figure figure )
    {
        Interactor interactor = figure.getInteractor();
        if ( interactor instanceof SelectionInteractor ) {
            // remove any selection handles, etc.
            SelectionModel model = 
                ((SelectionInteractor) interactor).getSelectionModel();
            if ( model.containsSelection( figure ) ) {
                model.removeSelection( figure );
            }
        }
        figureLayer.remove( figure );
        figureList.remove( figure);
    }

    /**
     * Return index of figure.
     */
    public int indexOf( Figure figure )
    {
        return figureList.indexOf( figure );
    }

    /**
     * Return the current properties of a figure.
     */
    public FigureProps getFigureProps( Figure figure )
    {
        Rectangle2D bounds = figure.getBounds();
        return new FigureProps( bounds.getX(), bounds.getY(),
                                bounds.getWidth(), bounds.getHeight() );
    }

    /**
     * Switch off selection using the drag box interactor.
     */
    public void disableFigureDraggerSelection()
    {
        //  Do this by the slight of hand that replaces the
        //  FigureLayer with one that has no figures.
        dragRegion.setFigureLayer( emptyFigureLayer );
    }
    private FigureLayer emptyFigureLayer = new FigureLayer();

    /**
     * Switch selection using the drag box interactor back on.
     */
    public void enableFigureDraggerSelection()
    {
        dragRegion.setFigureLayer( figureLayer );
    }

    /**
     *  Transform the positions of all figures from one graphics
     *  coordinate system to another. The first AST mapping should
     *  transform from old graphics coordinates to some intermediary
     *  system (like wavelength,counts) and the second back from this
     *  system to the new graphics coordinates.
     */
    public void astTransform( Mapping oldMapping, Mapping newMapping )
    {
        //  TODO: could do all this using a single AffineTransform?

        // Switch off figure resizing constraints
        new BasicPlotFigure().setTransformFreely( true );

        double[] oldCoords = new double[4];
        double[] tmpCoords = new double[4];
        double[][] neutralCoords = null;
        double[][] newCoords = null;
        for ( int i = 0; i < figureList.size(); i++ ) {

            Figure figure = (Figure) figureList.get( i );
            Rectangle2D rect = figure.getBounds();

            oldCoords[0] = rect.getX();
            oldCoords[1] = rect.getY();
            oldCoords[2] = rect.getX() + rect.getWidth();
            oldCoords[3] = rect.getY() + rect.getHeight();

            neutralCoords = ASTJ.astTran2( oldMapping, oldCoords, true );

            tmpCoords[0] = neutralCoords[0][0];
            tmpCoords[1] = neutralCoords[1][0];
            tmpCoords[2] = neutralCoords[0][1];
            tmpCoords[3] = neutralCoords[1][1];

            newCoords = ASTJ.astTran2( newMapping, tmpCoords, false );

            double xscale = ( newCoords[0][1] - newCoords[0][0] ) /
                            ( oldCoords[2] - oldCoords[0] );
            double yscale = ( newCoords[1][1] - newCoords[1][0] ) /
                            ( oldCoords[3] - oldCoords[1] );

            AffineTransform at = new AffineTransform();

            at.translate( newCoords[0][0], newCoords[1][0] );
            at.scale( xscale, yscale );
            at.translate( -oldCoords[0], -oldCoords[1] );

            figure.transform( at );
        }
        new BasicPlotFigure().setTransformFreely( false );
    }
}
