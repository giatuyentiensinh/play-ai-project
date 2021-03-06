package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;

import models.RatingTable.SimilarityMeasure;

/**
 * @author tuananh
 *
 */
public class RatingDictionary {

	/**
	 * Chiến lược đánh giá
	 * 
	 * @author tuananh
	 */
	public static enum Method {
		ITEM_BASELINE, RATER_BASELINE, MIXED_BASELINE, ITEM_SIMILARITY, RATER_SIMILARITY, CUSTOM
	}

	int numRater = 0;
	int numitem = 0;
	double[][] arrayMatrix;
	HashMap<Integer, Double> averageOfUser;
	HashMap<String, Integer> itemMatrixId;
	HashMap<Integer, String> itemMatrixIndex;
	HashMap<String, Integer> userMatrixId;
	HashMap<Integer, String> userMatrixIndex;
	Hashtable<String, String> itemIndex;

	HashMap<Integer, SimilarityTable> raterNeighbors;
	HashMap<Integer, SimilarityTable> itemNeighbors;

	public RatingDictionary() {
		itemMatrixId = new HashMap<>();
		itemMatrixIndex = new HashMap<>();
		userMatrixId = new HashMap<>();
		userMatrixIndex = new HashMap<>();
		itemIndex = new Hashtable<>();
		averageOfUser = new HashMap<>();
		raterNeighbors = new HashMap<>();
		itemNeighbors = new HashMap<>();
	}

	public static RatingDictionary addItems(Hashtable<String, String> itemIndex) {
		RatingDictionary rd = new RatingDictionary();
		rd.addItem(itemIndex);
		return rd;
	}

	private void addItem(Hashtable<String, String> itemIndex) {
		this.itemIndex = itemIndex;
	}

	public void addTrainingData(double[][] matrixA, MatrixUsed mu,
			HashMap<Integer, Double> averageOfUser) {
		arrayMatrix = matrixA;
		itemMatrixId = mu.itemMatrixId;
		itemMatrixIndex = mu.itemMatrixIndex;
		userMatrixId = mu.itemMatrixId;
		userMatrixIndex = mu.itemMatrixIndex;
		this.averageOfUser = averageOfUser;
		this.numitem = mu.numItem;
		this.numRater = mu.numUser;
	}

	public void computeItemSimilarities(int maxNeighbors,
			SimilarityMeasure raterSimilarityMeasure) {

	}

	public void computeRaterSimilarities(int maxNeighbors,
			SimilarityMeasure itemSimilarityMeasure) {
		for (int i = 0; i < numRater; i++) {
			SimilarityTable st = new SimilarityTable(i, arrayMatrix, numitem,
					numRater, itemSimilarityMeasure, maxNeighbors,
					userMatrixIndex, averageOfUser);
			if (st != null)
				raterNeighbors.put(new Integer(i), st);
		}
	}

	public RatingTable predictTestData(RatingTable data,
			Method predictionMethod, int numItemNeighbors,
			int numRaterNeighbors, int sampleFold, int numCrossFolds,
			boolean printPredictions) {
		RatingTable result = new RatingTable(null,
				RatingTable.CommonAttribute.NONE);

		for (Rating r : data.getRatings()) {
			if (r.seq % numCrossFolds == sampleFold) {
				double p = predict(r.rater, r.item, numRaterNeighbors);
				if (printPredictions) {
					System.out
							.println("r.rater: "
									+ r.rater
									+ " r.item: "
									+ r.item
									+ "predict of "
									+ arrayMatrix[userMatrixId.get(r.rater)
											.intValue() - 1][itemMatrixId.get(
											r.item).intValue() - 1] + " to -> "
									+ p);
				}
				Rating prediction = new Rating(r.rater, r.item, p);
				result.addRating(prediction);
			}
		}
		return result;
	}

	public double predict(String rater, String item, int numRaterNeighbors) {
		if (userMatrixId.containsKey(rater) && itemMatrixId.containsKey(item)) {
			int indexRater = userMatrixId.get(rater) -1;
			int indexItem = itemMatrixId.get(item) - 1;
			Integer index = new Integer(indexRater);
			if (!raterNeighbors.containsKey(index)) {
				System.out.println("index no raterNeighbors: "
						+ index.intValue());
			}
			ArrayList<SimilarityTable.Similarity> similarities = raterNeighbors
					.get(index).similarities;
			if (similarities.isEmpty()) {
				// No similar entities... default to rater baseline
				return averageOfUser.get(index);
			} else {
				double numerator1 = 0;
				double denomator1 = 0;
				for (int k = 0; k < Math.min(numRaterNeighbors,
						similarities.size()); k++) {
					SimilarityTable.Similarity s = similarities.get(k);
					numerator1 += s.value
							* (arrayMatrix[s.index][indexItem] - averageOfUser
									.get(new Integer(s.index)));
					denomator1 += Math.abs(s.value);
				}
				if (denomator1 != 0)
					return averageOfUser.get(index).doubleValue()
							+ numerator1 / denomator1;
			}
		}
		return 0;
	}
	
	public RatingTable predictTestData2(RatingTable data,
			Method predictionMethod, int numItemNeighbors,
			int numRaterNeighbors, int sampleFold, int numCrossFolds,
			boolean printPredictions) {
		RatingTable result = new RatingTable(null,
				RatingTable.CommonAttribute.NONE);

		for (Rating r : data.getRatings()) {
			if (r.seq % numCrossFolds == sampleFold) {
				double p = predict2(r.rater, r.item, numRaterNeighbors);
				if (printPredictions) {
					System.out
							.println("r.rater: "
									+ r.rater
									+ " r.item: "
									+ r.item
									+ "predict of "
									+ arrayMatrix[userMatrixId.get(r.rater)
											.intValue() - 1][itemMatrixId.get(r.item).intValue()-1] + " to -> " + p);
				}
				Rating prediction = new Rating(r.rater, r.item, p);
				result.addRating(prediction);
			}
		}
		return result;
	}

	private double predict2(String rater, String item, int numRaterNeighbors) {
		if (userMatrixId.containsKey(rater) && itemMatrixId.containsKey(item)) {
			return arrayMatrix[userMatrixId.get(rater).intValue()-1][itemMatrixId.get(item).intValue()-1];
		}
		return 0;
	}
	
	public Collection<Rating> getItemRecommendations(String rater, Method method, int numNeighbors) {
        ArrayList<Rating> result = new ArrayList<Rating>();
        for (int i = 0; i < numitem; i++) {
			double t = predict(rater, itemMatrixIndex.get(new Integer(i) + 1), numNeighbors);
			result.add(new Rating(rater, itemMatrixIndex.get(new Integer(i) + 1), t));
		}
        Collections.sort(result, Rating.Comp);
        return result;
    }

	public Collection<Rating> getItemRecommendations2(String iduser,
			Method predictionMethod, int numItemNeighbors) {
		ArrayList<Rating> result = new ArrayList<Rating>();
        for (int i = 0; i < numitem; i++) {
			double t = predict2(iduser, itemMatrixIndex.get(new Integer(i) + 1), numItemNeighbors);
			result.add(new Rating(iduser, itemMatrixIndex.get(new Integer(i) + 1), t));
		}
        Collections.sort(result, Rating.Comp);
        return result;
	}
}
