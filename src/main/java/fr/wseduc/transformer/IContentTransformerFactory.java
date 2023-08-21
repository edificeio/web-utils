package fr.wseduc.transformer;

/**
 * Factory to create a content transformer client
 */
public interface IContentTransformerFactory {

    /**
     * Creates a client
     * @return the client
     */
    IContentTransformerClient create();
}
