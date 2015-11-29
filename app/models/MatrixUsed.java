package models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;

import play.Play;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class MatrixUsed {

	Matrix ratings;
	public SingularValueDecomposition sVD;
	HashMap<String, Integer> itemMatrixId;
	HashMap<Integer, String> itemMatrixIndex;
	HashMap<String, Integer> userMatrixId;
	HashMap<Integer, String> userMatrixIndex;
	public Hashtable<String, String> itemIndex;
	public Integer numUser;
	public Integer numItem;

	public MatrixUsed() {
		itemMatrixId = new HashMap<>();
		itemMatrixIndex = new HashMap<>();
		userMatrixId = new HashMap<>();
		userMatrixIndex = new HashMap<>();
		itemIndex = new Hashtable<>();
		numUser = 0;
		numItem = 0;
		File itemFile = Play.application().getFile(
				Play.application().configuration().getString("item_dir"));
		File userFile = Play.application().getFile(
				Play.application().configuration().getString("user_dir"));
		BufferedReader inputItem = null;
		// Tao gia tri cho cac bien Item
		try {
			int index = 0;
			inputItem = new BufferedReader(new FileReader(itemFile));
			try {
				String line = null;
				while ((line = inputItem.readLine()) != null) {
					String words[] = line.split("\\|");
					index++;
					itemMatrixId.put(words[0], new Integer(index));
					itemMatrixIndex.put(new Integer(index), words[0]);
					itemIndex.put(words[0], words[1]);
					// System.out.println(words[0]);
				}
			}

			finally {
				numItem = index;
				System.out.println("numItem = index: " + numItem);
				inputItem.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Tao gia tri cho cac bien user
		BufferedReader inputUser = null;
		try {
			int index = 0;
			inputUser = new BufferedReader(new FileReader(userFile));
			try {
				String line = null;
				while ((line = inputUser.readLine()) != null) {
					String words[] = line.split("\\|");
					index++;
					userMatrixId.put(words[0], new Integer(index));
					userMatrixIndex.put(new Integer(index), words[0]);
				}
			}

			finally {
				numUser = index;
				System.out.println("numUser = index: " + numUser);
				inputUser.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void computeMatrix(RatingTable rt, int numFolds, int fold) {
		double arrayRatings[][] = new double[numUser][numItem];
		System.out.println("   " + numUser + " " + numItem);
		for (Rating r : rt.getRatings()) {
			if (numFolds <= 1 || r.seq % numFolds != fold) {
				arrayRatings[userMatrixId.get(r.rater).intValue() - 1][itemMatrixId
						.get(r.item).intValue() - 1] = r.rawScore;
			}
		}
		RatingData rd = new RatingData();
		rd.addTrainingData(rt, fold, numFolds);
		for (int i = 0; i < numUser; i++) {
			for (int j = 0; j < numItem; j++) {
				if (arrayRatings[i][j] == 0) {
					double t = rd.geometricMeanBaseline(
							userMatrixIndex.get(new Integer(i + 1)),
							itemMatrixIndex.get(new Integer(j + 1)));
					if(t>5){
						arrayRatings[i][j] = 5;
					} else{
						arrayRatings[i][j] = t;
					}
				}
			}
		}

		ratings = Matrix.constructWithCopy(arrayRatings);
		sVD = ratings.svd();
		System.out.println("Compute SVD done with rank: " + sVD.rank());
	}
}
