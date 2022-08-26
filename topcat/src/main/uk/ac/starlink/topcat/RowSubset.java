package uk.ac.starlink.topcat;

/**
 * Defines a selection of rows in a table model.
 * 
 * @author   Mark Taylor (Starlink)
 */
public abstract class RowSubset {

    private String name_;
    private Key key_;

    /**
     * A subset containing all rows (<tt>isIncluded</tt> always true).
     */
    public static RowSubset ALL = new RowSubset( "All" ) {
        public boolean isIncluded( long lrow ) {
            return true;
        }
    };

    /**
     * A subset containing no rows (<tt>isIncluded</tt> always false).
     */
    public static RowSubset NONE = new RowSubset( "None" ) {
        public boolean isIncluded( long lrow ) {
            return false;
        }
    };

    /**
     * Constructor.
     *
     * @param   name  subset name
     */
    public RowSubset( String name ) {
        name_ = name;
        key_ = new Key( TopcatUtils.identityString( this ) );
    }

    /**
     * Returns the name of this subset.
     *
     * @return name
     */
    public String getName() {
        return name_;
    }

    /**
     * Sets the name of this subset.
     *
     * @param  name  new name
     */
    public void setName( String name ) {
        name_ = name;
    }

    /**
     * Returns the key identifying this subset.
     * This value is intended for use by the GUI; only one subset in use
     * may have the same key at any one time, but if a subset goes out of use,
     * its key may be passed on to a different one that is intended as
     * a replacement that should inherit configuration set up for the
     * original owner.
     *
     * @return   identifer
     */
    public Key getKey() {
        return key_;
    }

    /**
     * Sets the key identifying this subset.
     * A key no longer in use may be passed on to a new subset intended
     * as its replacement.
     *
     * @param  key  new key
     */
    public void setKey( Key key ) {
        key_ = key;
    }

    /**
     * Indicates whether a given row is in the subset or not.
     *
     * @param  lrow  the index of the row in question
     * @return  <tt>true</tt> iff row <tt>lrow</tt> is to be included
     */
    public abstract boolean isIncluded( long lrow );

    /**
     * Returns this subset's name.
     */
    public String toString() {
        return getName();
    }

    /**
     * Class used as subset identifier.
     */
    public static class Key {

        private final String key_;

        /**
         * Constructor.
         *
         * @param  key  key text
         */
        private Key( String key ) {
            key_ = key;
        }

        @Override
        public int hashCode() {
            return key_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof Key && ((Key) o).key_.equals( this.key_ );
        }
    }
}
