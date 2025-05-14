package ianmarshall;

import java.util.Collections;
import java.util.List;

public class WorkerResult
{
	private boolean m_bProcessingCompleted = false;
	private Throwable m_thThrowable = null;
	private int m_nRun = 0;
	private List<MetricComponents> m_liG = null;

	public WorkerResult(boolean bProcessingCompleted, Throwable thThrowable, int nRun, List<MetricComponents> liG)
	{
		m_bProcessingCompleted = bProcessingCompleted;
		m_thThrowable = thThrowable;
		m_nRun = nRun;
		m_liG = MetricComponents.deepCopyMetricComponents(liG);
	}

	public boolean getProcessingCompleted()
	{
		return m_bProcessingCompleted;
	}

	public Throwable getThrowable()
	{
		return m_thThrowable;
	}

	public int getRun()
	{
		return m_nRun;
	}

	public List<MetricComponents> getMetricComponentsList()
	{
		return Collections.unmodifiableList(m_liG);
	}
}
