package aspectExperiments.telicity;

/**
 * Creates XMIs/JCas for manually annotated files. Primarily for MASC, but has also been used for the InterCorp agreement study files.
 */

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

public class CreateMascJCas {

	public static void main(String[] args) throws UIMAException, IOException {

		String inputDir = args[0]; // path to files with raw text
		String annotationsDir = args[1]; // path to files with annotations in XMI format (as output by SWAN)
		String outputDir = args[2]; // output folder for pre-processed XMIs

		// read text
		CollectionReader reader = createReader(TextReader.class, TextReader.PARAM_SOURCE_LOCATION, inputDir,
				TextReader.PARAM_LANGUAGE, "en", TextReader.PARAM_PATTERNS, new String[] { "[+]*.txt" });

		// read annotations
		AnalysisEngineDescription xmlAnnotationsReader = AnalysisEngineFactory.createEngineDescription(
				XMLAnnotationsReader.class, XMLAnnotationsReader.PARAM_ANNOTATIONS_DIR, annotationsDir);

		// tokenize, parse, add lemmas
		AnalysisEngineDescription stTokenizer = AnalysisEngineFactory.createEngineDescription(StanfordSegmenter.class,
				StanfordSegmenter.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription stParser = AnalysisEngineFactory.createEngineDescription(StanfordParser.class,
				StanfordParser.PARAM_LANGUAGE, "en", StanfordParser.PARAM_WRITE_POS, true,
				StanfordParser.PARAM_WRITE_PENN_TREE, true, StanfordParser.PARAM_MAX_TOKENS, 200,
				StanfordParser.PARAM_WRITE_CONSTITUENT, true, StanfordParser.PARAM_WRITE_DEPENDENCY, true,
				StanfordParser.PARAM_MODE, StanfordParser.DependenciesMode.CC_PROPAGATED);

		AnalysisEngineDescription stLemmas = AnalysisEngineFactory.createEngineDescription(StanfordLemmatizer.class);

		// write XMIs
		AnalysisEngineDescription xmiWriter = AnalysisEngineFactory.createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, outputDir);

		// runPipeline(reader, xmlAnnotationsReader, xmiWriter);

		runPipeline(reader, stTokenizer, stLemmas, stParser, xmlAnnotationsReader, xmiWriter);

	}

}
