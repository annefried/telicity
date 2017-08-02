package aspectExperiments.telicity;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import sitent.types.ClassificationAnnotation;
import sitent.util.FeaturesUtil;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class XMLAnnotationsReader extends JCasAnnotator_ImplBase {

	public static final String PARAM_ANNOTATIONS_DIR = "annotationsDir";
	@ConfigurationParameter(name = PARAM_ANNOTATIONS_DIR, mandatory = true, defaultValue = "null", description = "directory with annotations in XML format.")
	private String annotationsDir;

	Map<String, List<String>> annotFiles;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		annotFiles = new HashMap<String, List<String>>();

		File annotDir = new File(annotationsDir);
		for (String filename : annotDir.list()) {
			String[] parts = filename.split("_");
			String title = "";
			for (int i = 1; i < parts.length - 1; i++) {
				title += parts[i] + "_";
			}
			title = title.substring(0, title.length() - 1);
			if (!annotFiles.containsKey(title))
				annotFiles.put(title, new LinkedList<String>());
			annotFiles.get(title).add(filename);
		}

	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		DocumentMetaData dm = JCasUtil.selectSingle(jCas,
				DocumentMetaData.class);
		String filename = dm.getDocumentId().replaceAll(".txt", "");
		// for intercorp:
		filename = "AspectTelicity_" + filename;
		System.out.println("Processing: " + filename);

		// find the files with the annotations
		for (String annotFile : annotFiles.get(filename)) {
			System.out.println("adding annotations from: " + annotFile);
			String[] parts = annotFile.split("_");
			String title = "";
			for (int i = 1; i < parts.length - 1; i++) {
				title += parts[i] + "_";
			}
			title = title.substring(0, title.length() - 1);
			String annotator = parts[parts.length - 1].replaceAll(".xml", "");

			// read XML with annotations for this file and add annotations to
			// JCas
			File xmlFile;
			try {

				xmlFile = new File(annotationsDir + "/" + annotFile);

				SAXReader reader = new SAXReader();
				Document document = reader.read(xmlFile);
				Element root = document.getRootElement();
				root = root.element("annotations");

				for (@SuppressWarnings("unchecked")
				Iterator<Element> i = root.elementIterator("annotation"); i
						.hasNext();) {

					Element annotation = i.next();
					int begin = Integer.parseInt(annotation.element("start")
							.getText());
					int end = Integer.parseInt(annotation.element("end")
							.getText());

					String lexAsp = null;
					String telicity = null;

					for (@SuppressWarnings("unchecked")
					Iterator<Element> labels = annotation
							.elementIterator("labels"); labels.hasNext();) {
						Element label = labels.next();
						String labelSetName = label.attribute("labelSetName")
								.getValue();
						String annot = label.element("label").getText();
						if (labelSetName.equals("Lexical Aspectual Class")) {
							lexAsp = annot;
						}
						if (labelSetName.equals("Telicity")) {
							telicity = annot;
						}

					}

					ClassificationAnnotation classAnnot = new ClassificationAnnotation(
							jCas);
					classAnnot.setBegin(begin);
					classAnnot.setEnd(end);
					classAnnot.setTask("ASPECT");
					if (lexAsp != null) {
						FeaturesUtil.addFeature("class_lexical_aspect", lexAsp,
								jCas, classAnnot);
					}
					if (telicity != null) {
						FeaturesUtil.addFeature("class_telicity", telicity,
								jCas, classAnnot);
					}
					FeaturesUtil.addFeature("human_annotator", annotator, jCas,
							classAnnot);
					classAnnot.addToIndexes();
				}

			} catch (DocumentException e) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException();
			}

		}

	}

}
