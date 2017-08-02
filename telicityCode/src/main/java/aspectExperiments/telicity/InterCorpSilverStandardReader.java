package aspectExperiments.telicity;

/**
 * Creates XMIs / JCas from the CSV files containing the InterCorp silverstandard.
 * If any changes have been made regarding the input format, make sure to make the indices of "row" below match!
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import sitent.types.ClassificationAnnotation;
import sitent.util.FeaturesUtil;
import au.com.bytecode.opencsv.CSVReader;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class InterCorpSilverStandardReader extends JCasAnnotator_ImplBase {

	public static final String PARAM_ANNOTATIONS_DIR = "annotationsDir";
	@ConfigurationParameter(name = PARAM_ANNOTATIONS_DIR, mandatory = true, defaultValue = "null", description = "directory with annotations in CSV format.")
	private String annotationsDir;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		DocumentMetaData dm = JCasUtil.selectSingle(jCas, DocumentMetaData.class);
		String filename = dm.getDocumentId().replaceAll(".txt", "");
		System.out.println("Processing: " + filename);

		// read in annotations from CSV
		int idx = 0;// character offset within document
		int verbIdx = 0; // offset from start of sentence: for finding verb
							// within sentence
		String docText = jCas.getDocumentText();
		String prevSentence = null;

		try {
			CSVReader r = new CSVReader(new FileReader(annotationsDir + "/" + filename));
			String[] row;
			rowLoop: while ((row = r.readNext()) != null) {
				String verb = row[2];
				String label = row[6];
				if (label.equals("pf")) {
					label = "telic";
				} else if (label.equals("impf")) {
					label = "atelic";
				}
				String sentence = row[7].trim();

				// is this the same as the previous sentence?
				if (prevSentence == null) {
					// nothing to do
				} else if (!prevSentence.equals(sentence)) {
					// a new sentence has started
					idx += prevSentence.length() + 1;
					prevSentence = sentence;
					verbIdx = 0;

				}
				// System.out.println("\nSentence: " + sentence);
				// System.out.println("Verb: " + verb);
				// System.out.println("DocText: " + docText.substring(idx,
				// idx+verbIdx));

				while (!docText.substring(idx + verbIdx).startsWith(verb)
						|| (docText.substring(idx + verbIdx).startsWith(verb) && JCasUtil
								.selectCovered(jCas, Token.class, idx + verbIdx, idx + verbIdx + verb.length())
								.isEmpty())) {
					verbIdx++;
					if (verbIdx > sentence.length()) {
						// did not find this verb in the sentence, skip this row
						// occurs if there are two Czech verbs aligned to one
						// English verb
						continue rowLoop;

						// TODO: in general alignment problems if same verb
						// occurs multiple times in a sentence
						// skip those? --> filter in csv!!

					}
				}
				// add annotation here
				ClassificationAnnotation classAnnot = new ClassificationAnnotation(jCas);
				classAnnot.setTask("ASPECT");
				classAnnot.setBegin(idx + verbIdx);
				classAnnot.setEnd(idx + verbIdx + verb.length());
				FeaturesUtil.addFeature("class_telicity", label, jCas, classAnnot);
				FeaturesUtil.addFeature("human_annotator", "silverStandard", jCas, classAnnot);
				classAnnot.addToIndexes();
				// System.out.println("added annotation: " +
				// classAnnot.getCoveredText());
				verbIdx += verb.length();

				if (prevSentence == null) {
					prevSentence = sentence;
				}

			}

			r.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

	}

}
