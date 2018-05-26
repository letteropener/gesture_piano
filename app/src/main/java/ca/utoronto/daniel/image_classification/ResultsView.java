package ca.utoronto.daniel.image_classification;

import java.util.List;
import ca.utoronto.daniel.image_classification.Classifier.Recognition;



public interface ResultsView {
  public void setResults(final List<Recognition> results);
}
