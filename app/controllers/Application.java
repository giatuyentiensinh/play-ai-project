package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import models.MatrixUsed;
import models.Rating;
import models.RatingDictionary;
import models.RatingTable;
import models.SaveData;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import Jama.Matrix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author tuyenng
 *
 */
public class Application extends Controller {

	static int numCrossFolds = 10;
	static int sampleFold = 0;

	/**
	 * Có nên Trừ đi đường cơ sở khi tính similitary Sau đó cộng thêm sau khi
	 * đưa ra dự đoán
	 */
	static boolean predictAgainstBaseline = true;

	/**
	 * Số lượng đánh giá thấp nhất mà 2 item phải có chung trước khi chúng được
	 * xử lý Similitary
	 */
	static int minItemOverlapForSimilarity = 12;

	/**
	 * Số lượng đánh giá thấp nhất mà 2 rater phải có chung trước khi chúng được
	 * xử lý Similitary
	 */
	static int minRaterOverlapForSimilarity = 10;

	/**
	 * Số lượng các neighbors lớn nhất có thể
	 */
	static int maxNeighbors = 120;

	/**
	 * Số các neighbors nên có để đưa ra đánh giá
	 */
	static int numItemNeighbors = 20;
	static int numRaterNeighbors = 20;

	/**
	 * phương thức sử dụng để đưa ra đánh giá
	 */
	static RatingDictionary.Method predictionMethod = RatingDictionary.Method.CUSTOM;

	/**
	 * print cho mỗi dự đoán
	 */
	static boolean printPredictions = false;

	/** Cách tính similarities giữa 2 item */
	static RatingTable.SimilarityMeasure itemSimilarityMeasure = RatingTable.SimilarityMeasure.EUCLIDEAN;

	/** Cách tính similarities giữa 2 rater */
	static RatingTable.SimilarityMeasure raterSimilarityMeasure = RatingTable.SimilarityMeasure.PEARSON;

	public static Result getdata() {
		List<Integer> item = new ArrayList<Integer>();
		List<Double> RMSE = new ArrayList<Double>();
		List<Double> RMSE2 = new ArrayList<Double>();
		List<Double> MAE = new ArrayList<Double>();
		List<Double> MAE2 = new ArrayList<Double>();
		RatingTable data = SaveData.getData();
		MatrixUsed mu = SaveData.mu;
		for (int k = 5; k < mu.sVD.rank() / 4; k = k + 5) {
			RatingDictionary rd = RatingDictionary.addItems(mu.itemIndex);
			Matrix U = mu.sVD.getU();
			double[] sigVal = mu.sVD.getSingularValues();
			Matrix V = mu.sVD.getV();
			double[][] matrixA = new double[mu.numUser][mu.numItem];
			HashMap<Integer, Double> averageOfUser = new HashMap<>();
			for (int i = 0; i < mu.numUser; i++) {
				double sumRatingsOfUser = 0;
				for (int j = 0; j < mu.numItem; j++) {
					matrixA[i][j] = 0;
					for (int j2 = 0; j2 < k; j2++) {
						double a = sigVal[j2] * U.get(i, j2) * V.get(j, j2);
						matrixA[i][j] += a;
						sumRatingsOfUser += a;
					}
				}
				averageOfUser.put(new Integer(i), new Double(sumRatingsOfUser
						/ mu.numItem));
			}
			rd.addTrainingData(matrixA, mu, averageOfUser);
			// if (predictAgainstBaseline)
			// rd.subtractBaseline();
			rd.computeItemSimilarities(maxNeighbors, raterSimilarityMeasure);
			rd.computeRaterSimilarities(maxNeighbors, itemSimilarityMeasure);
			RatingTable p = rd.predictTestData(data, predictionMethod,
					numItemNeighbors, numRaterNeighbors, sampleFold,
					numCrossFolds, printPredictions);
			RatingTable p2 = rd.predictTestData2(data, predictionMethod,
					numItemNeighbors, numRaterNeighbors, sampleFold,
					numCrossFolds, printPredictions);
					
			
			// if (predictAgainstBaseline)
			// p.addBaseline(rd);

			System.out
					.println("RMSE of predictions against actual ratings: ( k = "
							+ k + ") " + p.getDistance(data));

			item.add(k);
			RMSE.add(p.getDistance(data));
			MAE.add(p.getAverageErrors(data));
			
			RMSE2.add(p2.getDistance(data));
			MAE2.add(p2.getAverageErrors(data));
			
			// output.println(k + "::" + p.getDistance(data));
		}

		ObjectNode node = Json.newObject();
		node.put("k", Json.toJson(item));
		node.put("RMSE", Json.toJson(RMSE));
		node.put("RMSE2", Json.toJson(RMSE2));
		node.put("MAE", Json.toJson(MAE));
		node.put("MAE2", Json.toJson(MAE2));

		return ok(node);
	}

	public static Result search() {
		RatingDictionary ratingDictionary = SaveData.rd;
		Collection<Rating> result = ratingDictionary.getItemRecommendations(
				"111", predictionMethod, numItemNeighbors);

		return ok(Json.toJson(result.size()));
	}

	public static Result searchFilm() {
		JsonNode params = request().body().asJson();

		String iduser = params.get("iduser").asText();
		RatingDictionary ratingDictionary = SaveData.rd;

		Collection<Rating> result = ratingDictionary.getItemRecommendations(
				iduser, predictionMethod, numItemNeighbors);
		
		Collection<Rating> result2 = ratingDictionary.getItemRecommendations2(
				iduser, predictionMethod, numItemNeighbors);

		MatrixUsed matrixUser = SaveData.mu;
		ArrayList<String> films = new ArrayList<String>();
		result.forEach(item -> {
			films.add(matrixUser.itemIndex.get(item.item));
		});
		
		ArrayList<String> films2 = new ArrayList<String>();
		result2.forEach(item -> {
			films2.add(matrixUser.itemIndex.get(item.item));
		});

		ObjectNode node = Json.newObject();
		node.put("films", Json.toJson(films));
		node.put("films2", Json.toJson(films2));
		node.put("userid", iduser);
		
		return ok(node);
	}
}
