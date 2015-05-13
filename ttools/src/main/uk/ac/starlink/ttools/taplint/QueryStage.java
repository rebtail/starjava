package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.vo.AdqlSyntax;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapLanguage;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage which performs some ADQL queries on data tables.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2011
 */
public class QueryStage implements Stage {

    private final VotLintTapRunner tapRunner_;
    private final MetadataHolder metaHolder_;
    private final CapabilityHolder capHolder_;

    /**
     * Constructor.
     *
     * @param  tapRunner    object that can run TAP queries
     * @param  metaHolder   provides table metadata at run time
     * @param  capHolder    provides capability info at run time;
     *                      may be null if capability-related queries
     *                      are not required
     */
    public QueryStage( VotLintTapRunner tapRunner, MetadataHolder metaHolder,
                       CapabilityHolder capHolder ) {
        tapRunner_ = tapRunner;
        metaHolder_ = metaHolder;
        capHolder_ = capHolder;
    }

    public String getDescription() {
        return "Make ADQL queries in " + tapRunner_.getDescription()
             + " mode";
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        SchemaMeta[] smetas = metaHolder_.getTableMetadata();
        String[] adqlLangs = capHolder_ == null
                           ? null
                           : getAdqlLanguages( capHolder_ );
        if ( smetas == null || smetas.length == 0 ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata available "
                           + "(earlier stages failed/skipped?) "
                           + "- will not run test queries" );
            return;
        }
        List<TableMeta> allTables = new ArrayList<TableMeta>();
        List<TableMeta> dataTables = new ArrayList<TableMeta>();
        for ( SchemaMeta smeta : smetas ) {
            for ( TableMeta tmeta : smeta.getTables() ) {
                allTables.add( tmeta );
                if ( ! tmeta.getName().toUpperCase()
                                      .startsWith( "TAP_SCHEMA." ) ) {
                    dataTables.add( tmeta ); 
                }
            }
        }
        if ( allTables.size() == 0 ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata available "
                           + "(earlier stages failed/skipped?) "
                           + "- will not run test queries" );
            return;
        }
        TableMeta[] tmetas =
            ( dataTables.size() > 0 ? dataTables : allTables )
           .toArray( new TableMeta[ 0 ] );
        new Querier( reporter, serviceUrl, tmetas, adqlLangs ).run();
        runDuffQuery( reporter, serviceUrl );
        tapRunner_.reportSummary( reporter );
    }

    /**
     * Returns all the known ADQL variants for a given capabilities element.
     *
     * @param  capHolder   capabilities metadata info supplier
     * @return  array of ADQL* language specifiers
     */
    private static String[] getAdqlLanguages( CapabilityHolder capHolder ) {
        TapCapability tcap = capHolder.getCapability();
        if ( tcap == null ) {
            return new String[] { "ADQL", "ADQL-2.0" };  // questionable?
        }
        List<String> adqlLangList = new ArrayList<String>();
        TapLanguage[] languages = tcap.getLanguages();
        for ( int il = 0; il < languages.length; il++ ) {
            TapLanguage lang = languages[ il ];
            if ( "ADQL".equals( lang.getName() ) ) {
                String[] versions = lang.getVersions();
                for ( int iv = 0; iv < versions.length; iv++ ) {
                    String version = versions[ iv ];
                    adqlLangList.add( version == null ? "ADQL"
                                                      : "ADQL-" + version );
                }
            }
        }
        if ( ! adqlLangList.contains( "ADQL" ) ) {
            adqlLangList.add( "ADQL" );
        }
        return adqlLangList.toArray( new String[ 0 ] );
    }

    /**
     * Runs a query with an intentionally nonsensical query string,
     * performing checks on whether the service issues error reports
     * in the correct way.
     *
     * @param  reporter  validation message destination
     * @param  serviceUrl   URL of TAP service
     */
    private void runDuffQuery( Reporter reporter, URL serviceUrl ) {
        String duffAdql = "DUFF QUERY";
        reporter.report( FixedCode.I_DUFF,
                         "Submitting duff query: " + duffAdql );
        VOElement resultsEl;
        try {
            TapQuery tq = new TapQuery( serviceUrl, duffAdql, null );
            InputStream in = tapRunner_.readResultInputStream( reporter, tq );
            VODocument doc = tapRunner_.readResultDocument( reporter, in );
            resultsEl = tapRunner_.getResultsResourceElement( reporter, doc );
        }
        catch ( SAXException e ) {
            reporter.report( FixedCode.E_DFSF,
                             "TAP result parse failed for \""
                           + duffAdql + "\"", e );
            return;
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_DFIO,
                             "TAP job failed for duff query", e );
            return;
        }
        VOElement statusInfo = null;
        for ( Node node = resultsEl.getFirstChild(); node != null;
              node = node.getNextSibling() ) {
            if ( node instanceof VOElement ) {
                VOElement el = (VOElement) node;
                String name = el.getVOTagName();
                boolean isStatusInfo =
                    "INFO".equals( name ) &&
                    "QUERY_STATUS".equals( el.getAttribute( "name" ) );
                boolean isTable =
                    "TABLE".equals( name );
                if ( isStatusInfo ) {
                    if ( statusInfo != null ) {
                        reporter.report( FixedCode.E_EST1,
                                         "Multiple INFOs with "
                                       + "name='QUERY_STATUS'" );
                    }
                    statusInfo = el;
                }
                if ( isTable ) {
                    reporter.report( FixedCode.W_HSTB,
                                     "Return from duff query contains TABLE" );
                }
            }
        }
        if ( statusInfo == null ) {
            reporter.report( FixedCode.E_DNST,
                             "Missing <INFO name='QUERY_STATUS'> element "
                           + "for duff query" );
        }
        else {
            String status = statusInfo.getAttribute( "value" );
            if ( "ERROR".equals( status ) ) {

                /* Error (presumably parse error) reported as expected
                 * for duff query. */
                String err = DOMUtils.getTextContent( statusInfo );
                if ( err == null || err.trim().length() == 0 ) {
                    reporter.report( FixedCode.W_NOMS,
                                     "<INFO name='QUERY_STATUS' "
                                   + "value='ERROR'> "
                                   + "element has no message content" );
                }
            }
            else if ( "OK".equals( status ) ) {
                reporter.report( FixedCode.W_DSUC,
                                 "Service reports OK from duff query" );
            }
            else {
                String msg = new StringBuffer()
                    .append( "QUERY_STATUS INFO has unknown value " )
                    .append( status )
                    .append( " is not OK/ERROR" )
                    .toString();
                reporter.report( FixedCode.E_DQUS, msg );
            }
        }
    }

    /**
     * Utility class to perform and check TAP queries.
     */
    private class Querier implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final String[] adqlLangs_;
        private final TableMeta tmeta1_;
        private final TableMeta tmeta2_;
        private final TableMeta tmeta3_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  serviceUrl  TAP service URL
         * @param  tmetas  metadata for known tables
         */
        Querier( Reporter reporter, URL serviceUrl, TableMeta[] tmetas,
                 String[] adqlLangs ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            adqlLangs_ = adqlLangs;

            /* Pick some representative tables for later testing.
             * Do it by choosing tables with column counts near the median. */
            tmetas = (TableMeta[]) tmetas.clone();
            Arrays.sort( tmetas, new Comparator<TableMeta>() {
                public int compare( TableMeta tm1, TableMeta tm2 ) {
                    return tm1.getColumns().length - tm2.getColumns().length;
                }
            } );
            int imed = tmetas.length / 2;
            tmeta1_ = tmetas[ imed ];
            tmeta2_ = tmetas[ Math.min( imed + 1, tmetas.length - 1 ) ];
            tmeta3_ = tmetas[ Math.min( imed + 2, tmetas.length - 1 ) ];
        }

        /**
         * Invoked to perform queries.
         */
        public void run() {
            runOneColumn( tmeta1_ );
            runSomeColumns( tmeta2_ );
            runJustMeta( tmeta3_ );
        }

        /**
         * Runs some tests using all the columns in an example table.
         *
         * @param  tmeta  table to test
         */
        private void runOneColumn( TableMeta tmeta ) {
            if ( ! checkHasColumns( tmeta ) ) {
                return;
            }
            final int nr0 = 10;
            String tname = tmeta.getName();

            /* Prepare an array of column specifiers corresponding to
             * the first single column in the table. */
            ColumnMeta cmeta1 = tmeta.getColumns()[ 0 ];
            ColSpec cspec1 = new ColSpec( cmeta1 );

            /* Check that limiting using TOP works. */
            String tAdql = new StringBuilder()
               .append( "SELECT TOP " )
               .append( nr0 )
               .append( " " )
               .append( cspec1.getQueryText() )
               .append( " FROM " )
               .append( tname )
               .toString();
            StarTable t1 =
                runCheckedQuery( tAdql, -1, new ColSpec[] { cspec1 }, nr0 );
            long nr1 = t1 != null ? t1.getRowCount() : 0;
            assert nr1 >= 0;

            /* Check that limiting using MAXREC works. */
            if ( nr1 > 0 ) {
                int nr2 = Math.min( (int) nr1 - 1, nr0 - 1 );
                String mAdql = new StringBuilder()
                   .append( "SELECT " )
                   .append( cspec1.getQueryText() )
                   .append( " FROM " )
                   .append( tname )
                   .toString();
                StarTable t2 =
                    runCheckedQuery( mAdql, nr2, new ColSpec[] { cspec1 }, -1 );
                if ( t2 != null && ! tapRunner_.isOverflow( t2 ) ) {
                    String msg = "Overflow not marked - no "
                               + "<INFO name='QUERY_STATUS' value='OVERFLOW'/> "
                               + "after TABLE";
                    reporter_.report( FixedCode.E_OVNO, msg );
                }
            }

            /* Check that all known variants (typically "ADQL" and "ADQL-2.0")
             * work the same. */
            if ( adqlLangs_ != null && adqlLangs_.length > 1 ) {
                List<String> okLangList = new ArrayList<String>();
                List<String> failLangList = new ArrayList<String>();
                for ( int il = 0; il < adqlLangs_.length; il++ ) {
                    String lang = adqlLangs_[ il ];
                    String vAdql = new StringBuilder()
                       .append( "SELECT TOP 1 " )
                       .append( cspec1.getQueryText() )
                       .append( " FROM " )
                       .append( tname )
                       .toString();
                    Map<String,String> extraParams =
                        new HashMap<String,String>();
                    extraParams.put( "LANG", lang );
                    TapQuery tq =
                        new TapQuery( serviceUrl_, vAdql, extraParams );
                    StarTable result =
                        tapRunner_.getResultTable( reporter_, tq );
                    ( result == null ? failLangList
                                     : okLangList ).add( lang );
                }
                if ( ! failLangList.isEmpty() && ! okLangList.isEmpty() ) {
                    String msg = new StringBuilder()
                       .append( "Some ADQL language variants fail: " )
                       .append( okLangList )
                       .append( " works, but " )
                       .append( failLangList )
                       .append( " doesn't" )
                       .toString();
                    reporter_.report( FixedCode.E_LVER, msg );
                }
            }
        }

        /**
         * Runs some tests using a selection of the columns in an example
         * table.
         *
         * @param  tmeta  table to run tests on
         */
        private void runSomeColumns( TableMeta tmeta ) {
            if ( ! checkHasColumns( tmeta ) ) {
                return;
            }

            /* Assemble column specifiers for the query. */
            String talias = tmeta.getName().substring( 0, 1 );
            if ( ! AdqlSyntax.getInstance().isIdentifier( talias ) ) {
                talias = "t";
            }
            List<ColSpec> clist = new ArrayList<ColSpec>();
            int ncol = tmeta.getColumns().length;
            int step = Math.max( 1, ncol / 11 );
            int ix = 0;
            for ( int icol = ncol - 1; icol >= 0; icol -= step ) {
                ColSpec cspec = new ColSpec( tmeta.getColumns()[ icol ] );
                if ( ix % 2 == 1 ) {
                    cspec.setRename( "taplint_c_" + ( ix + 1 ) );
                }
                if ( ix % 3 == 2 ) {
                    cspec.setTableAlias( talias );
                }
                clist.add( cspec );
                ix++;
            }
            ColSpec[] cspecs = clist.toArray( new ColSpec[ 0 ] );

            /* Assemble the ADQL text. */
            int nr = 8;
            StringBuffer abuf = new StringBuffer();
            abuf.append( "SELECT " )
                .append( "TOP " )
                .append( nr )
                .append( " " );
            for ( Iterator<ColSpec> colIt = clist.iterator();
                  colIt.hasNext(); ) {
                ColSpec col = colIt.next();
                abuf.append( col.getQueryText() );
                if ( colIt.hasNext() ) {
                    abuf.append( "," );
                }
                abuf.append( " " );
            }
            abuf.append( " FROM " )
                .append( tmeta.getName() )
                .append( " AS " )
                .append( talias );
            String adql = abuf.toString();

            /* Run the query. */
            runCheckedQuery( adql, -1, cspecs, nr );
        }

        /**
         * Test that a query with MAXREC=0 works (it should return metadata).
         *
         * @param  tmeta  table to run tests on
         */
        private void runJustMeta( TableMeta tmeta ) {
            if ( ! checkHasColumns( tmeta ) ) {
                return;
            }

            /* Run test. */
            ColSpec cspec = new ColSpec( tmeta.getColumns()[ 0 ] );
            String adql = new StringBuffer()
                .append( "SELECT " )
                .append( cspec.getQueryText() )
                .append( " FROM " )
                .append( tmeta.getName() )
                .toString();
            runCheckedQuery( adql, 0, new ColSpec[] { cspec }, -1 );
        }

        /**
         * Executes a TAP query and checks the results.
         *
         * @param  adql  query string
         * @param  maxrec  value of MAXREC limit; if negative none applied
         * @param  colSpecs  expected column metadata of result
         * @param  maxrow  expected maximum row count, different from MAXREC;
         *                 if negative ignored
         */
        private StarTable runCheckedQuery( String adql, int maxrec,
                                           ColSpec[] colSpecs, int maxrow ) {
            Map<String,String> extraParams = new HashMap<String,String>();
            if ( maxrec >= 0 ) {
                extraParams.put( "MAXREC", Integer.toString( maxrec ) );
            }
            TapQuery tq = new TapQuery( serviceUrl_, adql, extraParams );
            StarTable table = tapRunner_.getResultTable( reporter_, tq );
            if ( table != null ) {
                int nrow = (int) table.getRowCount();
                if ( maxrec >=0 && nrow > maxrec ) {
                    String msg = new StringBuffer()
                       .append( "More than MAXREC rows returned (" )
                       .append( nrow )
                       .append( " > " )
                       .append( maxrec )
                       .append( ")" )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( FixedCode.E_NREC, msg );
                }
                if ( maxrow >= 0 && nrow > maxrow ) {
                    String msg = new StringBuffer()
                       .append( "More rows than expected (" )
                       .append( nrow )
                       .append( ") returned" )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( FixedCode.E_NROW, msg );
                }
                checkMeta( adql, colSpecs, table );
            }
            return table;
        }

        /**
         * Checks that at least one column exists in a table metadata item.
         * If not, a FAILURE report is made and false is returned.
         *
         * @param   tmeta  table metadata object
         * @return  true iff tmeta has at least one column
         */
        private boolean checkHasColumns( TableMeta tmeta ) {
            boolean hasColumns = tmeta.getColumns().length > 0;
            if ( ! hasColumns ) {
                reporter_.report( FixedCode.F_ZCOL,
                                  "No columns known for table "
                                + tmeta.getName() );
       
            }
            return hasColumns;
        }

        /**
         * Checks metadata from a TAP result matches what's expected.
         *
         * @param  adql  text of query
         * @param  colSpecs   metadata of expected columns in result
         * @param  table  actual result table
         */
        private void checkMeta( String adql, ColSpec[] colSpecs,
                                StarTable table ) {

            /* Check column counts match. */
            int qCount = colSpecs.length;
            int rCount = table.getColumnCount();
            if ( qCount != rCount ) {
                String msg = new StringBuffer()
                   .append( "Query/result column count mismatch; " )
                   .append( qCount )
                   .append( " != " )
                   .append( rCount )
                   .append( " for " )
                   .append( adql )
                   .toString();
                reporter_.report( FixedCode.E_MCOL, msg );
                return;
            }
            int ncol = qCount;
            assert ncol == rCount;

            /* Check column names match. */
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColSpec cspec = colSpecs[ ic ];
                ColumnInfo cinfo = table.getColumnInfo( ic );
                String qName = cspec.getResultName();
                String rName = cinfo.getName();
                if ( ! qName.equalsIgnoreCase( rName ) ) {
                    String msg = new StringBuffer()
                       .append( "Query/result column name mismatch " )
                       .append( "for column #" )
                       .append( ic )
                       .append( "; " )
                       .append( qName )
                       .append( " != " )
                       .append( rName )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( FixedCode.E_CNAM, msg );
                }
                String columnId = rName.equalsIgnoreCase( qName )
                                ? qName
                                : "#" + ic;

                /* Check column types match. */
                String qType = cspec.getColumnMeta().getDataType();
                String rType =
                    (String) cinfo.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                                     String.class );
                if ( ! CompareMetadataStage
                      .compatibleDataTypes( qType, rType ) ) {
                    String msg = new StringBuffer()
                       .append( "Query/result column type mismatch " )
                       .append( "for column " )
                       .append( columnId )
                       .append( "; " )
                       .append( qType )
                       .append( " vs. " )
                       .append( rType )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( FixedCode.E_QTYP, msg );
                }
            }
        }
    }

    /**
     * Specifies a column for an ADQL query.
     */
    private static class ColSpec {
        private final ColumnMeta cmeta_;
        private String talias_;
        private String rename_;

        /**
         * Constructor.
         *
         * @param   cmeta  column metadata on which this queried column is based
         */
        ColSpec( ColumnMeta cmeta ) {
            cmeta_ = cmeta;
        }

        /**
         * Returns the column metadata object this column is based on.
         *
         * @return  col meta
         */
        ColumnMeta getColumnMeta() {
            return cmeta_;
        }

        /**
         * Sets an optional table alias by which the table this column
         * comes from is known.
         *
         * @param  talias  table alias, may be null
         */
        public void setTableAlias( String talias ) {
            talias_ = talias;
        }

        /**
         * Sets an optional changed name by which this column will be queried
         * as.  If null, the name will be taken from the column metadata.
         *
         * @param  rename  column alias, or null
         */
        public void setRename( String rename ) {
            rename_ = rename;
        }

        /**
         * Returns the ADQL text for the column list in the SELECT statement
         * corresponding to this column.
         *
         * @return  query column specifier
         */
        String getQueryText() {
            StringBuffer sbuf = new StringBuffer();
            if ( talias_ != null ) {
                sbuf.append( talias_ )
                    .append( "." );
            }
            sbuf.append( cmeta_.getName() );
            if ( rename_ != null ) {
                sbuf.append( " AS " )
                    .append( rename_ );
            }
            return sbuf.toString();
        }

        /**
         * Returns the name of the column that should be returned from the
         * query for this column.
         *
         * @return   result column name
         */
        String getResultName() {
            return rename_ == null ? cmeta_.getName() : rename_;
        }
    }
}
