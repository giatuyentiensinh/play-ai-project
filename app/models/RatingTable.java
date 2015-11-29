package models;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author tuananh
 *
 */
public class RatingTable {

	/** Distance measure - Euclidean hoặc hê số tương quan Pearson */
	public static enum SimilarityMeasure {
		EUCLIDEAN, PEARSON
	}

	/** Kiểu dữ liệu được lưu trữ */
	public static enum CommonAttribute {
		ITEM, RATER, NONE
	}

	private HashMap<String, Rating> ratings;

	/** Đánh Index theo (raterId, ItemId, NONE */
	private CommonAttribute commonInfo;

	private String name;

	/** Số đánh giá */
	private int count;

	/** Tổng số điểm đánh giá */
	private double points;

	/** Tổng số loga tự nhiên của điểm đánh giá */
	private double logSum;

	/**
	 * Constructor
	 * 
	 * @param entry
	 *            thành phần chung của đánh giá (người đánh giá - critic, item,
	 *            NONE)
	 */
	public RatingTable(String entry, CommonAttribute type) {
		name = entry;
		this.commonInfo = type;
		ratings = new HashMap<String, Rating>();
		points = 0;
		logSum = 0;
		count = 0;
	}

	public String getName() {
		return name;
	}

	public Collection<Rating> getRatings() {
		return ratings.values();
	}

	/**
	 * dựng Id cho RatingTable theo Item hoặc Rater
	 * 
	 * @param rating
	 * @return Id của RatingTable
	 */
	private String keyFor(Rating rating) {
		switch (commonInfo) {
		case ITEM:
			return rating.rater;

		case RATER:
			return rating.item;

		default:
			return rating.rater + " # " + rating.item;
		}
	}

	/**
	 * Thêm đánh giá
	 * 
	 * @param rating
	 */
	public void addRating(Rating rating) {
		if (rating == null)
			return;

		ratings.put(keyFor(rating), rating);
		count++;
		points += rating.rawScore;
		logSum += Math.log(rating.rawScore);
	}

	/**
	 * Get keys của ratings
	 * 
	 * @return list key của ratings
	 */
	public Collection<String> getRatedKeys() {
		return ratings.keySet();
	}

	/**
	 * Đưa ra giá trị rating ứng với key ( keyFor() )
	 * 
	 * @param key
	 * @return
	 */
	public Rating getRatingFor(String key) {
		return ratings.get(key);
	}

	/**
	 * Đưa ra Rating khi RatingTable có key Trùng với giá trị keyFor(r)
	 * 
	 * @param r
	 * @return
	 */
	public Rating getMatchingRating(Rating r) {
		return getRatingFor(keyFor(r));
	}

	/**
	 * Hệ số prediction cho cơ sở ước tính trung bình hình học từ tập hợp các dữ
	 * liệu đánh giá
	 * 
	 * @return
	 */
	public double getFactor() {
		if (count == 0)
			return 0;
		return logSum / count;
	}

	/**
	 * Điểm đánh giá trung bình
	 * 
	 * @return
	 */
	public double getAverage() {
		if (count == 0)
			return 0;
		return points / count;
	}

	private double getRMSDistance(RatingTable table) {
		double sum_of_squares = 0, diff = 0;
		int ct = 0;

		for (Rating r1 : this.getRatings()) {
			Rating r2 = table.getMatchingRating(r1);
			if (r2 != null) {
				diff = r1.score - r2.score;
				sum_of_squares += diff * diff;
				ct++;
			}
		}

		double result = sum_of_squares;
		if (ct > 0)
			result /= ct;

		return Math.sqrt(result);
	}

	public double getDistance(RatingTable table) {
		return getRMSDistance(table);
	}

	private double getRMSSimilarity(RatingTable table) {
		return 1 / (1 + getRMSDistance(table));
	}
	
	private double getMAE(RatingTable table) {
        double sum_of_abs = 0, diff = 0;
        int ct = 0;

        for (Rating r1 : this.getRatings()) {
            Rating r2 = table.getMatchingRating(r1);
            if (r2 != null) {
                diff = r1.score - r2.score;
                sum_of_abs += Math.abs(diff);
                ct++;
            }
        }
        
        double result = sum_of_abs;
        if (ct > 0)
            result /= ct;
        
        return result;
    }
    
    public double getAverageErrors(RatingTable table) {
        return getMAE(table);
    }

