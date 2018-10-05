package uk.ac.starlink.vo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for working with TapService instances.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2016
 */
public class TapServices {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** GAVO RegTAP service is pretty reliable. */
    private static final TapService REGTAP =
        initDefaultTapService( RegTapRegistryQuery.GAVO_REG );

    /**
     * Returns a default service  corresponding to a Relational Registry
     * (RegTAP) service.
     *
     * <p>The current implementation returns a hardcoded value,
     * the main GAVO registry service.  Perhaps it should be pluggable,
     * but the GAVO RegTAP service is expected to be pretty reliable.
     *
     * @return  default RegTAP service
     */
    public static TapService getRegTapService() {
        return REGTAP;
    }

    /**
     * Creates a TAP service from a string giving the base URL,
     * with the endpoints in the default places.
     *
     * @param  baseUrl   base TAP URL
     * @return   service using standard (v1.0) TAP endpoints
     * @throws   IllegalArgumentException  in case of a bad URL
     */
    public static TapService createDefaultTapService( String baseUrl ) {
        try {
            return createDefaultTapService( new URL( baseUrl ) );
        }
        catch ( MalformedURLException e ) {
            throw (RuntimeException)
                  new IllegalArgumentException( "Not a URL: " + baseUrl )
                 .initCause( e );
        }
    }

    /**
     * Creates a TAP service from a string giving the base URL,
     * with the endpoints in the default places.
     *
     * <p>This setup is more or less mandatory for
     * TAP 1.0 services, but TAP 1.1 services can provide different
     * sets of endpoints (capability/interface elements) for different
     * purposes, for instance with different securityMethods.
     *
     * @param  baseUrl   base TAP URL
     * @return   TAP service with standard (v1.0) endpoints
     */
    public static TapService createDefaultTapService( URL baseUrl ) {
        final String identity = baseUrl.toString();
        final URL sync = appendPath( baseUrl, "/sync" );
        final URL async = appendPath( baseUrl, "/async" );
        final URL tables = appendPath( baseUrl, "/tables" );
        final URL capabilities = appendPath( baseUrl, "/capabilities" );
        final URL availability = appendPath( baseUrl, "/availability" );
        final URL examples = appendPath( baseUrl, "/examples" );
        return new TapService() {
            public String getIdentity() {
                return identity;
            }
            public URL getSyncEndpoint() {
                return sync;
            }
            public URL getAsyncEndpoint() {
                return async;
            }
            public URL getTablesEndpoint() {
                return tables;
            }
            public URL getCapabilitiesEndpoint() {
                return capabilities;
            }
            public URL getAvailabilityEndpoint() {
                return availability;
            }
            public URL getExamplesEndpoint() {
                return examples;
            }
            public TapVersion getTapVersion() {
                return TapVersion.V10;
            }
        };
    }

    /**
     * Adds a sub-path to an existing URL.
     *
     * @param  base  base URL
     * @return  subPath   text to append to base
     * @return   concatenation
     */
    private static URL appendPath( URL base, String subPath ) {
        try {
            return new URL( base.toString() + subPath );
        }
        catch ( MalformedURLException e ) {
            assert false;
            throw (RuntimeException)
                  new IllegalArgumentException( "Bad URL???" )
                 .initCause( e );
        }
    }

    /**
     * Initialises a default TAP service.
     * If the URL is bad, a warning is logged, and null is returned.
     *
     * @param   baseUrl   base TAP URL
     * @return   service with standard (v1.0) TAP endpoints
     */
    private static TapService initDefaultTapService( String baseUrl ) {
        try {
            return createDefaultTapService( new URL( baseUrl ) );
        }
        catch ( MalformedURLException e ) {
            logger_.log( Level.WARNING,
                         "Failed to get endpoints for TAP service: " + baseUrl,
                         e );
            return null;
        }
    }
}