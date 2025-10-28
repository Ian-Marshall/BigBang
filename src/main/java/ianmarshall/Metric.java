package ianmarshall;

/**
 * This class represents all the elements of the metric or fundamental tensor at all the points in space-time
 * under consideration.
 */
public class Metric
{
	private final int m_nRadiusElements;
	private final int m_nTimeElements;
	private final MetricComponents[][] m_aMetricComponents;    // 1st dimension: radius; 2nd dimension: time

	/**
	 * Build a metric from the supplied radius and time values,
	 * with all the <code>MetricComponent</code>s of each <code>MetricComponents</code> initialised to zero.
	 * @param adblRadii
	 *   The array of radius values to be used for the metric tensor components.
	 * @param adblTimes
	 *   The array of time values to be used for the metric tensor components.
	 */
	public Metric(Double[] adblRadii, Double[] adblTimes)
	{
		m_nRadiusElements = adblRadii.length;
		m_nTimeElements = adblTimes.length;
		m_aMetricComponents = new MetricComponents[m_nRadiusElements][m_nTimeElements];

		for (int r = 0; r < m_nRadiusElements; r++)
		{
			double dblRadius = adblRadii[r].doubleValue();

			for (int t = 0; t < m_nTimeElements; t++)
				m_aMetricComponents[r][t] = new MetricComponents(dblRadius, adblTimes[t].doubleValue(), 0.0, 0.0, 0.0, 0.0);
		}
	}

	private Metric(int nRadiusElements, int nTimeElements, MetricComponents[][] aMetricComponents)
	{
		m_nRadiusElements = nRadiusElements;
		m_nTimeElements = nTimeElements;
		m_aMetricComponents = new MetricComponents[m_nRadiusElements][m_nTimeElements];

		for (int r = 0; r < m_nRadiusElements; r++)
			for (int t = 0; t < m_nTimeElements; t++)
				m_aMetricComponents[r][t] = aMetricComponents[r][t].copy();
	}

	public MetricComponents getMetricComponents(int nRIndex, int nTIndex)
	{
 // if ((nRIndex < 0) || (nRIndex >= m_nRadiusElements) || (nTIndex < 0) || (nTIndex >= m_nTimeElements))
 // 	throw new IndexOutOfBoundsException(String.format("At least one invalid metric component index:"
 // 	 + " (nRIndex = %d (should be 0 to %d inclusive), nTIndex = %d (should be 0 to %d inclusive))",
 // 	 nRIndex, m_nRadiusElements - 1, nTIndex, m_nTimeElements - 1));

		return m_aMetricComponents[nRIndex][nTIndex];
	}

	public Metric copy()
	{
		Metric mResult = new Metric(m_nRadiusElements, m_nTimeElements, m_aMetricComponents);
		return mResult;
	}
}
