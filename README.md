WebSearchEngine
===============

This repository contains homework solutions for the Web Search Engines class taught at New York University during Fall 2014 by Professors Cong Yu and Fernando Diaz.

The homeworks lead to building a simple web search engine.

###Group 14

- In order to run the SearchEngine, you can issue:
java edu.nyu.cs.cs2580.SearchEngine 25814 data/corpus.tsv

You can then go to the browser and enter:
http://localhost:25814/search?query%3Dgoogle%26ranker%3Dphrase%26format%3Dhtml

Please note that the url must be encoded like above

- To enable the Ranker and Evaluator to produce the output files, which will be saved in the "results" directory, change the boolean "saveOutput" in both classes to true. The default setting is false, which does not generate output files.

- The input into the Evaluator is via standard input, corresponding to the format of the output of the ranker: QUERY<TAB>DOCUMENTID-1<TAB>TITLE<TAB>SCORE

It is assumed that the input fed into the Evaluator is more than or equal to 10 lines. This is because some metrics, e.g, precision at 10, require such a minimum amount line of input.

It is also assumed that all lines of input has the same query (only considering one query per call).

- Below are the beta parameters for the construction of the simple linear model,

 betaCos = 1.0/0.8
 betaQL = 1.0/9.0
 betaPhrase = 1.0/300.0
 betaNumviews = 1.0/20000.0

- Click logging is also implemented. The log file is saved in the "results" folder.