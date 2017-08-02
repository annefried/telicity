package aspectExperiments.telicity;

import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import sitent.types.ClassificationAnnotation;
import sitent.types.SEFeature;
import sitent.types.Segment;
import sitent.util.FeaturesUtil;
import sitent.util.SitEntUimaUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

// map extracted features to the Telicity annotation task ones

public class TelicityFeatureMapperAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		Collection<ClassificationAnnotation> classAnnots = JCasUtil.select(
				jCas, ClassificationAnnotation.class);
		Iterator<ClassificationAnnotation> classAnnotIt = classAnnots
				.iterator();

		while (classAnnotIt.hasNext()) {
			ClassificationAnnotation classAnnot = classAnnotIt.next();
			if (classAnnot.getTask().equals("ASPECT")) {
				// // copy over verb features
				// Collection<ClassificationAnnotation> classAnnots2 = JCasUtil
				// .selectCovered(ClassificationAnnotation.class,
				// classAnnot);
				// for (ClassificationAnnotation ca2 : classAnnots2) {
				// if (ca2.getTask().equals("VERB")) {
				// for (Annotation annot : SitEntUimaUtils.getList(ca2
				// .getFeatures())) {
				// SEFeature feature = (SEFeature) annot;
				// FeaturesUtil.addFeature(feature.getName(),
				// feature.getValue(), jCas, classAnnot);
				// }
				// }
				// }

				// simply create Segment
				Collection<Token> tokens = JCasUtil.selectCovered(Token.class, classAnnot);
				Token mainVerb = tokens.iterator().next();

				Segment segment = new Segment(jCas);
				segment.setMainVerb(mainVerb);
				segment.setBegin(classAnnot.getBegin());
				segment.setEnd(classAnnot.getEnd());
				// copy over annotations
				for (Annotation annot : SitEntUimaUtils.getList(classAnnot
						.getFeatures())) {
					SEFeature feature = (SEFeature) annot;
					FeaturesUtil.addFeature(feature.getName(),
							feature.getValue(), jCas, segment);
				}
				segment.addToIndexes();

			}
			
		}

	}

}
