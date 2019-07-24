package uk.ac.starlink.util;

/**
 * Extendable array of longs..
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class LongList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public LongList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public LongList( int size ) {
        super( new long[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public LongList( long[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */ 
    public long get( int i ) {
        checkIndex( i );
        return ((long[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, long value ) {
        checkIndex( i );
        ((long[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( long value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Appends all the elements of a second list to this list.
     *
     * @param  other  other list
     * @return   true iff this collection changed as a result of the call
     */
    public boolean addAll( LongList other ) {
        return super.addAll( other );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public long[] toLongArray() {
        return (long[]) toArray();
    }
}
