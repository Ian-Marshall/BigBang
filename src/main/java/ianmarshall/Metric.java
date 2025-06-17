package ianmarshall;

/**
 * This class represents all the elements of the metric or fundamental tensor at all the points in space-time
 * under consideration.
 */
public class Metric
{
	public enum DerivativeLevel
	{
		None, FirstTime, FirstRadius, SecondTime, SecondRadius, FirstTimeFirstRadius;
	}

	private static final int S_N_ELEMENTS_RADIUS = 1000;
	private static final int S_N_ELEMENTS_TIME = 1000;
	private MetricComponents[][] m_aMetricComponents = null;    // 1st dimension: radius; 2nd dimension: time

	public Metric()
	{
		m_aMetricComponents = new MetricComponents[S_N_ELEMENTS_RADIUS][S_N_ELEMENTS_TIME];
	}

	/**
	* Make this method public when it is needed outside this class.
	*/
	private void zeroMetricComponents()
	{
		for (int r = 0; r < S_N_ELEMENTS_RADIUS; r++)
			for (int t = 0; t < S_N_ELEMENTS_TIME; t++)
				m_aMetricComponents[r][t] = new MetricComponents(0.0, 0.0, 0.0, 0.0, 0.0);
	}

	/**
	* Make this method public when it is needed outside this class.
	*/
	private Metric deepCopy()
	{
		Metric mResult = new Metric();

		for (int r = 0; r < S_N_ELEMENTS_RADIUS; r++)
			for (int t = 0; t < S_N_ELEMENTS_TIME; t++)
			{
				MetricComponents mc = m_aMetricComponents[r][t];
				mResult.m_aMetricComponents[r][t] = mc.copy();
			}

		return mResult;
	}
}
