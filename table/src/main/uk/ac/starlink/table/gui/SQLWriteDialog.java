package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCFormatter;
import uk.ac.starlink.table.jdbc.WriteMode;

/**
 * A popup dialog for querying the user about the location of a new
 * JDBC table to write.
 */
public class SQLWriteDialog extends SQLDialog implements TableSaveDialog {

    private JDialog dialog_; 
    private JComboBox modeSelector_;
    private static Icon icon_;

    /**
     * Constructs a new SQLWriteDialog.
     */
    public SQLWriteDialog() {
        super( "Write New SQL Table" );
        modeSelector_ = new JComboBox( WriteMode.getAllModes() );
        modeSelector_.setSelectedItem( WriteMode.CREATE );
        getStack().addLine( "Write Mode", null, modeSelector_ );
    }

    public String getName() {
        return "SQL Table";
    }

    public String getDescription() {
        return "Write table as a new table in an SQL relational database";
    }

    public Icon getIcon() {
        if ( icon_ == null ) {
            icon_ = new ImageIcon( getClass().getResource( "sqlread.gif" ) );
        }
        return icon_;
    }

    public boolean showSaveDialog( Component parent, StarTableOutput sto,
                                   ComboBoxModel formatModel,
                                   StarTable table ) {
        useAuthenticator( sto.getJDBCHandler().getAuthenticator() );
        JDialog dialog = createDialog( parent, "Write New SQL Table" );
        final boolean[] done = new boolean[ 1 ];
        while ( ! done[ 0 ] ) {
            dialog.setVisible( true );
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                SaveWorker worker = new SaveWorker( parent, table, getRef() ) {
                    public void attemptSave( StarTable table )
                            throws IOException {
                        WriteMode mode =
                            (WriteMode) modeSelector_.getSelectedItem();
                        Connection conn = null;
                        try {
                            conn = getConnector().getConnection();
                            new JDBCFormatter( conn, table )
                               .createJDBCTable( getRef(), mode );
                        }
                        catch ( SQLException e ) {
                            throw (IOException) 
                                  new IOException( e.getMessage() )
                                 .initCause( e );
                        }
                        finally {
                            if ( conn != null ) {
                                try {
                                    conn.close();
                                }
                                catch ( SQLException e ) {
                                    // never mind
                                }
                            }
                        }
                    }
                    public void done( boolean success ) {
                        done[ 0 ] = success;
                    }
                };
                setEnabled( false );
                worker.invoke();
                setEnabled( true );
            }
            else {
                return false;
            }
        }
        return true;
    }
}
