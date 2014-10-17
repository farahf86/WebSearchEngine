package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.*;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends IndexerInvertedOccurrence {

  protected Map<Integer, List<Byte>> _utilityIndex = new HashMap<Integer, List<Byte>>();

  public IndexerInvertedCompressed(Options options) {
    super(options);
  }

  @Override
  public void processDocument(int docId, String text) throws IOException, BoilerpipeProcessingException {
    text = removeNonVisibleContext(text);  // step 1 of document processing
    text = removePunctuation(text).toLowerCase();
    text = performStemming(text);  // step 2 of document processing

    Vector<Integer> docTokensAsIntegers = readTermVector(text);

    Set<Integer> uniqueTokens = new HashSet<Integer>();  // unique term ID
    uniqueTokens.addAll(docTokensAsIntegers);

    // Indexing
    Map<Integer, List<Integer>> occurences = new HashMap<Integer,List<Integer>>();

    for(int position = 0; position < docTokensAsIntegers.size(); position++) {
      int word = docTokensAsIntegers.get(position);
      if (!occurences.containsKey(word)) {
        occurences.put(word, new LinkedList<Integer>());
      }
      List<Integer> occurancesList = occurences.get(word);
      occurancesList.add(position);
    }

    for(int word : occurences.keySet()) {
      if(!_utilityIndex.containsKey(word)) {
        _utilityIndex.put(word, new LinkedList<Byte>());
      }
      List<Byte> postingList = _utilityIndex.get(word);
      List<Integer> occurancesList = occurences.get(word);

      byte[] docIdAsBytes = VByteUtils.encodeInt(docId);
      for(byte b : docIdAsBytes) {
        postingList.add(b);
      }

      byte[] sizeAsBytes = VByteUtils.encodeInt(occurancesList.size());
      for(byte b : sizeAsBytes) {
        postingList.add(b);
      }

      for(int occurance : occurancesList) {
        byte[] occuranceAsBytes = VByteUtils.encodeInt(occurance);
        for (byte b : occuranceAsBytes) {
          postingList.add(b);
        }
      }

      _utilityIndexFlatSize += occurancesList.size() + 2;
      occurences.put(word, null);

      if(_utilityIndexFlatSize > UTILITY_INDEX_FLAT_SIZE_THRESHOLD) {
        String filePath = _options._indexPrefix + WORDS_DIR + "/" + _utilityPartialIndexCounter++;
        dumpUtilityIndexToFileAndClearFromMemory(filePath);
      }
    }

    System.out.println("Finished indexing document id: " + docId);
  }

  // This method may be deprecated in later versions. Use with caution!
  @Override
  protected List<Integer> postingsListForWord(int word) throws IOException {
    List<Integer> postingsList = new LinkedList<Integer>();
    FileUtils.FileRange fileRange = _index.get(word);
    _indexRAF.seek(_indexOffset + fileRange.offset);
    int bytesRead = 0;
    boolean keepGoing;
    while(bytesRead < fileRange.length) {
      int pos = 0;
      byte[] buf = new byte[8];
      keepGoing = true;
      do {
        buf[pos++] = _indexRAF.readByte();
        bytesRead++;
        keepGoing = (buf[pos] >>> 7 == 0);
      } while(keepGoing);
      byte[] asBytes = new byte[pos];
      for(int i = 0; i < asBytes.length; i++) {
        asBytes[i] = buf[i];
      }
      postingsList.add(VByteUtils.decodeByteArray(asBytes));
    }

    return postingsList;
  }
}
