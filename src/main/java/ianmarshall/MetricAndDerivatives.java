package ianmarshall;

import java.util.EnumMap;
import java.util.List;

import static ianmarshall.MetricAndDerivatives.DerivativeLevel.None;

public class MetricAndDerivatives
{
	/**
	 * The ordering of these elements matters. Pure first derivatives must be placed before pure second derivatives
	 * because the pure first derivatives are calculated before the pure second derivatives.
	 */
	public enum DerivativeLevel
	{
		None, FirstRadius, FirstTime, SecondRadius, SecondTime, FirstRadiusFirstTime;
	}

	/**
	 * These are the Ricci tensor components which can be non-zero.
	 * R33 is excluded since this is simply R22 * ((sin theta)^2).
	 */
	public enum RicciTensor
	{
		R00, R01, R11, R22
	}

	private final int m_nRadiusElements;
	private final int m_nTimeElements;
	private final EnumMap<DerivativeLevel, Metric> m_mapMetrics;

	/**
	 * Build an initialised metric and its derivatives from the supplied radius and time values.
	 * "Initialised" here means that the metric tensor components are set to zero.
	 * @param liRadii
	 *   The list of radius values to be used for the metric tensor components.
	 * @param liTimes
	 *   The list of time values to be used for the metric tensor components.
	 * @return
	 *   A <code>MetricAndDerivatives</code> object holding the initialised metric and its derivatives.
	 */
	public MetricAndDerivatives(List<Double> liRadii, List<Double> liTimes)
	{
		m_nRadiusElements = liRadii.size();
		m_nTimeElements = liTimes.size();
		m_mapMetrics = new EnumMap<>(DerivativeLevel.class);
		Metric metricNonDerivative = new Metric(liRadii, liTimes);
		m_mapMetrics.put(None, metricNonDerivative);
		initialiseDerivativeMetrics(metricNonDerivative);
	}

	public int getNRadiusElements()
	{
		return m_nRadiusElements;
	}

	public int getNTimeElements()
	{
		return m_nTimeElements;
	}

	/*
	public MetricAndDerivatives(int nRadiusElements, int nTimeElements)
	{
		this(nRadiusElements, nTimeElements, null);
	}
	*/

	/*
	public MetricAndDerivatives(int nRadiusElements, int nTimeElements, Metric metricNonDerivative)
	{
		m_nRadiusElements = nRadiusElements;
		m_nTimeElements = nTimeElements;
		m_mapMetrics = new EnumMap<>(DerivativeLevel.class);

		if (metricNonDerivative != null)
			m_mapMetrics.put(None, metricNonDerivative);

		initialiseDerivativeMetrics();
	}
	*/

	/**
	 * Build initialised metric derivatives from the supplied non-derivative metric.
	 * "Initialised" here means that the metric tensor components are set to zero.
	 * @param metricNonDerivative
	 *   A <code>MetricAndDerivatives</code> object holding the initialised non-derivative metric.
	 */
	private void initialiseDerivativeMetrics(Metric metricNonDerivative)
	{
		for (DerivativeLevel level: DerivativeLevel.values())
			if (level != None)
			{
				Metric metric = metricNonDerivative.copy();
				m_mapMetrics.put(level, metric);
			}
	}

	public Metric getMetric(DerivativeLevel level)
	{
		return m_mapMetrics.get(level);
	}
}
