package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class RankerHW1 {
  private Index _index;
  public static enum RankingMethod { COSINE, QL, PHRASE, NUMVIEWS, LINEAR };
  public static enum Format { TEXT, HTML };
  public static final String RESULTS_FOLDER = "./results/";

  private boolean saveOutput = false;

  public RankerHW1(String indexSource) throws FileNotFoundException {
    _index = new Index(indexSource);

    if(saveOutput) {
        saveOutput();
    }
  }
  
  public void saveOutput() throws FileNotFoundException {
    System.out.println("Processing query file to " + RESULTS_FOLDER + " folder");
    String inputQueryPath   = "./data/queries.tsv";

    // output file names
    String vsmFileName      = "hw1.1-vsm.tsv";  // vector space model
    String qlFileName       = "hw1.1-ql.tsv";  // query likelihood with Jelinek-Mercer smoothing
    String phraseFileName   = "hw1.1-phrase.tsv";  // phrase-based RankingMethod
    String numviewFileName  = "hw1.1-numviews.tsv";  // numviews-based RankingMethod
    String linearFileName   = "hw1.2-linear.tsv";  // linear RankingMethod
    
    // output query response to be written out to each output file
    String vsmQueryResponse     = "";
    String qlQueryResponse      = "";
    String phraseQueryResponse  = "";
    String numviewQueryResponse = "";
    String linearQueryResponse  = "";
    
    Vector<String> queries = new Vector<String>();
    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(inputQueryPath));
      String line = null;
      while((line = reader.readLine())!=null){
        queries.add(line);
      }
      reader.close();
    } catch (IOException ioe) {
      System.err.println("Oops " + ioe.getMessage());
    }
    
    for(int i=0; i < queries.size(); i++){
      vsmQueryResponse      += getQueryResponse(queries.get(i), "COSINE", "TEXT");
      qlQueryResponse       += getQueryResponse(queries.get(i), "QL", "TEXT");
      phraseQueryResponse   += getQueryResponse(queries.get(i), "PHRASE", "TEXT");
      numviewQueryResponse  += getQueryResponse(queries.get(i), "NUMVIEWS", "TEXT");
      linearQueryResponse   += getQueryResponse(queries.get(i), "LINEAR", "TEXT");
    }
    
    // writing out to files
    File vsmFile = new File(RESULTS_FOLDER + vsmFileName);
    PrintWriter vsmWriter = new PrintWriter(vsmFile);
    vsmWriter.write(vsmQueryResponse);
    vsmWriter.close();
    
    File qlFile = new File(RESULTS_FOLDER + qlFileName);
    PrintWriter qlWriter = new PrintWriter(qlFile);
    qlWriter.write(qlQueryResponse);
    qlWriter.close();
    
    File phraseFile = new File(RESULTS_FOLDER + phraseFileName);
    PrintWriter phraseWriter = new PrintWriter(phraseFile);
    phraseWriter.write(phraseQueryResponse);
    phraseWriter.close();
    
    File numviewFile = new File(RESULTS_FOLDER + numviewFileName);
    PrintWriter numviewWriter = new PrintWriter(numviewFile);
    numviewWriter.write(numviewQueryResponse);
    numviewWriter.close();
    
    File linearFile = new File(RESULTS_FOLDER + linearFileName);
    PrintWriter linearWriter = new PrintWriter(linearFile);
    linearWriter.write(linearQueryResponse);
    linearWriter.close();
  }
  
  public String getQueryResponse(String query, String method, String format){
    try {
        RankingMethod m = RankingMethod.valueOf(method.toUpperCase()); // throws IllegalArgumentException if not a valid enum
        Format f = Format.valueOf(format.toUpperCase());
        Vector<ScoredDocument> scoredDocuments = runquery(query, m);
        Collections.sort(scoredDocuments);
        return getResultsAsString(query, scoredDocuments, f);
    } catch (IllegalArgumentException e) {
        return null;
    }
  }

  // This method returns the result string after sorting the scored documents in the required format:
  // QUERY<TAB>DOCUMENTID-1<TAB>TITLE<TAB>SCORE
  public String getResultsAsString(String query, Vector<ScoredDocument> sortedDocuments, Format format){
	String result = "";
    if(format.equals(Format.TEXT)) {
        for (ScoredDocument document : sortedDocuments) {
            result += query + "\t" + document.asTextResult() + "\n";
        }
    } else if (format.equals(Format.HTML)) {
        for (ScoredDocument document : sortedDocuments) {
            //result += "<a href=\"./clicktrack?documentId=" + document._did + "&query=" + query + "\">" +
            //        document._title + "</a><br/>\n";
        }
    }
    return result;
  }
  
  public Vector<ScoredDocument> runquery(String query, RankingMethod method) {
    Vector<ScoredDocument> retrievalResults = new Vector<ScoredDocument>();
    
    // Build query vector
    Scanner s = new Scanner(query);
    Vector<String> qv = new Vector<String>();
    while (s.hasNext()){
      qv.add(s.next());
    }

    for (int i = 0; i < _index.numDocs(); ++i){
        // Build document term frequency map.
        Map<String, Integer> documentMap = new HashMap<String, Integer>();
        /*
        Vector<String> dv = _index.getDoc(i).get_body_vector();
        for(String word : dv) {
            if(documentMap.containsKey(word)) {
                documentMap.put(word, documentMap.get(word) + 1);
            } else {
                documentMap.put(word, 1);
            }
        }*/

        switch (method) {
            case COSINE:
                retrievalResults.add(cosineSimilarity(qv, documentMap, i));
                break;
            case QL:
                retrievalResults.add(queryLikelihood(qv, documentMap, i));
                break;
            case PHRASE:
                retrievalResults.add(phraseRanker(qv, documentMap, i));
                break;
            case NUMVIEWS:
                retrievalResults.add(numViews(i));
                break;
            case LINEAR:
                retrievalResults.add(simpleLinear(qv, documentMap, i));
                break;
        }
    }
    return retrievalResults;
  }
  
  public ScoredDocument cosineSimilarity(Vector<String> qv, Map<String, Integer> documentMap, int did){
    double score = 0, q_sqr = 0, d_sqr = 0;
    int n = _index.numDocs();
    double idf, tf_q, tf_d, tfidf_q, tfidf_d;

    // inserts query terms into map
    Map<String, Integer> queryMap = new HashMap<String, Integer>();
    for(String query : qv) {
      if(queryMap.containsKey(query)) {
        queryMap.put(query, queryMap.get(query) + 1);
      } else {
        queryMap.put(query, 1);
      }
    }

    // iterates over all words in query
    for(String term : queryMap.keySet()) {
      idf = 1 + Math.log(n / _index.documentFrequency(term)) / Math.log(2);
      tf_q = queryMap.get(term); // count of term in query
      tf_d = documentMap.containsKey(term) ? documentMap.get(term) : 0; // count of term in document
      tfidf_q = tf_q * idf;  // tfidf of term in query
      tfidf_d = tf_d * idf;  // tfidf of term in document
      q_sqr += tfidf_q * tfidf_q;  // computing sum(x^2) term of cosine similarity
      score += tfidf_q * tfidf_d;  // computing sum(x*y) term of cosine similarity
    }

    // we count sum(y^2) separately so that we include all words in document
    for(String term : documentMap.keySet()) {
        idf = 1 + Math.log(n / _index.documentFrequency(term)) / Math.log(2);
        tf_d = documentMap.containsKey(term) ? documentMap.get(term) : 0; // count of term in document
        tfidf_d = tf_d * idf;  // tfidf of term in document
        d_sqr += tfidf_d * tfidf_d; // computing sum(y^2) term of cosine similarity
    }

    if (q_sqr * d_sqr == 0)
      score = 0;
    else
      score /= Math.sqrt(q_sqr * d_sqr); // computing cosine similarity

    return new ScoredDocument(_index.getDoc(did), score);
  }
  
  public ScoredDocument queryLikelihood(Vector<String> qv, Map<String, Integer> documentMap, int did){
    Document d = _index.getDoc(did);
    //Vector<String> dv = d.get_body_vector();
    
    //int documentSize = dv.size();
    int totalWordsInCorpus = _index.termFrequency();
	double lambda = 0.5;
	
	double score = 0;
	
	for(String word : qv) {
	  int wordFrequencyInDocument = documentMap.containsKey(word) ? documentMap.get(word) : 0;
	  int wordFrequencyInCorpus = _index.termFrequency(word);

	  // This formula calculates the value of log( P(Q|D) ), which is given by
	  // the summation of log( ((1-lambda) * wordFrequencyInDocument/documentSize) + wordFrequencyInCorpus/totalWordsInCorpus )
	  // Usage of the log is to overcome the problem of multiplying many small numbers together, which might
	  // lead to accuracy problem.
	  //score += Math.log(((1-lambda) * ((double)wordFrequencyInDocument) / documentSize) +
      //                    lambda * ((double)wordFrequencyInCorpus) / totalWordsInCorpus);
	}
	
    return new ScoredDocument(d, score);
  } 
  
  public ScoredDocument phraseRanker(Vector<String> qv, Map<String, Integer> documentMap, int did){
    Document d = _index.getDoc(did);
    //Vector<String> dv = d.get_body_vector();
    double score = 0;

    if(qv.size() ==  1) {
        String term = qv.firstElement();
        score = documentMap.containsKey(term) ? documentMap.get(term) : 0;
        return new ScoredDocument(_index.getDoc(did), score);
    }

    for(int i = 0; i < qv.size()-1; i++) {
    	//score += getBigramFrequencyInDocument(dv, qv.get(i), qv.get(i+1));
    }
    return new ScoredDocument(_index.getDoc(did), score);
  }
  
  // Method that returns the number of times a bigram appears in the document
  int getBigramFrequencyInDocument(Vector<String> dv, String word1, String word2)
  {
	  int frequency = 0;
      for(int i = 0; i < dv.size()-1; i++) {
          //for a bigram to be counted, we need to find the 2 words consecutively
          if (dv.get(i).equalsIgnoreCase(word1) && dv.get(i + 1).equalsIgnoreCase(word2))
              frequency++;
      }
	  return frequency;
  }
  
  public ScoredDocument numViews(int did){
    Document d = _index.getDoc(did);
    return new ScoredDocument(d, d.getNumViews());
  }
  
  public ScoredDocument simpleLinear(Vector<String> qv, Map<String, Integer> documentMap, int did){
	double betaCos = 1.0/0.8;
	double betaQL = 1.0/9.0;
	double betaPhrase = 1.0/300.0;
	double betaNumviews = 1.0/20000.0;

    double combined_score = 0; // to be removed
	//double combined_score = (betaCos      * cosineSimilarity(qv, documentMap, did)._score) +
	//		                (betaQL       * queryLikelihood(qv, documentMap, did)._score) +
	//		                (betaPhrase   * phraseRanker(qv, documentMap, did)._score) +
	//		                (betaNumviews * numViews(did)._score);
	
	return new ScoredDocument(_index.getDoc(did), combined_score);
  }
}
