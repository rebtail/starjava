package uk.ac.starlink.vo;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.ivoa.registry.search.ParseException;
import net.ivoa.registry.search.Where2DOM;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Can submit ADQL queries to a registry and return the result as a
 * list of resources.
 * This class uses custom SAX parsing of the SOAP response 
 * to ensure that even a large response (not uncommon)
 * can be processed without large client-side resource usage.
 *
 * @author   Mark Taylor
 * @see   <a href="http://www.ivoa.net/Documents/RegistryInterface/"
 *           >IVOA Registry Interfaces</a>
 */
public class RegistryClient {

    private static final String RI_NS = 
        "http://www.ivoa.net/xml/RegistryInterface/v1.0";
    private static final String XSI_NS =
        "http://www.w3.org/2001/XMLSchema-instance";
    private static final String ACTION_BASE =
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0";

    private static final String RESOURCE_PATH =
        "/SearchResponse/VOResources/Resource";
    private static final String TITLE_PATH =
        RESOURCE_PATH + "/title";
    private static final String IDENTIFIER_PATH =
        RESOURCE_PATH + "/identifier";
    private static final String SHORTNAME_PATH =
        RESOURCE_PATH + "/shortName";
    private static final String PUBLISHER_PATH =
        RESOURCE_PATH + "/curation/publisher";
    private static final String CONTACTNAME_PATH =
        RESOURCE_PATH + "/curation/contact/name";
    private static final String CONTACTEMAIL_PATH =
        RESOURCE_PATH + "/curation/contact/email";
    private static final String REFURL_PATH =
        RESOURCE_PATH + "/content/referenceURL";
    private static final String CAPABILITY_PATH =
        RESOURCE_PATH + "/capability";
    private static final String STDID_PATH =
        CAPABILITY_PATH + "/@standardID";
    private static final String XSITYPE_PATH =
        CAPABILITY_PATH + "/@xsi:type";
    private static final String DESCRIPTION_PATH =
        CAPABILITY_PATH + "/description";
    private static final String CAPINTERFACE_PATH =
        CAPABILITY_PATH + "/interface";
    private static final String ACCESSURL_PATH =
        CAPINTERFACE_PATH + "/accessURL";
    private static final String VERSION_PATH =
        CAPINTERFACE_PATH + "interface/@version";

    private final SoapClient soapClient_;

    /**
     * Constructs a RegistryClient given a SOAP endpoint.
     *
     * @param  endpoint   SOAP endpoint for RI-compliant registry service
     */
    RegistryClient( URL endpoint ) {
        this( new SoapClient( endpoint ) );
    }

    /**
     * Constructs a RegistryClient given a SOAP client.
     *
     * @param  soapClient  SOAP client which talks to an RI-compliant
     *         registry service
     */
    RegistryClient( SoapClient soapClient ) {
        soapClient_ = soapClient;
    }

    /**
     * Returns the SOAP endpoint this client talks to.
     *
     * @return  registry endpoint
     */
    public URL getEndpoint() {
        return soapClient_.getEndpoint();
    }

    /**
     * Returns a list of resources corresponding to a given ADQL/S query.
     *
     * @param  adqls  WHERE clause (minus WHERE) in ADQL specifying search
     * @return   array of resources
     */
    public RegResource[] searchAdql( String adqls ) throws IOException {
        String soapAction = ACTION_BASE + "#Search";
        String searchBody = createSearchBody( adqls );
        ResourceHandler resourceHandler = new ResourceHandler();
        soapClient_.execute( searchBody, soapAction, resourceHandler );
        return resourceHandler.getResources();
    }

