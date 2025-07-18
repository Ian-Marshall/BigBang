package ianmarshall;

import java.util.List;

/**
 * This class represents all the elements of the metric or fundamental tensor at all the points in space-time
 * under consideration.
 */
public class Metric
{
	private final int m_nRadiusElements;
	private final int m_nTimeElements;
	private final MetricComponents[][] m_aMetricComponents;    // 1st dimension: radius; 2nd dimension: time

	private Metric(int nRadiusElements, int nTimeElements, MetricComponents[][] aMetricComponents)
	{
		m_nRadiusElements = nRadiusElements;
		m_nTimeElements = nTimeElements;
		m_aMetricComponents = new MetricComponents[m_nRadiusElements][m_nTimeElements];

		for (int r = 0; r < m_nRadiusElements; r++)
			for (int t = 0; t < m_nTimeElements; t++)
				m_aMetricComponents[r][t] = aMetricComponents[r][t].copy();
	}

	public Metric(List<Double> liRadii, List<Double> liTimes)
	{
		m_nRadiusElements = liRadii.size();
		m_nTimeElements = liTimes.size();
		m_aMetricComponents = new MetricComponents[m_nRadiusElements][m_nTimeElements];

		for (int r = 0; r < m_nRadiusElements; r++)
		{
			double dblRadius = liRadii.get(r);

			for (int t = 0; t < m_nTimeElements; t++)
				m_aMetricComponents[r][t] = new MetricComponents(dblRadius, liTimes.get(t), 0.0, 0.0, 0.0);
		}
	}

	public Metric copy()
	{
		Metric mResult = new Metric(m_nRadiusElements, m_nTimeElements, m_aMetricComponents);
		return mResult;
	}

	public MetricComponents getMetricComponents(int nRIndex, int nTIndex)
	{
		if ((nRIndex < 0) || (nRIndex >= m_nRadiusElements) || (nTIndex < 0) || (nTIndex >= m_nTimeElements))
			throw new IndexOutOfBoundsException(String.format(
			 "At least one invalid metric component index: (nRIndex = %d, nTIndex = %d)", nRIndex, nTIndex));

		return m_aMetricComponents[nRIndex][nTIndex];
	}
}
