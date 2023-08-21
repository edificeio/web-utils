package fr.wseduc.transformer.impl;

import fr.wseduc.transformer.IContentTransformerClient;
import fr.wseduc.transformer.IContentTransformerFactory;

/**
 * Factory for dummy implementation of {@link IContentTransformerClient} to avoid NPEs
 */
public class NoopContentTransformerFactory implements IContentTransformerFactory {

    public static final NoopContentTransformerFactory instance = new NoopContentTransformerFactory();

    @Override
    public IContentTransformerClient create() {
        return IContentTransformerClient.noop;
    }

    public NoopContentTransformerFactory() {}
}