    /**
     * Returns the SOAP body used for the registry query.
     *
     * @param  adqls  WHERE clause (minus WHERE) in ADQL specifying search
     * @return   SOAP &lt;rs:Search&gt; element in a String
     */
    protected String createSearchBody( String adqls )
            throws IOException {
        Where2DOM w2d = new Where2DOM( new StringReader( "where " + adqls ) );
        Element whereEl;
        try {
            whereEl = w2d.Where( null );
        }
        catch ( ParseException e ) {
           throw (IOException) new IOException( "ADQL Syntax Error" )
                               .initCause( e );
        }
        whereEl.setPrefix( "rs" );
        return new StringBuffer()
            .append( "<rs:Search " )
            .append( "xmlns:rs='http://www.ivoa.net/wsdl/RegistrySearch/v1.0'" )
            .append( ">" )
            .append( soapClient_.nodeToString( whereEl ) )
            .append( "</rs:Search>" )
            .toString();
    }

    /**
     * RegResource implementation used by this class.
     */
    private static class Resource implements RegResource {
        private final String title_;
        private final String identifier_;
        private final String shortName_;
        private final String publisher_;
        private final String contact_;
        private final String referenceUrl_;
        private final RegCapabilityInterface[] capabilities_;

        /**
         * Constructor.
         *
         * @param  rMap  map of resource values with XPath-like paths as keys
         * @param  capabilities  capabilities of this resource
         */
        private Resource( Map rMap, RegCapabilityInterface[] capabilities ) {
            capabilities_ = capabilities;
            title_ = (String) rMap.remove( TITLE_PATH );
            identifier_ = (String) rMap.remove( IDENTIFIER_PATH );
            shortName_ = (String) rMap.remove( SHORTNAME_PATH );
            publisher_ = (String) rMap.remove( PUBLISHER_PATH );
            contact_ = makeContact( (String) rMap.remove( CONTACTNAME_PATH ),
                                    (String) rMap.remove( CONTACTEMAIL_PATH ) );
            referenceUrl_ = (String) rMap.remove( REFURL_PATH );
            assert rMap.isEmpty();
        }

        public String getContact() {
            return contact_;
        }
        public String getIdentifier() {
            return identifier_;
        }
        public String getPublisher() {
            return publisher_;
        }
        public String getReferenceUrl() {
            return referenceUrl_;
        }
        public String getShortName() {
            return shortName_;
        }
        public String getTitle() {
            return title_;
        }
        public RegCapabilityInterface[] getCapabilities() {
            return capabilities_;
        }

        /**
         * Amalgamates a name and email address into a single string.
         *
         * @param  name  name
         * @param  email   email address
         * @return  combined contact string
         */
        private static String makeContact( String name, String email ) {
            if ( email != null && name != null ) {
                return name + " <" + email + ">";
            }
            else if ( email != null ) {
                return email;
            }
            else if ( name != null ) {
                return name;
            }
            else {
                return null;
            }
        }
    }

    /**
     * RegCapabilityInterface implementation used by this class.
     */
    private static class Capability implements RegCapabilityInterface {
        private final String accessUrl_;
        private final String description_;
        private final String standardId_;
        private final String version_;
        private final String xsiType_;

        /**
         * Constructor.
         *
         * @param  cMap  map of capability values with XPath-like paths as keys
         */
        public Capability( Map cMap ) {
            accessUrl_ = (String) cMap.remove( ACCESSURL_PATH );
            description_ = (String) cMap.remove( DESCRIPTION_PATH );
            standardId_ = (String) cMap.remove( STDID_PATH );
            version_ = (String) cMap.remove( VERSION_PATH );
            xsiType_ = (String) cMap.remove( XSITYPE_PATH );
            assert cMap.isEmpty();
        }

        public String getAccessUrl() {
            return accessUrl_;
        }
        public String getDescription() {
            return description_;
        }
        public String getStandardId() {
            return standardId_;
        }
        public String getVersion() {
            return version_;
        }
        public String getXsiType() {
            return xsiType_;
        }
    }

    /**
     * SAX ContentHandler which processes a SearchResponse registry response
     * to generate a list of RegResources.
     */
    private static class ResourceHandler extends DefaultHandler {
        private Map resourceMap_;
        private Map capabilityMap_;
        private StringBuffer txtBuf_;
        private List capabilityList_;
        private final List resourceList_;
        private final Set resourcePathSet_;
        private final Set capabilityPathSet_;
        private final StringBuffer path_;

