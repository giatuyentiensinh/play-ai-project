package models;

import java.util.Collection;
import java.util.Hashtable;

/**
 * @author tuananh
 *
 */
public class RatingData {

	/**
	 * 
	 * @author tuananh
	 */

	private Hashtable<String, RatingTable> raterData;
	private Hashtable<String, RatingTable> itemData;

	private double scoreTotals;
	private double logTotals;
	private int numRatings;

	/**
	 * Constructor
	 */
	public RatingData() {
		raterData = new Hashtable<String, RatingTable>();
		itemData = new Hashtable<String, RatingTable>();

		scoreTotals = 0;
		logTotals = 0;
		numRatings = 0;
	}

	/**
	 * @return
	 */
	public Collection<RatingTable> getRaters() {
		return raterData.values();
	}

	/**
	 * @return
	 */
	public Collection<RatingTable> getItems() {
		return itemData.values();
	}

	/**
	 * @param raterName
	 * @return
	 */
	public RatingTable getRater(String raterName) {
		return raterData.get(raterName);
	}

	/**
	 * @param itemName
	 * @return
	 */
	public RatingTable getItem(String itemName) {
		return itemData.get(itemName);
	}

	/**
	 * @param raterName
	 * @param itemName
	 * @return
	 */
	public double getRating(String raterName, String itemName) {
		if (raterName == null || itemName == null)
			return -1.0;
		RatingTable c = raterData.get(raterName);
		if (c == null)
			return -1.0;
		Rating r1 = c.getRatingFor(itemName);
		if (r1 == null)
			return -1.0;
		else
			return r1.score;
	}

	/**
	 * @param raterName
	 * @return
	 */
	public Collection<Rating> getRaterRatings(String raterName) {
		if (raterName == null)
			return null;
		RatingTable c = raterData.get(raterName);
		if (c == null)
			return null;
		return c.getRatings();
	}

	/**
	 * @param c
	 */
	public void addRater(RatingTable c) {
		if (c != null && !raterData.containsKey(c.getName()))
			raterData.put(c.getName(), c);
	}

	/**
	 * @param m
	 */
	public void addItem(RatingTable m) {
		if (m != null && !itemData.containsKey(m.getName()))
			itemData.put(m.getName(), m);
	}

	/**
	 * @param r
	 */
	public void addRating(Rating r) {
		String raterName = r.rater;
		String itemName = r.item;

		RatingTable c = raterData.get(raterName);
		if (c == null) {
			c = new RatingTable(raterName, RatingTable.CommonAttribute.RATER);
			addRater(c);
		}
		c.addRating(r);

		RatingTable m = itemData.get(itemName);
		if (m == null) {
			m = new RatingTable(itemName, RatingTable.CommonAttribute.ITEM);
			addItem(m);
		}
		m.addRating(r);

		numRatings++;
		scoreTotals += r.score;
		logTotals += Math.log(r.score);
	}

	/**
	 * @param t
	 * @param fold
	 * @param numFolds
	 */
	public void addTrainingData(RatingTable t, int fold, int numFolds) {
		for (Rating r : t.getRatings()) {
			if (numFolds <= 1 || r.seq % numFolds != fold)
				addRating(r);
		}
	}

	public double defaultScore() {
		return scoreTotals / numRatings;
	}

	/**
	 * @param rater
	 * @param item
	 * @return
	 */
	public double rawGeometricMean(String rater, String item) {
		RatingTable r = raterData.get(rater);
		if (r == null)
			return defaultScore();
		RatingTable i = itemData.get(item);
		if (i == null)
			return defaultScore();
		double f1 = r.getFactor();
		double f2 = i.getFactor();
		double al = logTotals / numRatings;
		return Math.exp(f1 + f2 - al);
	}

	/**
	 * @param rater
	 * @param item
	 * @return
	 */
	public double geometricMeanBaseline(String rater, String item) {
		return rawGeometricMean(rater, item);
	}
}
