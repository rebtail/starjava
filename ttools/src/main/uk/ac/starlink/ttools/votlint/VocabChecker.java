package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.vo.VocabTerm;
import uk.ac.starlink.vo.Vocabulary;

/**
 * Checks an attribute that is defined by the content of an IVOA Vocabulary.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2019
 */
public class VocabChecker implements AttributeChecker {

    private final URL vocabUrl_;
    private final Collection<String> fixedTerms_;
    private Map<String,VocabTerm> retrievedTerms_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.votlint" );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/timescale. */
    public static final VocabChecker TIMESCALE =
        new VocabChecker( "http://www.ivoa.net/rdf/timescale",
                          new String[] {
                              "TAI", "TT", "UT", "UTC", "GPS",
                              "TCG", "TCB", "TDB", "UNKNOWN", 
                          } );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/refposition. */
    public static final VocabChecker REFPOSITION =
        new VocabChecker( "http://www.ivoa.net/rdf/refposition",
                          new String[] {
                              "TOPOCENTER", "GEOCENTER", "BARYCENTER",
                              "HELIOCENTER", "EMBARYCENTER", "UNKNOWN",
                          } );

    /**
     * Constructor.
     *
     * @param   vocabUrl  URI/URL for vocabulary document
     * @param   fixedTerms    hard-coded non-preliminary, non-deprecated terms
     *                        known in the vocabulary;
     *                        other terms may be available by resolving
     *                        the vocabulary URL
     */
    private VocabChecker( String vocabUrl, String[] fixedTerms ) {
        try {
            vocabUrl_ = new URL( vocabUrl );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalArgumentException( "Not a URL: " + vocabUrl );
        }
        fixedTerms_ = new LinkedHashSet<String>( Arrays.asList( fixedTerms ) );
    }

    public void check( String nameValue, ElementHandler handler ) {
        VotLintContext context = handler.getContext();

        /* Note that the online vocabulary document is only consulted
         * if encountered vocabulary terms are not present in the
         * hard-coded list. */
        if ( ! fixedTerms_.contains( nameValue ) ) {
            VocabTerm term = getRetrievedTerms().get( nameValue );
            if ( term == null ) {
                StringBuffer sbuf = new StringBuffer()
                    .append( "\"" )
                    .append( nameValue )
                    .append( "\"" )
                    .append( " not known in vocabulary " )
                    .append( vocabUrl_ )
                    .append( " (known:" );
                Set<String> terms = new TreeSet<String>();
                terms.addAll( fixedTerms_ );
                terms.addAll( getRetrievedTerms().keySet() );
                for ( Iterator<String> it = terms.iterator(); it.hasNext(); ) {
                    sbuf.append( " " )
                        .append( it.next() );
                    if ( it.hasNext() ) {
                        sbuf.append( "," );
                    }
                }
                sbuf.append( ")" );
                context.warning( new VotLintCode( "VCU" ), sbuf.toString() );
            }
            else if ( term.isDeprecated() ) {
                String msg = new StringBuffer()
                   .append( "\"" )
                   .append( nameValue )
                   .append( "\"" )
                   .append( " is marked *deprecated* in vocabulary " )
                   .append( vocabUrl_ )
                   .toString();
                context.warning( new VotLintCode( "VCD" ), msg );
            }
            else if ( term.isPreliminary() ) {
                String msg = new StringBuffer()
                   .append( "\"" )
                   .append( nameValue )
                   .append( "\"" )
                   .append( " is marked *preliminary* in vocabulary " )
                   .append( vocabUrl_ )
                   .toString();
                context.info( new VotLintCode( "VCP" ), msg );
            }
        }
    }

    /**
     * Lazily acquires vocabulary values by reading the resource at the
     * vocabulary URI.
     *
     * @return   term map retrieved from online vocabulary;
     *           in case of a read error this may be empty, but not null
     */
    public Map<String,VocabTerm> getRetrievedTerms() {
        if ( retrievedTerms_ == null ) {
            Map<String,VocabTerm> terms;
            try {
                terms = Vocabulary.readVocabulary( vocabUrl_ ).getTerms();
                int nRead = terms.size();
                if ( nRead > 0 ) {
                    terms = new LinkedHashMap<>( terms );
                    terms.keySet().removeAll( fixedTerms_ );
                    int nNew = terms.size();
                    String msg = new StringBuffer()
                        .append( "Read vocabulary from " )
                        .append( vocabUrl_ )
                        .append( ": " )
                        .append( nRead )
                        .append( " terms, " )
                        .append( nNew )
                        .append( " unknown" )
                        .toString();
                    logger_.info( msg );
                }
                else {
                    logger_.warning( "No terms read from vocabulary at "
                                   + vocabUrl_ );
                }
            }
            catch ( IOException e ) {
                terms = Collections.emptyMap();
                logger_.log( Level.WARNING,
                             "Unable to read vocabulary from " + vocabUrl_, e );
            }
            retrievedTerms_ = Collections.unmodifiableMap( terms );
        }
        return retrievedTerms_;
    }
}
