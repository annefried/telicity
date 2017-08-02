package aspectExperiments.telicity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import sitent.types.ClassificationAnnotation;

// simply marks predefined targets for verb feature extraction

public class TelicityVerbSelectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		List<ClassificationAnnotation> toAdd = new LinkedList<ClassificationAnnotation>();

		Collection<ClassificationAnnotation> classAnnots = JCasUtil.select(
				jCas, ClassificationAnnotation.class);
		for (ClassificationAnnotation ca : classAnnots) {
			if (ca.getTask().equals("ASPECT")) {

				ClassificationAnnotation classAnnot = new ClassificationAnnotation(
						jCas, ca.getBegin(), ca.getEnd());
				classAnnot.setTask("VERB");
				toAdd.add(classAnnot);
			}
		}

		outer: for (ClassificationAnnotation ca : toAdd) {
			Collection<ClassificationAnnotation> classAnnots2 = JCasUtil
					.selectCovered(ClassificationAnnotation.class, ca);
			for (ClassificationAnnotation ca2 : classAnnots2) {
				if (ca2.getTask().equals("VERB")) {
					// already have features for this aspect target
					continue outer;
				}
			}
			ca.addToIndexes();
		
		}

	}

}
