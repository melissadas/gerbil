package org.aksw.gerbil.annotator.impl.nerd;

import java.util.List;

import org.aksw.gerbil.annotator.EntityExtractor;
import org.aksw.gerbil.annotator.impl.AbstractAnnotator;
import org.aksw.gerbil.config.GerbilConfiguration;
import org.aksw.gerbil.datatypes.ErrorTypes;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.eurecom.nerd.client.NERD;
import fr.eurecom.nerd.client.schema.Entity;
import fr.eurecom.nerd.client.type.DocumentType;
import fr.eurecom.nerd.client.type.ExtractorType;
import fr.eurecom.nerd.client.type.GranularityType;

/**
 * Annotator for NERD-ML based on the {@link NERD} class.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class NERDAnnotator extends AbstractAnnotator implements EntityExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NERDAnnotator.class);
    //
    // public static final String ANNOTATOR_NAME = "NERD-ML";
    //
    private static final String NERD_WEB_SERVICE_KEY_PROPERTY_NAME = "org.aksw.gerbil.annotators.NERD.key";
    private static final String NERD_API_PROPERTY_NAME = "org.aksw.gerbil.annotators.NERD.host";

    private NERD nerd;

    public NERDAnnotator() throws GerbilException {
        String host = GerbilConfiguration.getInstance().getString(NERD_API_PROPERTY_NAME);
        if (host == null) {
            throw new GerbilException(
                    "Couldn't load the NERD-ML host (\"" + NERD_API_PROPERTY_NAME + "\") from properties file.",
                    ErrorTypes.ANNOTATOR_LOADING_ERROR);
        }
        // Load and use the key if there is one
        String key = GerbilConfiguration.getInstance().getString(NERD_WEB_SERVICE_KEY_PROPERTY_NAME);
        if (key == null) {
            throw new GerbilException("Couldn't load the NERD-ML API key (\"" + NERD_WEB_SERVICE_KEY_PROPERTY_NAME
                    + "\") from properties file.", ErrorTypes.ANNOTATOR_LOADING_ERROR);
        }
        nerd = new NERD(host, key);
    }

    public NERDAnnotator(String host) throws GerbilException {
        // Load and use the key if there is one
        String key = GerbilConfiguration.getInstance().getString(NERD_API_PROPERTY_NAME);
        if (key == null) {
            throw new GerbilException(
                    "Couldn't load the NERD API key (\"" + NERD_API_PROPERTY_NAME + "\") from properties file.",
                    ErrorTypes.ANNOTATOR_LOADING_ERROR);
        }
        nerd = new NERD(host, key);
    }

    public NERDAnnotator(String host, String key) {
        nerd = new NERD(host, key);
    }

    @Override
    public List<MeaningSpan> performLinking(Document document) throws GerbilException {
        return getNERDAnnotations(document).getMarkings(MeaningSpan.class);
    }

    @Override
    public List<Span> performRecognition(Document document) throws GerbilException {
        return getNERDAnnotations(document).getMarkings(Span.class);
    }

    @Override
    public List<MeaningSpan> performExtraction(Document document) throws GerbilException {
        return getNERDAnnotations(document).getMarkings(MeaningSpan.class);
    }

    /**
     * Send request to NERD and parse the response as a set of scored
     * annotations.
     *
     * @param text
     *            the text to send
     */
    public Document getNERDAnnotations(Document document) throws GerbilException {
        Document resultDoc = new DocumentImpl(document.getText(), document.getDocumentURI());
        try {
            List<Entity> entities = nerd.annotate(ExtractorType.NERDML, DocumentType.PLAINTEXT, document.getText(),
                    GranularityType.OEN, 60L, true, true);
            LOGGER.debug("NERD has found {} entities", entities.size());
            for (Entity e : entities) {
                resultDoc.addMarking(new ScoredNamedEntity(e.getStartChar(), e.getEndChar() - e.getStartChar(),
                        e.getUri(), e.getConfidence()));
            }
        } catch (Exception e) {
            throw new GerbilException("Exception while querying NERD-ML.", e, ErrorTypes.UNEXPECTED_EXCEPTION);
        }
        return resultDoc;
    }
}
