package ianmarshall;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * This class represents an element of the metric or fundamental tensor at a point in space-time.
 * For the Big Bang approximation, this point in space-time is given by the radius and time.
 */
public class MetricComponents
{
	/**
	 * This is private until it is used outside this class.
	 * It is used to identify the position of a <code>MetricComponent</code> in the metric tensor.
	*/
	private enum MetricPosition
	{
		R, T
	}

	public enum MetricComponent
	{
		A, B, D
	}

	/**
	 * These are the Ricci tensor components which can be non-zero.
	 * R33 is excluded since this is simply R22 * ((sin theta)^2).
	 */
	public enum RicciTensor
	{
		R00, R01, R11, R22
	}

	private double m_R = 0.0;    // The radius co-ordinate of the metric
	private double m_T = 0.0;    // The time co-ordinate of the metric
	private double m_A = 0.0;    // }
	private double m_B = 0.0;    // } The component values of the metric
	private double m_D = 0.0;    // }

	public MetricComponents(double r, double t, double a, double b, double d)
	{
		m_R = r;
		m_T = t;
		m_A = a;
		m_B = b;
		m_D = d;
	}

	public double getR()
	{
		return m_R;
	}

	public void setR(double r)
	{
		m_R = r;
	}

	public double getT()
	{
		return m_T;
	}

	public void setT(double t)
	{
		m_T = t;
	}

	public double getA()
	{
		return m_A;
	}

	public void setA(double a)
	{
		m_A = a;
	}

	public double getB()
	{
		return m_B;
	}

	public void setB(double b)
	{
		m_B = b;
	}

	public double getD()
	{
		return m_D;
	}

	public void setD(double d)
	{
		m_D = d;
	}

	public Entry<Double, Double> getComponent(MetricComponent mc)
	{
		double dbl = 0.0;

		switch (mc)
			{
				case A:
					dbl = getA();
					break;
				case B:
					dbl = getB();
					break;
				case D:
					dbl = getD();
					break;
				default:
					throw new IllegalArgumentException(String.format("Metric component \"%s\" not found.", mc.toString()));
			}

		Entry<Double, Double> entryResult = new SimpleEntry<>(Double.valueOf(getR()), Double.valueOf(dbl));
		return entryResult;
	}

	public void setComponent(MetricComponent mc, double dbl)
	{
		switch (mc)
		{
			case A:
				setA(dbl);
				break;
			case B:
				setB(dbl);
				break;
			case D:
				setD(dbl);
				break;
			default:
				throw new IllegalArgumentException(String.format("Metric component \"%s\" not found.", mc.toString()));
		}
	}

	public MetricComponents copy()
	{
		return new MetricComponents(getR(), getT(), getA(), getB(), getD());
	}

	/**
	 * Make a deep copy of a list of <code>MetricComponents</code>.
	 * @param liG
	 *   The list of <code>MetricComponents</code> to be copied.
	 *   If this is <code>null</code> then an empty list will be returned.
	 * @return
	 *   A deep copy of the list supplied.
	 */
	public static List<MetricComponents> deepCopyMetricComponents(List<MetricComponents> liG)
	{
		int nSize = liG != null ? liG.size() : 0;
		List<MetricComponents> liResult = new ArrayList<>(nSize);

		if (nSize > 0)
			for (MetricComponents mc: liG)
			{
				MetricComponents mcCopy = mc.copy();
				liResult.add(mcCopy);
			}

		return liResult;
	}
}
