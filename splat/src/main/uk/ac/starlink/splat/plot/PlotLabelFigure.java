/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-DEC-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.plot;

import diva.canvas.toolbox.LabelFigure;

import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import javax.swing.event.EventListenerList;

/**
 * Implementation of a LabelFigure for use with a DivaPlot.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotLabelFigure
    extends LabelFigure
    implements PlotFigure
{
    //  Constructors from LabelFigure

    public PlotLabelFigure()
    {
        super();
        fireCreated();
    }

    public PlotLabelFigure( String s )
    {
        super( s );
        fireCreated();
    }

    public PlotLabelFigure( String s, String face, int style, int size )
    {
        super( s, face, style, size );
        fireCreated();
    }

    public PlotLabelFigure( String s, Font f )
    {
        super( s, f );
        fireCreated();
    }

    public PlotLabelFigure( String s, Font font, double padding, int anchor )
    {
        super( s, font, padding, anchor );
        fireCreated();
    }

    public void transform( AffineTransform at )
    {
        super.transform( at );
        fireChanged();
    }

    public void translate( double x, double y )
    {
        super.translate( x, y );
        fireChanged();
    }

    // Part of PlotFigure interface, not implemented.
    public void setShape( Shape shape )
    {
        //  Do nothing.
    }

    //
    //  Transform freely interface.
    //

    /**
     * Hint that figures should ignore any transformation constraints
     * (to match a resize of Plot).
     */
    protected static boolean transformFreely = false;

    public void setTransformFreely( boolean state )
    {
        transformFreely = state;
    }

    /**
     * Find out if this is an occasion when a figure should give up
     * any constraints and transform freely.
     */
    public static boolean isTransformFreely()
    {
        return transformFreely;
    }

    //
    //  FigureListener events.
    //
    protected EventListenerList listeners = new EventListenerList();

    /**
     *  Registers a listener for to be informed when figure changes
     *  occur.
     *
     *  @param l the FigureListener
     */
    public void addListener( FigureListener l )
    {
        listeners.add( FigureListener.class, l );
    }

    /**
     * Remove a listener.
     *
     * @param l the FigureListener
     */
    public void removeListener( FigureListener l )
    {
        listeners.remove( FigureListener.class, l );
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has created to all listeners.
     */
    protected void fireCreated()
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this,
                                                FigureChangedEvent.CREATED );
                }
                ((FigureListener)list[i+1]).figureCreated( e );
            }
        }
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has been removed.
     */
    protected void fireRemoved()
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this,
                                                FigureChangedEvent.REMOVED );
                }
                ((FigureListener)list[i+1]).figureRemoved( e );
            }
        }
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has changed.
     */
    protected void fireChanged()
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this,
                                                FigureChangedEvent.CHANGED );
                }
                ((FigureListener)list[i+1]).figureChanged( e );
            }
        }
    }
}
