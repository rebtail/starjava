package uk.ac.starlink.topcat;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.task.StiltsCommand;

/**
 * Reports the STILTS command generated by the current state of a
 * supplied StiltsReporter component.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2024
 */
public class StiltsAction extends BasicAction {

    private final StiltsReporter reporter_;
    private final Supplier<Window> parentSupplier_;

    /**
     * Constructor.
     *
     * @param   reporter   component that can generate stilts command lines
     * @param   parentSupplier  provides a parent window for this action
     */
    public StiltsAction( StiltsReporter reporter,
                         Supplier<Window> parentSupplier ) {
        super( "STILTS", ResourceIcon.STILTS,
               "Show STILTS command corresponding to current window" );
        reporter_ = reporter;
        parentSupplier_ = parentSupplier;
    }

    public void actionPerformed( ActionEvent evt ) {
        StiltsCommand cmd =
            reporter_.createStiltsCommand( TopcatTableNamer.PATHNAME_NAMER );
        System.out.println( cmd );
    }
}
