
public class PlayerG {

	private double[] weights;

	public int pickMove(State s, int[][] legalMoves) {

		double maxVal = getValue(s,0);
		int maxMove = 0;

		for (int i = 1; i < legalMoves.length; i++){
			double currVal = getValue(s,i);
			if (currVal > maxVal){
				maxVal = currVal;
				maxMove = i;
			}

		}
		
		return maxMove;
	}

	public int[] getFeatures(State s, int action){
		return s.tryMove(action);
	}

	public double getValue(int[] features){
		if (features == null){
			return Double.NEGATIVE_INFINITY;
		}
		double result = 0;
		for (int i = 0; i < weights.length; i++){
			result += weights[i]*features[i];
		}
		return result;
	}

	public double getValue(State s, int action){
		int[] features = s.tryMove(action);
		return getValue(features);
	}

	public void setWeights(double[] w){
		weights = new double[w.length];
		for (int i = 0; i < w.length; i++){
			weights[i] = w[i];
		}
	}

}
