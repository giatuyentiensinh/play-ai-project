package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @author tuananh
 *
 */
public class SimilarityTable {
	static public class Similarity {
		/** Tên của similar (rater hoặc item) */
		public int index;
		/** Độ tương quan */
		public double value;

		/**
		 * Constructor
		 * 
		 * @param indexRater2
		 * @param value
		 */
		public Similarity(int indexRater2, double value) {
			this.index = indexRater2;
			this.value = value;
		}
	}

	/**
	 * So sánh đôi tưởng để sắp xếp các bản ghi Bản ghi có có giá trị similariry
	 * lớn đứng trước
	 */
	static public Comparator<Similarity> Comp = new Comparator<Similarity>() {
		public int compare(Similarity s1, Similarity s2) {
			if (Math.abs(s1.value) > Math.abs(s2.value))
				return -1;
			else if (Math.abs(s2.value) > Math.abs(s1.value))
				return 1;
			else
				return s1.index >= s2.index ? 1 : -1;
		}
	};

	/** Tên đối tượng */
	public String name;
	/** danh sách các similatiry có quan hệ với đới tượng */
	public ArrayList<Similarity> similarities;

	public SimilarityTable(int raterIndex, double[][] matrix, int numItem,
			int numUser, RatingTable.SimilarityMeasure sim, int maxNeighbors,
			HashMap<Integer, String> id, HashMap<Integer, Double> averageOfUser) {
		this.name = id.get(new Integer(raterIndex));
		similarities = new ArrayList<Similarity>();
		double raterAverage1 = averageOfUser.get(new Integer(raterIndex))
				.doubleValue();
		// Dựng tất cả các entity và sắp xếp theo similarity
		for (int i = 0; i < numUser; i++) {
			if (i == raterIndex)
				continue;
			double raterAverage2 = averageOfUser.get(new Integer(i))
					.doubleValue();
			Similarity elt = computeSimilarity(matrix[raterIndex],
					raterAverage1, matrix[i], raterAverage2, numItem, i);
			if (elt != null)
				similarities.add(elt);
		}
		Collections.sort(similarities, Comp);

		// Loại bỏ các similarities khi vượt quá max để giảm bộ nhớ
		int length = similarities.size();
		if (length > maxNeighbors)
			similarities.subList(maxNeighbors, length).clear();
		similarities.trimToSize();
	}

	private Similarity computeSimilarity(double[] ds, double raterAverage1,
			double[] ds2, double raterAverage2, int numItem, int indexRater2) {
		double numerator = 0;
		double denominator1 = 0;
		double denominator2 = 0;
		for (int i = 0; i < numItem; i++) {
			numerator += (ds[i] - raterAverage1) * (ds2[i] - raterAverage2);
			denominator1 += (ds[i] - raterAverage1) * (ds[i] - raterAverage1);
			denominator2 += (ds2[i] - raterAverage2) * (ds2[i] - raterAverage2);
		}
		double denomator = Math.sqrt(denominator1) * Math.sqrt(denominator2);
		if (denomator == 0)
			return null;
		double result = numerator / denomator;
		Similarity a = new Similarity(indexRater2, result);
		return a;
	}

	/**
	 * @param detail
	 */
	public void summarize(int detail) {
		int ct = 0;
		System.out.println("top matches for " + name + ":");
		for (Similarity s : similarities) {
			System.out.println("{ " + s.index + "@ " + s.value + " by y = "
					+ " }");
			ct++;
			if (ct >= detail)
				break;
		}
	}

}
