package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import models.MatrixUsed;
import models.Rating;
import models.RatingDictionary;
import models.RatingTable;
import play.Play;
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

	public static RatingTable tabulateMovieLensData(File ratings) {
		int numRatings = 0;

		RatingTable result = new RatingTable(null,
				RatingTable.CommonAttribute.NONE);
		BufferedReader input = null;

		try {
			input = new BufferedReader(new FileReader(ratings));
			try {
				String line = null; // not declared within while loop
				while ((line = input.readLine()) != null) {
					String words[] = line.split("	");
					String critic = words[0];
					double score = Double.parseDouble(words[2]);
					result.addRating(new Rating(critic, words[1], score));
					numRatings++;
				}
			} finally {
				input.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(">> " + numRatings + " ratings loaded...");

		return result;

	}

	public static Result getdata() {

		List<Integer> item = new ArrayList<Integer>();
		List<Double> RMSE = new ArrayList<Double>();

		MatrixUsed mu = new MatrixUsed();
		File ratings = Play.application().getFile(
				Play.application().configuration().getString("rate_dir"));
		RatingTable data = tabulateMovieLensData(ratings);
		mu.computeMatrix(data, numCrossFolds, sampleFold);
		for (int k = 10; k < mu.sVD.rank() / 30; k = k + 5) {
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
			// if (predictAgainstBaseline)
			// p.addBaseline(rd);

			System.out
					.println("RMSE of predictions against actual ratings: ( k = "
							+ k + ") " + p.getDistance(data));

			item.add(k);
			RMSE.add(p.getDistance(data));
			// output.println(k + "::" + p.getDistance(data));

		}

		ObjectNode node = Json.newObject();
		node.put("k", Json.toJson(item));
		node.put("RMSE", Json.toJson(RMSE));

		return ok(node);
	}

	public static Result tuyen() {
		List<Integer> k = new ArrayList<Integer>();
		ArrayList<String> name = new ArrayList<String>();

		for (int i = 0; i < 10; i++) {
			k.add(i * i);
			name.add("gia tri i = " + (i + 1));
		}

		ObjectNode node = Json.newObject();
		node.put("k", Json.toJson(k));
		node.put("RMSE", Json.toJson(name));

		return ok(node);
	}

	public static Result handerFilm() {
		JsonNode params = request().body().asJson();
		String filmname = params.get("filmName").asText();
		return ok(filmname);
	}
}
