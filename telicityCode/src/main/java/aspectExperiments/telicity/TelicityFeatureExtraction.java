package aspectExperiments.telicity;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import sitent.classifiers.SitEntFeaturesAnnotator;
import sitent.classifiers.WekaArffWriterAnnotator;
import sitent.segmentation.SitEntSyntacticFeaturesAnnotator;
import sitent.syntSemFeatures.nounPhrase.NounPhraseFeaturesAnnotator;
import sitent.syntSemFeatures.nounPhrase.NounPhraseSelectorAnnotator;
import sitent.syntSemFeatures.segment.BrownClusterFeaturesAnnotator;
import sitent.syntSemFeatures.segment.MathewKatzFeaturesAnnotator;
import sitent.syntSemFeatures.segment.SpeechModeFeaturesAnnotator;
import sitent.syntSemFeatures.verbs.LinguisticIndicatorsAnnotator;
import sitent.syntSemFeatures.verbs.VerbFeaturesAnnotator;

public class TelicityFeatureExtraction {

	public static final boolean PREPROCESS = false;

	public static void main(String[] args) throws UIMAException, IOException {

		String inputDir = args[0]; // the folder with the XMIs created in the previous step
		String outputDir = args[1]; // output directory for XMIs with features
		String arffPath = args[2]; // output directory for ARFF files (to be used by classifier)

		// read XMIs
		CollectionReader reader = createReader(XmiReader.class, XmiReader.PARAM_SOURCE_LOCATION, inputDir,
				XmiReader.PARAM_LANGUAGE, "en", XmiReader.PARAM_PATTERNS, new String[] { "[+]*.xmi" });

		// tokenize, parse, add lemmas
		AnalysisEngineDescription stTokenizer = AnalysisEngineFactory.createEngineDescription(StanfordSegmenter.class,
				StanfordSegmenter.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription stParser = AnalysisEngineFactory.createEngineDescription(StanfordParser.class,
				StanfordParser.PARAM_LANGUAGE, "en", StanfordParser.PARAM_WRITE_POS, true,
				StanfordParser.PARAM_WRITE_PENN_TREE, true, StanfordParser.PARAM_MAX_TOKENS, 200,
				StanfordParser.PARAM_WRITE_CONSTITUENT, true, StanfordParser.PARAM_WRITE_DEPENDENCY, true,
				StanfordParser.PARAM_MODE, StanfordParser.DependenciesMode.CC_PROPAGATED);

		AnalysisEngineDescription stLemmas = AnalysisEngineFactory.createEngineDescription(StanfordLemmatizer.class);

		// filter annotations that don't match the tokens
		AnalysisEngineDescription filter = AnalysisEngineFactory.createEngineDescription(TelicityFilterAnnotator.class);

		// extract syntactic-semantic features
		AnalysisEngineDescription npSelector = AnalysisEngineFactory.createEngineDescription(
				NounPhraseSelectorAnnotator.class, NounPhraseSelectorAnnotator.PARAM_TARGET, "AllNounPhrases");
		AnalysisEngineDescription npFeatures = AnalysisEngineFactory.createEngineDescription(
				NounPhraseFeaturesAnnotator.class, NounPhraseFeaturesAnnotator.PARAM_COUNTABILITY_PATH,
				"resources/countability/webcelex_countabilityNouns.txt", NounPhraseFeaturesAnnotator.PARAM_WORDNET_PATH,
				"resources/wordnet3.0");
		AnalysisEngineDescription verbSelector = AnalysisEngineFactory
				.createEngineDescription(TelicityVerbSelectorAnnotator.class);
		AnalysisEngineDescription verbFeatures = AnalysisEngineFactory.createEngineDescription(
				VerbFeaturesAnnotator.class, VerbFeaturesAnnotator.PARAM_WORDNET_PATH, "resources/wordnet3.0",
				VerbFeaturesAnnotator.PARAM_TENSE_FILE, "resources/tense/tense.txt");
		AnalysisEngineDescription lingInd = AnalysisEngineFactory.createEngineDescription(
				LinguisticIndicatorsAnnotator.class, LinguisticIndicatorsAnnotator.PARAM_LING_IND_FILE,
				"resources/linguistic_indicators/linguistic-indicators-Gigaword-AFE-XIE.csv");

		// add some more features directly to the Segment annotations
		// extract segment-based POS/lemma/word features
		// AnalysisEngineDescription posLemma = AnalysisEngineFactory
		// .createEngineDescription(PosLemmaDepFeaturesAnnotator.class);

		// extract features designed for recognition of QUESTIONS +
		// IMPERATIVES
		AnalysisEngineDescription speechModeFeatures = AnalysisEngineFactory.createEngineDescription(
				SpeechModeFeaturesAnnotator.class, SpeechModeFeaturesAnnotator.PARAM_QUESTION_WORDS_FILE,
				"resources/word_lists/question_words.txt", SpeechModeFeaturesAnnotator.PARAM_WORDNET_PATH,
				"resources/wordnet3.0");

		AnalysisEngineDescription mkFeatures = AnalysisEngineFactory.createEngineDescription(
				MathewKatzFeaturesAnnotator.class, MathewKatzFeaturesAnnotator.PARAM_HABIT_ADV_PATH,
				"resources/mathew_katz_lists/habitual-adverbs.txt");

		AnalysisEngineDescription brownFeatures = AnalysisEngineFactory.createEngineDescription(
				BrownClusterFeaturesAnnotator.class, BrownClusterFeaturesAnnotator.PARAM_BROWN_CLUSTER_DIR,
				"resources/brown_clusters");

		AnalysisEngineDescription telicityFeatureMapper = AnalysisEngineFactory
				.createEngineDescription(TelicityFeatureMapperAnnotator.class);

		// segments have been created, identify subjects
		AnalysisEngineDescription mainRefIdentifier = AnalysisEngineFactory
				.createEngineDescription(SitEntSyntacticFeaturesAnnotator.class);

		// map features to segments
		// add features for classification to the Segment annotations for
		// the above syntactic-semantic features
		AnalysisEngineDescription sitEntFeatureMapper = AnalysisEngineFactory
				.createEngineDescription(SitEntFeaturesAnnotator.class);

		// write ARFF files (for classification toolkit Weka, also used to
		// generate CRFPP input files)
		AnalysisEngineDescription arffWriter = AnalysisEngineFactory.createEngineDescription(
				WekaArffWriterAnnotator.class, WekaArffWriterAnnotator.PARAM_RESET_SEGIDS, false,
				WekaArffWriterAnnotator.PARAM_ARFF_LOCATION, arffPath, WekaArffWriterAnnotator.PARAM_CLASS_ATTRIBUTE,
				"Telicity", WekaArffWriterAnnotator.PARAM_SPARSE_FORMAT, true,
				WekaArffWriterAnnotator.PARAM_OMIT_FEATURES, "segment_acl2007_G_verbLemma_.*",
				WekaArffWriterAnnotator.PARAM_TARGET_TYPE, "Segment", WekaArffWriterAnnotator.PARAM_ESCAPE_VALUES,
				true);

		// write XMIs
		AnalysisEngineDescription xmiWriter = AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, outputDir);

		if (PREPROCESS) {
			runPipeline(reader, stTokenizer, stParser, stLemmas, filter, npSelector, npFeatures, verbSelector,
					verbFeatures, lingInd, speechModeFeatures, mkFeatures, brownFeatures, telicityFeatureMapper,
					mainRefIdentifier, sitEntFeatureMapper, arffWriter, xmiWriter);
		} else {

			runPipeline(reader, filter, npSelector, npFeatures, verbSelector, verbFeatures, lingInd, speechModeFeatures,
					mkFeatures, brownFeatures, telicityFeatureMapper, mainRefIdentifier, sitEntFeatureMapper,
					arffWriter, xmiWriter);
		}
	}
}
