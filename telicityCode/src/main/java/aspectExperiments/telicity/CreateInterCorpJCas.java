package aspectExperiments.telicity;


import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

public class CreateInterCorpJCas {

	public static void main(String[] args) throws UIMAException, IOException {
		String inputDir = args[0]; // path to InterCorp "raw text" files (the shuffled sentences, need to match annotations CSV)
		String annotationsDir = args[1]; // path to InterCorp annotations files
		String outputDir = args[2]; // path to folder where parsed output is going to be written
		

		// read text
		CollectionReader reader = createReader(TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION, inputDir,
				TextReader.PARAM_LANGUAGE, "en", TextReader.PARAM_PATTERNS,
				new String[] { "[+]*.txt" });

		// read annotations
		AnalysisEngineDescription csvAnnotationsReader = AnalysisEngineFactory
				.createEngineDescription(InterCorpSilverStandardReader.class,
						InterCorpSilverStandardReader.PARAM_ANNOTATIONS_DIR,
						annotationsDir);
		
		// tokenize, parse, add lemmas
		AnalysisEngineDescription stTokenizer = AnalysisEngineFactory
				.createEngineDescription(StanfordSegmenter.class,
						StanfordSegmenter.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription stParser = AnalysisEngineFactory
				.createEngineDescription(StanfordParser.class,
						StanfordParser.PARAM_LANGUAGE, "en",
						StanfordParser.PARAM_WRITE_POS, true,
						StanfordParser.PARAM_WRITE_PENN_TREE, true,
						StanfordParser.PARAM_MAX_TOKENS, 200,
						StanfordParser.PARAM_WRITE_CONSTITUENT, true,
						StanfordParser.PARAM_WRITE_DEPENDENCY, true,
						StanfordParser.PARAM_MODE,
						StanfordParser.DependenciesMode.CC_PROPAGATED);

		AnalysisEngineDescription stLemmas = AnalysisEngineFactory
				.createEngineDescription(StanfordLemmatizer.class);

		

		// write XMIs
		AnalysisEngineDescription xmiWriter = AnalysisEngineFactory
				.createEngineDescription(XmiWriter.class,
						XmiWriter.PARAM_TARGET_LOCATION, outputDir);

		runPipeline(reader,
				stTokenizer, stParser, stLemmas, csvAnnotationsReader, xmiWriter);
		

	}

}
