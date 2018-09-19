package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TypedListModel;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * Layer plot window for 2D Cartesian plots.
 *
 * @author   Mark Taylor
 * @since    19 Mar 2013
 */
public class PlanePlotWindow
             extends StackPlotWindow<PlaneSurfaceFactory.Profile,PlaneAspect> {
    private static final PlanePlotType PLOT_TYPE = PlanePlotType.getInstance();
    private static final PlanePlotTypeGui PLOT_GUI = new PlanePlotTypeGui();

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  tablesModel  list of available tables
     */
    public PlanePlotWindow( Component parent,
                            TypedListModel<TopcatModel> tablesModel ) {
        super( "Plane Plot", parent, PLOT_TYPE, PLOT_GUI, tablesModel );
        getToolBar().addSeparator();
        addHelp( "PlanePlotWindow" );
    }

    /**
     * Defines GUI features specific to plane plot.
     */
    private static class PlanePlotTypeGui
            implements PlotTypeGui<PlaneSurfaceFactory.Profile,PlaneAspect> {
        public AxisController<PlaneSurfaceFactory.Profile,PlaneAspect>
                createAxisController() {
            return new PlaneAxisController();
        }
        public PositionCoordPanel createPositionCoordPanel( int npos ) {
            return SimplePositionCoordPanel
                  .createPanel( PLOT_TYPE.getPointDataGeoms()[ 0 ], npos );
        }
        public boolean hasPositions() {
            return true;
        }
        public boolean isPlanar() {
            return true;
        }
        public GangerFactory getGangerFactory() {
            return SingleGanger.FACTORY;
        }
        public ZoneFactory createZoneFactory() {
            return ZoneFactories.FIXED;
        } 
        public String getNavigatorHelpId() {
            return "planeNavigation";
        }
    }
}
