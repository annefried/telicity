package aspectExperiments.telicity;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import sitent.types.ClassificationAnnotation;
import sitent.util.FeaturesUtil;

public class TelicityFilterAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// add LINGUISTIC INDICATOR features for verbs
		Iterator<ClassificationAnnotation> annots = JCasUtil.select(jcas, ClassificationAnnotation.class).iterator();

		List<ClassificationAnnotation> toRemove = new LinkedList<ClassificationAnnotation>();

		while (annots.hasNext()) {

			ClassificationAnnotation annot = annots.next();

			if (annot.getTask() == null || !annot.getTask().equals("ASPECT")) {
				toRemove.add(annot);
				continue;
			}

			else if (annot.getTask().equals("ASPECT")) {

				// only keep majority vote gold standard
				if (!FeaturesUtil.getFeatureValue("human_annotator", annot).equals("majority")) {
					toRemove.add(annot);
					continue;
				}

				// ClassificationAnnotation for verbs covers exactly one token
				// (the head)
				List<Token> tokens = JCasUtil.selectCovered(Token.class, annot);
				if (tokens.size() != 1) {
					// this should not happen
					toRemove.add(annot);
				}
			}

		}

		for (ClassificationAnnotation ca : toRemove) {
			ca.removeFromIndexes();
		}

	}

	public static void main(String[] args) throws UIMAException, IOException {
		String annotDir = "/media/annemarie/Data/Work/Projects/AspectPrediction/Telicity-EN-CZ/automatic_classification/masc2/2-gold-standard";
		String xmiDir = "/media/annemarie/Data/Work/Projects/AspectPrediction/Telicity-EN-CZ/automatic_classification/masc2/3-gold-standard";

		// read XMIs
		CollectionReader reader = createReader(XmiReader.class, XmiReader.PARAM_SOURCE_LOCATION, annotDir,
				XmiReader.PARAM_PATTERNS, new String[] { "[+]*.xmi" }, XmiReader.PARAM_LANGUAGE, "en");

		// create gold standard
		AnalysisEngineDescription telicityFilter = AnalysisEngineFactory
				.createEngineDescription(TelicityFilterAnnotator.class);

		// write XMIs
		AnalysisEngineDescription xmiWriter = AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, xmiDir);

		runPipeline(reader, telicityFilter, xmiWriter);
	}

}
