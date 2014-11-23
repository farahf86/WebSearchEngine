package edu.nyu.cs.cs2580;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;

public class SpearmanTest {

  static Scanner scanner = null;
  static String pathToPagerank = "tests/Spearman/pagerank";
  static String pathToNumviews = "tests/Spearman/numviews";
  static double tolerance = 0.000001;

  @Test
  public void testSpearmanCoefficient() throws Exception {
    // collect PageRank and Numviews data
    Map<String, Float> pageRanks = Maps.newHashMap();
    Map<String, Integer> numViews = Maps.newHashMap();

    scanner = new Scanner(new BufferedReader(new FileReader(pathToPagerank)));
    while (scanner.hasNextLine()) {
      String[] line = scanner.nextLine().split("\\s+");
      checkState(line.length == 2);
      pageRanks.put(line[0], Float.parseFloat(line[1]));
    }
    scanner.close();

    scanner = new Scanner(new BufferedReader(new FileReader(pathToNumviews)));
    while (scanner.hasNextLine()) {
      String[] line = scanner.nextLine().split("\\s+");
      checkState(line.length == 2);
      if (pageRanks.containsKey(line[0])) {
        numViews.put(line[0], Integer.parseInt(line[1]));
      }
    }
    scanner.close();

    // assign rank for each document
    List<Float> x_k = Spearman.assignRank(pageRanks);
    List<Float> y_k = Spearman.assignRank(numViews);

    assertEquals(Spearman.spearmanCoefficient(x_k, y_k), 0.08947368421052632, tolerance);
  }

  @Test
  public void testAssignRank() throws Exception {
    // collect PageRank and Numviews data
    Map<String, Float> pageRanks = Maps.newHashMap();
    Map<String, Integer> numViews = Maps.newHashMap();

    scanner = new Scanner(new BufferedReader(new FileReader(pathToPagerank)));
    while (scanner.hasNextLine()) {
      String[] line = scanner.nextLine().split("\\s+");
      checkState(line.length == 2);
      pageRanks.put(line[0], Float.parseFloat(line[1]));
    }
    scanner.close();

    scanner = new Scanner(new BufferedReader(new FileReader(pathToNumviews)));
    while (scanner.hasNextLine()) {
      String[] line = scanner.nextLine().split("\\s+");
      checkState(line.length == 2);
      if (pageRanks.containsKey(line[0])) {
        numViews.put(line[0], Integer.parseInt(line[1]));
      }
    }
    scanner.close();

    List<Float> x_k = Spearman.assignRank(pageRanks);
    List<Float> y_k = Spearman.assignRank(numViews);

    float[] expectedPageRank = {1.0F, 5.0F, 3.0F, 4.0F, 2.0F};
    float[] expectedNumViews = {1.0F, 4.0F, 2.5F, 5.0F, 2.5F};

    for (int i = 1; i<expectedPageRank.length; i++){
      assertEquals(x_k.get(i), expectedPageRank[i], tolerance);
      assertEquals(y_k.get(i), expectedNumViews[i], tolerance);
    }
  }
}
