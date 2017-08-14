package org.javarosa.xpath.analysis;

/**
 * Created by amstone326 on 8/11/17.
 */

public interface XPathAnalyzable {

    void applyAndPropagateAccumulatingAnalyzer(XPathAccumulatingAnalyzer analyzer);

    //void applyBooleanAnalyzer(XPathBooleanAnalyzer analyzer);
}
