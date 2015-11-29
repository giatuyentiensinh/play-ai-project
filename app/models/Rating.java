package models;

import java.util.Comparator;

/**
 * @author tuananh
 *
 */
public class Rating {

	/**
	 * lưu số các đánh giá và đưa cho chúng id duy nhất
	 */
	static int ct = 0;

	/**
	 * Reset lại số đêm ct
	 */
	public static void recount() {
		ct = 0;
	}

	/**
	 * tên của người đánh giá
	 */
	public final String rater;
	public final String item;
	/** Đánh giá (Có thể là sau khi normalized) */
	public double score;

	/** Đánh giá (before normalized) */
	public final double rawScore;
	/** Id duy nhất cho đánh giá mới được tạo */
	public final int seq;

	/**
	 * Constructor
	 * 
	 * @param rater
	 * @param item
	 * @param score
	 */
	public Rating(String rater, String item, double score) {
		this.rater = rater;
		this.item = item;
		this.rawScore = score;
		this.score = score;
		this.seq = ct++;
	}

	/**
	 * In ra mô tả cho Rating
	 */
	public void print() {
		System.out.println("{ " + rater + " rates " + item + " at " + rawScore
				+ " }");
	}

	/**
	 * Sắp xếp thứ hạng tăng dần theo điểm số Được sử dụng để đưa ra Recomment
	 * tại RatingDictionary.getItemRecommendations
	 * RatingDictionary.getRaterRecommendations
	 */
	static public Comparator<Rating> Comp = new Comparator<Rating>() {
		public int compare(Rating r1, Rating r2) {
			if (r1.score > r2.score)
				return -1;
			else if (r2.score > r1.score)
				return 1;

			int c = r1.rater.compareTo(r2.rater);
			if (c != 0)
				return c;
			return r1.item.compareTo(r2.item);
		}
	};
}
