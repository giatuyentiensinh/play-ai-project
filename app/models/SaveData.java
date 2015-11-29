package models;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import Jama.Matrix;
import models.MatrixUsed;
import models.Rating;
import models.RatingDictionary;
import models.RatingTable;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;

public class SaveData{
	
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
	
	
	public static MatrixUsed mu;
	public static RatingDictionary rd;
	public static RatingTable data;
	
	
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

		Logger.info(numRatings + " ratings loaded ...");

		return result;

	}
	
	
	public SaveData() {
		MatrixUsed mu = new MatrixUsed();
		File ratings = Play.application().getFile(
				Play.application().configuration().getString("rate_dir"));
		data = tabulateMovieLensData(ratings);
		mu.computeMatrix(data, numCrossFolds, sampleFold);
		rd = RatingDictionary.addItems(mu.itemIndex);
		Matrix U = mu.sVD.getU();
		double[] sigVal = mu.sVD.getSingularValues();
		Matrix V = mu.sVD.getV();
		double[][] matrixA = new double[mu.numUser][mu.numItem];
		HashMap<Integer, Double> averageOfUser = new HashMap<>();
		int k = 10;
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
		rd.computeItemSimilarities(maxNeighbors, raterSimilarityMeasure);
		rd.computeRaterSimilarities(maxNeighbors, itemSimilarityMeasure);
	}
	
	public static RatingTable getData(){
		return data;
	}

}
