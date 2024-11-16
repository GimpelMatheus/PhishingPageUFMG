package br.ufmg.utils;

public class Pair implements Comparable<Pair> {
	private final String x;
	private final float y;

	public Pair(String x,float req) {
		this.x = x;
		this.y = req;
	}

	public String firstValue() {
		return x;
	}

	public float secondValue() {
		return y;
	}

	@Override
	public int compareTo(Pair arg0) {
		if(y == arg0.y) {
			return 0;
		}else if(y > arg0.y) {
			return 1;
		}else{
			return -1;
		}
	}

}