	/**
	 * Độ tương đồng giữa 2 người đánh giá hoặc 2 bộ phim
	 * 
	 * @param table
	 * @return
	 */
	public double getPearsonSimilarity(RatingTable table) {
		double sum1Sq = 0, sum2Sq = 0, pSum = 0, n = 0;
		double sum1 = 0, sum2 = 0, num = 0, den = 0;

		for (Rating r1 : this.getRatings()) {
			Rating r2 = table.getMatchingRating(r1);
			if (r2 != null) {
				sum1 += r1.score;
				sum2 += r2.score;
				sum1Sq += r1.score * r1.score;
				sum2Sq += r2.score * r2.score;
				pSum += r1.score * r2.score;
				n++;
			}
		}

		if (n == 0)
			return 0;

		num = pSum - (sum1 * sum2 / n);
		den = Math
				.sqrt((sum1Sq - sum1 * sum1 / n) * (sum2Sq - sum2 * sum2 / n));
		if (den == 0)
			return 0;

		double r = num / den;
		return (r > 0) ? r : 0;
	}

	/**
	 * @param table
	 * @param simMeasure
	 * @return
	 */
	public double getSimilarity(RatingTable table, SimilarityMeasure simMeasure) {
		switch (simMeasure) {
		case PEARSON:
			return getPearsonSimilarity(table);
		case EUCLIDEAN:
		default:
			return getRMSSimilarity(table);
		}
	}

	// /** Xây dựng quan hệ giữa RatingTable hiện tại và các rating đã đánh giá
	// * Dựa trên lý thuyết hồi quy tuyến tính
	// * @param table
	// * @param sim
	// * @param minOverlap
	// * @return
	// */
	// public SimilarityTable.Similarity regressAgainst(
	// RatingTable table, SimilarityMeasure sim, int minOverlap)
	// {
	// // bước đầu tiên: Đọc dữ liệu, tính xbar, ybar
	// double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
	// int n = 0;
	// for (Rating r1 : table.getRatings()) {
	// Rating r2 = this.getMatchingRating(r1);
	// if (r2 != null) {
	// sumx += r1.score;
	// sumx2 += r1.score * r1.score;
	// sumy += r2.score;
	// n++;
	// }
	// }
	//
	// if (n < minOverlap)
	// return null;
	//
	// double xbar = sumx / n;
	// double ybar = sumy / n;
	//
	// // Tính các giá trị thống kê
	// double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
	// for (Rating r1 : table.getRatings()) {
	// Rating r2 = this.getMatchingRating(r1);
	// if (r2 != null) {
	// xxbar += (r1.score - xbar) * (r1.score - xbar);
	// yybar += (r2.score - ybar) * (r2.score - ybar);
	// xybar += (r1.score - xbar) * (r2.score - ybar);
	// }
	// }
	//
	//
	// double beta1 = xybar / xxbar;
	// double beta0 = ybar - beta1 * xbar;
	//
	// // Phân tích kết quả
	// double ssr = 0.0;
	// for (Rating r1 : table.getRatings()) {
	// Rating r2 = this.getMatchingRating(r1);
	// if (r2 != null) {
	// double fit = beta0;
	// fit += beta1*r1.score;
	// ssr += (fit - ybar) * (fit - ybar);
	// }
	// }
	//
	// double R2;
	//
	// // Tao du doan chung khi du các kết quả đạt yêu cầu
	// if (yybar == 0 || xxbar == 0) {
	// beta1 = 0;
	// beta0 = ybar;
	// R2 = 0;
	// }
	// else
	// R2 = ssr / yybar;
	//
	// double r = (beta1 > 0) ? Math.sqrt(R2) : 0;
	// if (sim == SimilarityMeasure.EUCLIDEAN)
	// return new SimilarityTable.Similarity(table.getName(),
	// getSimilarity(table, sim), beta1, beta0);
	//
	// return (beta1 <= 0) ? null : new
	// SimilarityTable.Similarity(table.getName(), r, beta1, beta0);
	// }

	/**
	 * @param rd
	 * @return
	 */
	// public double subtractBaseline(RatingDictionary rd)
	// {
	// double adjustments = 0;
	//
	// for (Rating r : getRatings()) {
	// double correction = rd.rawGeometricMean(r.rater, r.item);
	// adjustments += correction;
	// r.score = r.rawScore - correction;
	// }
	//
	// points -= adjustments;
	// return adjustments;
	// }

	/**
	 * @param rd
	 * @return
	 */
	// public double addBaseline(RatingDictionary rd)
	// {
	// double adjustments = 0;
	//
	// for (Rating r : getRatings()) {
	// double correction = rd.rawGeometricMean(r.rater, r.item);
	// adjustments += correction;
	// r.score = r.rawScore + correction;
	// }
	// points += adjustments;
	// return adjustments;
	// }

}