        /**
         * Constructor.
         */
        ResourceHandler() {
            resourceList_ = new ArrayList();
            path_ = new StringBuffer();
            resourcePathSet_ = new HashSet( Arrays.asList( new String[] {
                TITLE_PATH,
                IDENTIFIER_PATH,
                SHORTNAME_PATH,
                PUBLISHER_PATH,
                CONTACTNAME_PATH,
                CONTACTEMAIL_PATH,
                REFURL_PATH,
            } ) );
            capabilityPathSet_ = new HashSet( Arrays.asList( new String[] {
                STDID_PATH,
                XSITYPE_PATH,
                DESCRIPTION_PATH,
                ACCESSURL_PATH,
                VERSION_PATH,
            } ) );
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            try {
                path_.append( '/' )
                     .append( localName );
                String path = path_.toString();
                if ( RESOURCE_PATH.equals( path ) ) {
                    resourceMap_ = new HashMap();
                    capabilityList_ = new ArrayList();
                }
                else if ( CAPABILITY_PATH.equals( path ) ) {
                    capabilityMap_ = new HashMap();
                }
                else if ( resourcePathSet_.contains( path ) ||
                          capabilityPathSet_.contains( path ) ) {
                    txtBuf_ = new StringBuffer();
                }
                int natt = atts.getLength();
                for ( int ia = 0; ia < natt; ia++ ) {
                    String apath = path + "/@" + atts.getQName( ia );
                    if ( resourcePathSet_.contains( apath ) ) {
                        resourceMap_.put( apath, atts.getValue( ia ) );
                    }
                    else if ( capabilityPathSet_.contains( apath ) ) {
                        capabilityMap_.put( apath, atts.getValue( ia ) );
                    }
                }
            }
            catch ( RuntimeException e ) {
                throw new SAXException( e );
            }
        }

        public void endElement( String namepaceURI, String localName,
                                String qName )
                throws SAXException {
            try {
                String path = path_.toString();
                if ( RESOURCE_PATH.equals( path ) ) {
                    RegCapabilityInterface[] caps =
                        (RegCapabilityInterface[])
                        capabilityList_
                       .toArray( new RegCapabilityInterface[ 0 ] );
                    capabilityList_ = null;
                    trimValues( resourceMap_ );
                    resourceList_.add( new Resource( resourceMap_, caps ) );
                    resourceMap_ = null;
                }
                else if ( CAPINTERFACE_PATH.equals( path ) ) {
                    trimValues( capabilityMap_ );
                    capabilityList_.add( new Capability( capabilityMap_ ) );
                    for ( Iterator it = capabilityMap_.keySet().iterator();
                          it.hasNext(); ) {
                        if ( ((String) it.next()).startsWith( path ) ) {
                            it.remove();
                        }
                    }
                }
                else if ( CAPABILITY_PATH.equals( path ) ) {
                    capabilityMap_ = null;
                }
                else if ( resourcePathSet_.contains( path ) ) {
                    resourceMap_.put( path, txtBuf_.toString() );
                    txtBuf_ = null;
                }
                else if ( capabilityPathSet_.contains( path ) ) {
                    capabilityMap_.put( path, txtBuf_.toString() );
                    txtBuf_ = null;
                }
                path_.setLength( path_.length() - localName.length() - 1 );
            }
            catch ( RuntimeException e ) {
                throw new SAXException( e );
            }
        }

        public void characters( char[] ch, int start, int length ) {
            if ( txtBuf_ != null ) {
                txtBuf_.append( ch, start, length );
            }
        }

        public RegResource[] getResources() {
            return (RegResource[])
                   resourceList_.toArray( new RegResource[ 0 ] );
        }

        /**
         * Strips leading and trailing whitespace from string values of
         * map entries.
         *
         * @param  map to trim in place
         */
        private static void trimValues( Map map ) {
            for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                Object val = entry.getValue();
                if ( val instanceof String ) {
                    String tval = ((String) val).trim();
                    if ( tval.length() == 0 ) {
                        it.remove();
                    }
                    else {
                        entry.setValue( tval );
                    }
                }
            }
        }
    } 
}
