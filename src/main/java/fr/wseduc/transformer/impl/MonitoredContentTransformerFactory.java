package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerFactory;

/**
 * Factory for monitored content transformer client
 */
public class MonitoredContentTransformerFactory implements IContentTransformerFactory {

    IContentTransformerFactory contentTransformerFactory;

    public MonitoredContentTransformerFactory(IContentTransformerFactory contentTransformerFactory) {
        this.contentTransformerFactory = contentTransformerFactory;
    }

    @Override
    public IContentTransformerClient create() {
        // TODO mest : implement a monitored content transformer client in WB-2009
        return IContentTransformerClient.noop;
    }
}
