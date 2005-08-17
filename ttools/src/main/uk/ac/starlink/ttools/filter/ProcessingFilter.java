package uk.ac.starlink.ttools.filter;

import java.util.Iterator;

/**
 * Defines a type of filter-like processing which can be done on a StarTable.
 * An object in this class serves as a factory for
 * {@link ProcessingStep} instances, based on a list of command-line
 * arguments.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public interface ProcessingFilter {

    /**
     * Usage message for this filter.  This should contain any arguments
     * which apply to this item; the name itself should not be included.
     *
     * @return  usage string
     */
    String getUsage();

    /**
     * Creates a new ProcessingStep based on a sequence of command-line
     * arguments.  The <tt>argIt</tt> argument is an iterator over the
     * command-line arguments positioned just before any arguments
     * intended for this filter.  If legal, any that can be comprehended
     * by this filter should be read (iterated over) and removed,
     * and a <tt>ProcessingStep</tt> should accordingly be returned.
     * In the case of a successful return, it is essential
     * that no arguments other than the ones intended for this
     * filter are read from the iterator.
     *
     * <p>If the argument list is badly-formed as far as this filter is
     * concerned, an {@link ArgException} should be thrown.
     * If its <code>usageFrament</code> is blank, it will be filled in
     * later using this mode's usage text.
     *
     * @param  argIt  iterator over command-line arguments positioned
     *         just after the -getName() flag
     */
    ProcessingStep createStep( Iterator argIt ) throws ArgException;
}
