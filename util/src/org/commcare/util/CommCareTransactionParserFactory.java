package org.commcare.util;

import java.io.IOException;
import java.util.Hashtable;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.commcare.util.mocks.UserXmlParser;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.FixtureXmlParser;
import org.commcare.xml.LedgerXmlParsers;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * 
 * A factory for covering all of the basic transactions expected in userspace against
 * the mock data sandbox provided.  
 * 
 * This set of transactions should almost certainly be made uniform between different platform
 * implementations, but the different platforms require some subtle difference right now which
 * make that challenging.   
 * 
 * @author ctsims
 *
 */
public class CommCareTransactionParserFactory implements TransactionParserFactory {

    private TransactionParserFactory userParser;
    private TransactionParserFactory caseParser;
    private TransactionParserFactory stockParser;
    private TransactionParserFactory fixtureParser;
    
    MockUserDataSandbox sandbox;
    
    int requests = 0;
    String syncToken;
    
    public CommCareTransactionParserFactory(MockUserDataSandbox sandbox ) {
        this.sandbox = sandbox;
        
        fixtureParser = new TransactionParserFactory() {
            FixtureXmlParser created = null;
            
            public TransactionParser getParser(KXmlParser parser) {
                if(created == null) {
                    created = new FixtureXmlParser(parser) {
                        //TODO: store these on the file system instead of in DB?
                        private IStorageUtilityIndexed fixtureStorage;
                        
                        /*
                         * (non-Javadoc)
                         * @see org.commcare.xml.FixtureXmlParser#storage()
                         */
                        @Override
                        public IStorageUtilityIndexed storage() {
                            if(fixtureStorage == null) {
                                fixtureStorage = CommCareTransactionParserFactory.this.sandbox.getUserFixtureStorage();
                            } 
                            return fixtureStorage;
                        }
                    };
                }
                
                return created;
            }
        }; 
        
        this.initUserParser();
        this.initCaseParser();
        this.initStockParser();
    }
    
    
    /* (non-Javadoc)
     * @see org.commcare.data.xml.TransactionParserFactory#getParser(java.lang.String, java.lang.String, org.kxml2.io.KXmlParser)
     */
    public TransactionParser getParser(KXmlParser parser) {
        String namespace = parser.getNamespace();
        String name = parser.getName();
        if(LedgerXmlParsers.STOCK_XML_NAMESPACE.matches(namespace)) {
            if(stockParser == null) {
                throw new RuntimeException("Couldn't process Stock transaction without initialization!");
            }
            req();
            return stockParser.getParser(parser);
        } else if(name != null && name.toLowerCase().equals("case")) {
            if(caseParser == null) {
                throw new RuntimeException("Couldn't receive Case transaction without initialization!");
            }
            req();
            return caseParser.getParser(parser);
        } else if(name != null && name.toLowerCase().equals("registration")) {
            if(userParser == null) {
                throw new RuntimeException("Couldn't receive User transaction without initialization!");
            }
            req();
            return userParser.getParser(parser);
        } else if(name != null && name.toLowerCase().equals("fixture")) {
            req();
            return fixtureParser.getParser(parser);
        }else if(name != null && name.toLowerCase().equals("message")) {
            //server message;
            //" <message nature=""/>"
        } else if(name != null && name.toLowerCase().equals("sync") && namespace != null && "http://commcarehq.org/sync".equals(namespace)) {
            return new TransactionParser<String>(parser) {
                /*
                 * (non-Javadoc)
                 * @see org.commcare.data.xml.TransactionParser#commit(java.lang.Object)
                 */
                @Override
                public void commit(String parsed) throws IOException {}

                /*
                 * (non-Javadoc)
                 * @see org.javarosa.xml.ElementParser#parse()
                 */
                @Override
                public String parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
                    this.checkNode("sync");
                    this.nextTag("restore_id");
                    syncToken = parser.nextText();
                    if(syncToken == null) {
                        throw new InvalidStructureException("Sync block must contain restore_id with valid ID inside!", parser);
                    }
                    syncToken = syncToken.trim();
                    return syncToken;
                }
                
            };
        }
        return null;
    }
    
    private void req() {
        requests++;
        reportProgress(requests);
    }
    
    public void reportProgress(int total) {
        //nothing
    }
    
    public void initUserParser() {
        userParser = new TransactionParserFactory() {
            UserXmlParser created = null;
            
            public TransactionParser getParser(KXmlParser parser) {
                if(created == null) {
                    created = new UserXmlParser(parser, sandbox.getUserStorage());
                }
                
                return created;
            }
        };
    }
    
    public void initCaseParser() {
        final int[] tallies = new int[3];
        caseParser = new TransactionParserFactory() {
            CaseXmlParser created = null;
            
            public TransactionParser<Case> getParser(KXmlParser parser) {
                if(created == null) {
                    created = new CaseXmlParser(parser, sandbox.getCaseStorage());
                }
                
                return created;
            }
        };
    }
    
    public void initStockParser() {
        stockParser = new TransactionParserFactory() {
            
            public TransactionParser<Ledger[]> getParser(KXmlParser parser) {
                return new LedgerXmlParsers(parser, sandbox.getLedgerStorage());
            }
        };
    }
    
    public String getSyncToken() {
        return syncToken;
    }
}
