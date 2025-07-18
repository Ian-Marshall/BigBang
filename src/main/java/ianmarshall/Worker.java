package ianmarshall;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import ianmarshall.MetricAndDerivatives.DerivativeLevel;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.None;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.FirstRadius;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.FirstTime;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.SecondRadius;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.SecondTime;
import static ianmarshall.MetricAndDerivatives.DerivativeLevel.FirstRadiusFirstTime;
import ianmarshall.MetricComponents.MetricComponent;
import static ianmarshall.MetricComponents.MetricComponent.A;
import static ianmarshall.MetricComponents.MetricComponent.B;
import static ianmarshall.MetricComponents.MetricComponent.D;
import ianmarshall.MetricComponents.MetricPosition;
import static ianmarshall.MetricComponents.MetricPosition.R;
import static ianmarshall.MetricComponents.MetricPosition.T;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Runnable
{
	public class WorkerUncaughtExceptionHandler implements UncaughtExceptionHandler
	{
		public WorkerUncaughtExceptionHandler()
		{
		}

		@Override
		public void uncaughtException(Thread t, Throwable th)
		{
			m_WorkerResult = new WorkerResult(m_bProcessingCompleted, th, m_nRun, m_liG);
			m_bStopped = true;
		}

	}

	// private static final double DBL_SUCCESS_LOG_PROBABILITY = 0.001;
	private static final Logger logger = LoggerFactory.getLogger(Worker.class);
	private static final StringBuilder s_sbMoveLog = new StringBuilder();    // Refactor this for multi-instance use
	private int m_nRun = 0;
	private int m_nRuns = 0;

	/**
	 * This holds the values of the metric tensor, and optionally their derivatives,
	 * each of which stores metric components for each value of radius and time.
	 */
	private MetricAndDerivatives m_madG = null;

	private boolean m_bFirstRun = true;    // This will also be true when resuming running after a pause
	private volatile boolean m_bStopping = false;
	private boolean m_bStopped = false;
	private boolean m_bProcessingCompleted = false;
	private WorkerUncaughtExceptionHandler m_wuehExceptionHandler = null;
	private WorkerResult m_WorkerResult = null;

	private SimulatedAnnealing m_saSimulatedAnnealing = null;
	private double m_dblEnergyCurrent = -1.0;

	/**
	 * The constructor.
	 * @param spStartParameters
	 *   The the application's start parameters.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means no run has yet been executed.
	 * @param madG
	 *   If not <code>null</code> then use this to set the metric tensor values, otherwise calculate the initial values.
	 */
	public Worker(StartParameters spStartParameters, int nRun, MetricAndDerivatives madG)
	{
		m_nRun = nRun;
		m_nRuns = spStartParameters.getNumberOfRuns();
		m_madG = madG;
		m_wuehExceptionHandler = new WorkerUncaughtExceptionHandler();
		m_saSimulatedAnnealing = new SimulatedAnnealing(spStartParameters);
	}

	public void stopExecution()
	{
		m_bStopping = true;
		logger.info(String.format("Stopping run number %s...", BigBangSimulatedAnnealing.formatInteger(m_nRun)));
	}

	/**
	 * @return
	 *   Whether working has stopped, whether all processing has been completed or not.
	 */
	public boolean getStopped()
	{
		return m_bStopped;
	}

	public WorkerUncaughtExceptionHandler getWorkerUncaughtExceptionHandler()
	{
		return m_wuehExceptionHandler;
	}

	public WorkerResult getWorkerResult()
	{
		return m_WorkerResult;
	}

	@Override
	public void run()
	{
		m_bStopping = false;
		m_bStopped = false;

		while ((!m_bStopping) && (m_nRun < m_nRuns))
		{
			m_nRun++;
	 // logger.info(String.format("Started run number %s.", BigBangSimulatedAnnealing.formatInteger(m_nRun)));

			if (m_bFirstRun)
			{
				if (m_madG == null)
					m_madG = initialiseMetricTensors();

				calculateAllDifferentialsForAllValues(m_madG);

				// The current energy has not been calculated yet
				m_dblEnergyCurrent = m_saSimulatedAnnealing.energy(m_liG, m_liGFirstDerivative, m_liGSecondDerivative, m_nRun);

				m_bFirstRun = false;
			}

			List<MetricComponents> liGNew = m_saSimulatedAnnealing.neighbour(m_liG);
			List<MetricComponents> liGNewFirstDerivative = MetricComponents.deepCopyMetricComponents(m_liGFirstDerivative);
			List<MetricComponents> liGNewSecondDerivative = MetricComponents.deepCopyMetricComponents(m_liGSecondDerivative);
			calculateAllDifferentialsForAllValues(liGNew, liGNewFirstDerivative, liGNewSecondDerivative);
			double dblEnergyNew = m_saSimulatedAnnealing.energy(liGNew, liGNewFirstDerivative, liGNewSecondDerivative,
			 m_nRun);
			double dblTemperature = m_saSimulatedAnnealing.temperature(m_nRun, m_nRuns);
			double dblProbability = m_saSimulatedAnnealing.acceptanceProbability(m_dblEnergyCurrent, dblEnergyNew,
			 dblTemperature);
			boolean bAcceptMove = Math.random() < dblProbability;
			String sLogEntry = null;
	 // bAcceptMove = false;    // Delete this line

			if (bAcceptMove)
			{
		 // if (Math.random() < DBL_SUCCESS_LOG_PROBABILITY)
		 // {
					sLogEntry = String.format("Run number %s:"
					 + "    ***  Accepted move from energy %f to %f at temperature %f with probability %.5f.  ***",
					 BigBangSimulatedAnnealing.formatInteger(m_nRun), m_dblEnergyCurrent, dblEnergyNew, dblTemperature,
					 dblProbability);

					int nStartLength = s_sbMoveLog.length();
					s_sbMoveLog.append(String.format("%n  run %d: %s", m_nRun, sLogEntry));

					if (nStartLength == 0)
					{
						String sRemove = String.format("%n");
						int nLengthRemove = sRemove.length();
						s_sbMoveLog.delete(0, nLengthRemove);
					}
		 // }

				m_liG = liGNew;
				m_liGFirstDerivative = liGNewFirstDerivative;
				m_liGSecondDerivative = liGNewSecondDerivative;
				m_dblEnergyCurrent = dblEnergyNew;

		 // String sLogMessage = m_saSimulatedAnnealing.popLatestLogMessage();
		 // logger.info(sLogMessage);
			}
			else if (dblProbability >= 0.5)
			{
				sLogEntry = String.format("Run number %s:"
				 + " rejected move from energy %f to %f with probability %.5f.",
				 BigBangSimulatedAnnealing.formatInteger(m_nRun), m_dblEnergyCurrent, dblEnergyNew, dblProbability);

		 // sLogEntry = String.format("%n***  Remove the setting of bAcceptMove to false.  ***");
			}

			if (sLogEntry == null)
				sLogEntry = String.format("Run number %s: (pre-move) energy = %f.",
				 BigBangSimulatedAnnealing.formatInteger(m_nRun), m_dblEnergyCurrent);

			if (sLogEntry != null)
			{
				logger.info(sLogEntry);
		 // logger.info(String.format("Completed run number %s with current energy %f.",
		 //  BigBangSimulatedAnnealing.formatInteger(m_nRun), m_dblEnergyCurrent));
			}

	 // logger.info(String.format("Completed run number %s with current energy %f.",
	 //  BigBangSimulatedAnnealing.formatInteger(m_nRun), m_dblEnergyCurrent));
		}

		logger.info(String.format("Move log is:%n%s", s_sbMoveLog));
		reportFinalTensorValues();

		if (m_nRun >= m_nRuns)
		{
			s_sbMoveLog.setLength(0);
			m_bProcessingCompleted = true;
			logger.info("All processing has been completed.");
		}
		else
			logger.info("Stopped before all processing completed.");

		m_WorkerResult = new WorkerResult(m_bProcessingCompleted, null, m_nRun, m_liG);
		m_bStopped = true;
	}

	/**
	 * Initialise the metric tensor, and various derivatives with respect to radius and time,
	 * with start values for graduated radius and time values.
	 */
	private MetricAndDerivatives initialiseMetricTensors()
	{
		List<Double> liRadii = new ArrayList<>();
		List<Double> liTimes = new ArrayList<>();

		StringBuilder sbLog = new StringBuilder("Initialising the metric components (a selection is shown)...");
		String sIndent = " ".repeat(72);

		sbLog.append(String.format(
			 "%n%1$sindex                   T                   R                   A                   B                   D"
		 + "%n%1$s-----  ------------------  ------------------  ------------------  ------------------  ------------------",
		 sIndent));

		String sFormat = "%n" + sIndent + "%5d  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f";

		final double DBL_R_MIN = 1.01;
		final double DBL_R_MAX = 100.0;
		final double DBL_STEP_FACTOR_RADIUS = 1.014;

		final double DBL_T_MIN = 0.0;
		final double DBL_STEP_TIME = 1.0;

		double dblR = DBL_R_MIN;
		double dblT = DBL_T_MIN;
		int i = 0;
		boolean bLoop = true;
		boolean bOneMoreLoop = false;

		while (bLoop)
		{
			if (bOneMoreLoop)
				bLoop = false;

			double dblA =  1.0;
			double dblB =  -1.0;
			double dblD =  1.0;    // Let us try first having D positive (it could turn out to be negative instead)
			liRadii.add(dblR);
			liTimes.add(dblT);

			if ((i >= 662) || ((i % 100) == 0))
				sbLog.append(String.format(sFormat, i, dblT, dblR, dblA, dblB, dblD));

			double dblRNew = ((dblR  - 1.0) * DBL_STEP_FACTOR_RADIUS) + 1.0;

			if (dblRNew < DBL_R_MAX)
				dblR = dblRNew;
			else if (dblR < DBL_R_MAX)
			{
				// We shall loop once more only, and then not rely on comparison precision
				dblR = DBL_R_MAX;
				bOneMoreLoop = true;
			}
			else
				bLoop = false;

			dblT += DBL_STEP_TIME;
			i++;
		}

		MetricAndDerivatives madResult = buildMetricAndDerivatives(liRadii, liTimes);
		logger.info(sbLog.toString());
		logger.info("The metric components have been initialised.");
		return madResult;
	}

	/**
	 * Build an initialised metric and its derivatives from the supplied radius and time values.
	 * @param liRadii
	 *   The list of radius values to be used for the metric tensor components.
	 * @param liTimes
	 *   The list of time values to be used for the metric tensor components.
	 * @return
	 *   A <code>MetricAndDerivatives</code> object holding the initialised metric and its derivatives.
	 */
	private MetricAndDerivatives buildMetricAndDerivatives(List<Double> liRadii, List<Double> liTimes)
	{
		MetricAndDerivatives madResult = new MetricAndDerivatives(liRadii, liTimes);
		return madResult;
	}

	/**
	 * Calculate the first- and second-order differentials of all the metric tensor components
	 * with respect to radius and/or time.
	 * <br>
	 * All of the parameters must be not <code>null</code> and contain the same number of elements
	 * for the same radius and time values. This number of elements must be at least 5.
	 * @param madG
	 *   The metric tensor components and its derivatives.
	 */
	private void calculateAllDifferentialsForAllValues(MetricAndDerivatives madG)
	{
		int nRadiusElements = madG.getNRadiusElements();
		int nTimeElements   = madG.getNTimeElements();

		for (DerivativeLevel dlDerivativeLevel: DerivativeLevel.values())
			if (dlDerivativeLevel != None)
			{
				MetricPosition mpVarying;
				switch(dlDerivativeLevel)
				{
					case FirstRadius:
					case SecondRadius:
						mpVarying = R;
						break;
					case FirstTime:
					case SecondTime:
					case FirstRadiusFirstTime:
						// It does not matter whether we use R or T here, but we must be consistent with the code lower down
						mpVarying = T;
						break;
					default:
						throw new RuntimeException(String.format("Invalid derivative level \"%s\".", dlDerivativeLevel.toString()));
				}

				for (MetricComponent mcMetricComponent: MetricComponent.values())
					for (int nRIndex = 0; nRIndex < nRadiusElements; nRIndex++)
						for (int nTIndex = 0; nTIndex < nTimeElements; nTIndex++)
							calculateDifferentialOfMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpVarying,
							 mcMetricComponent);
			}
	}

	/**
	 * Calculate the specified level of differential of the specified metric component and store it
	 * in the <code>MetricAndDerivatives</code> object supplied.
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain at least 3 elements
	 * for each space-time dimension (time and radius here).
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be calculated.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component, the differential of which is to be calculated.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component, the differential of which is to be calculated.
	 * @param mpVarying
	 *   The metric position, the varying of the value at which is to be calculated.
	 * @param mcMetricComponent
	 *   The metric component, the differential of which is to be calculated.
	 */
	private void calculateDifferentialOfMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex, MetricPosition mpVarying, MetricComponent mcMetricComponent)
	{
		double dblValue = differentialOfMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpVarying,
		 mcMetricComponent);
		setMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpVarying, mcMetricComponent, dblValue);
	}

	/**
	 * Calculate the specified level of differential of the specified metric component.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be calculated.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component, the differential of which is to be calculated.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component, the differential of which is to be calculated.
	 * @param mpVarying
	 *   The metric position, the varying of the value at which is to be calculated.
	 * @param mcMetricComponent
	 *   The metric component, the differential of which is to be calculated.
	 * @return
	 *   The specified level of differential of the specified metric component.
	 */
	private double differentialOfMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex, MetricPosition mpVarying, MetricComponent mcMetricComponent)
	{
		double dblResult = 0.0;

		DerivativeLevel dlGetting;
		switch(dlDerivativeLevel)
		{
			case FirstRadius:
			case SecondRadius:
			case FirstTime:
			case SecondTime:
				dlGetting = None;
				break;
			case FirstRadiusFirstTime:
				// We must be consistent with whether we vary R or T in this case. The statement below does this automatically.
				dlGetting = mpVarying == T ? FirstRadius : FirstTime;
				break;
			default:
				throw new RuntimeException(String.format("Invalid derivative level \"%s\".", dlDerivativeLevel.toString()));
		}

		int nVaryingIndex = -1;
		int nVaryingMaxIndex = -1;
		switch (mpVarying)
		{
			case R:
				nVaryingIndex = nRIndex;
				nVaryingMaxIndex = madG.getNRadiusElements() - 1;
				break;
			case T:
				nVaryingIndex = nTIndex;
				nVaryingMaxIndex = madG.getNTimeElements() - 1;
				break;
		}

		// The middle elements (index 1) are those of the point, the derivatives of which are to be calculated.
		// This may be different from the index supplied if it is the first or last point.
		// In these cases, we shall use forward and backward differences, respectively, instead.
		double[] adblPos = new double[3];
		double[] adblComponent = new double[3];

		final int nVaryingStart;
		if (nVaryingIndex == 0)
			nVaryingStart = nVaryingIndex;        // Forward difference for the first point
		else if (nVaryingIndex < nVaryingMaxIndex)
			nVaryingStart = nVaryingIndex - 1;    // Central difference for an internal point
		else
			nVaryingStart = nVaryingIndex - 2;    // Backward difference for the last point

		final int nVaryingFinish = nVaryingStart + 2;
		int n = 0;    // The array index

		// Load the arrays
		for (int i = nVaryingStart; i <= nVaryingFinish; i++)
		{
			switch (mpVarying)
			{
				case R:
					nRIndex = i;
					break;
				case T:
					nTIndex = i;
					break;
			}

			Entry<Double, Double> entry = getMetricComponent(madG, dlGetting, nRIndex, nTIndex, mpVarying, mcMetricComponent);
			adblPos[n] = entry.getKey().doubleValue();
			adblComponent[n] = entry.getValue().doubleValue();
			n++;
		}

		double dblFirstDifferentialNext = (adblComponent[2] - adblComponent[1]) / (adblPos[2] - adblPos[1]);
		double dblFirstDifferentialPrev = (adblComponent[1] - adblComponent[0]) / (adblPos[1] - adblPos[0]);

		switch(dlDerivativeLevel)
		{
			case FirstRadius:
			case FirstTime:
			case FirstRadiusFirstTime:
				dblResult = 0.5 * (dblFirstDifferentialNext + dblFirstDifferentialPrev);
				break;
			case SecondRadius:
			case SecondTime:
				dblResult = 2.0 * (dblFirstDifferentialNext - dblFirstDifferentialPrev) / (adblComponent[2] - adblComponent[0]);
				break;
			default:    // For example: None
				throw new RuntimeException(String.format("Invalid differentiation request for:"
				 + "%n  dlDerivativeLevel = %s,"
				 + "%n  nRIndex           = %d,"
				 + "%n  nTIndex           = %d,"
				 + "%n  mpVarying         = %s,"
				 + "%n  mcMetricComponent = %s,"
				 + "%n  dlGetting         = %s.",
				 dlDerivativeLevel.toString(), nRIndex, nTIndex, mpVarying.toString(), mcMetricComponent.toString(),
				 dlGetting.toString()));
		}

		return dblResult;
	}

	/**
	 * Get the value of the specified component of the specified level of differential of the specified indices
	 * at the specified metric position from the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be found.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be found.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be found.
	 * @param mpMetricPosition
	 *   The metric position to be found.
	 * @param mcMetricComponent
	 *   The metric component to be found.
	 * @return
	 *   An <code>Entry</code> with:
	 *   <ul>
	 *     <li>key: the value of the specified metric position</li>
	 *     <li>value: the value of the specified component of the specified level of differential of the specified index.
	 *     </li>
	 *   </ul>
	 */
	public static Entry<Double, Double> getMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex, MetricPosition mpMetricPosition, MetricComponent mcMetricComponent)
	{
		MetricComponents mcMetricComponents = getMetricComponents(madG, dlDerivativeLevel, nRIndex, nTIndex);
		return mcMetricComponents.getComponent(mpMetricPosition, mcMetricComponent);
	}

	/**
	 * Set the value of the specified component of the specified level of differential of the specified indices
	 * at the specified metric position in the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level of the value to be set.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be set.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be set.
	 * @param mpMetricPosition
	 *   The metric position of the value to be set.
	 * @param mcMetricComponent
	 *   The metric component of the value to be set.
	 * @param dblValue
	 *   The value to be set.
	 */
	public static void setMetricComponent(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel, int nRIndex,
	 int nTIndex, MetricPosition mpMetricPosition, MetricComponent mcMetricComponent, double dblValue)
	{
		Entry<Double, Double> entry = getMetricComponent(madG, dlDerivativeLevel, nRIndex, nTIndex, mpMetricPosition,
		 mcMetricComponent);
		entry.setValue(Double.valueOf(dblValue));
	}

	/**
	 * Obtain the <code>MetricComponents</code> for the given parameters.
	 *
	 * Get the <code>MetricComponents</code> of the specified level of differential of the specified index
	 * from the <code>MetricAndDerivatives</code> supplied.
	 * @param madG
	 *   The metric tensor components and its derivatives, in order of ascending adjacent radius and time values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be found.
	 * @param nRIndex
	 *   The zero-based index value of the radius of the metric component to be found.
	 * @param nTIndex
	 *   The zero-based index value of the time of the metric component to be found.
	 * @return
	 *   The <code>MetricComponents</code>.
	 */
	private static MetricComponents getMetricComponents(MetricAndDerivatives madG, DerivativeLevel dlDerivativeLevel,
	 int nRIndex, int nTIndex)
	{
		Metric mMetric = madG.getMetric(dlDerivativeLevel);
		return mMetric.getMetricComponents(nRIndex, nTIndex);
	}

	/*
	 * Calculate the Jacobian matrix (values) at the given point in space-time (the radius).
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the
	 * same number of elements for the same radius values.
	 * This number of elements must be at least 5.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param nIndex
	 *   The zero-based index of the metric component of the point in space-time (the radius) to be used.
	 * @return
	 *   The Jacobian matrix (values) at the given point in space-time (the radius).
	 */
	/*
	private DoubleMatrix2D calculateJacobianMatrixValues(List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative, List<MetricComponents> liGSecondDerivative, int nIndex)
	{
		Entry<Double, Double> entry = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative,
		 None, nIndex, A);
		double dblR = entry.getKey().doubleValue();
		double dblA = entry.getValue().doubleValue();

		double dblB = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, None, nIndex, B).
		 getValue().doubleValue();

		double dAdR = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, A)
		 .getValue().doubleValue();

		double dBdR = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, B)
		 .getValue().doubleValue();

		double d2AdR2 = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, Second, nIndex, A)
		 .getValue().doubleValue();

		double dR00dA = (1 / (4.0 * dblA * dblA * dblB)) * dAdR * dAdR;
		double dR00dB = -((1 / (dblB * dblB * dblR)) * dAdR) + ((1 / (4.0 * dblA * dblB * dblB)) * dAdR * dAdR)
		 + ((1 / (2.0 * dblB * dblB * dblB)) * dAdR * dBdR) - ((1 / (2.0 * dblB * dblB)) * d2AdR2);

		double dR11dA = ((1 / (4.0 * dblA * dblA * dblB)) * dAdR * dBdR) + ((1 / (2.0 * dblA * dblA * dblA)) * dAdR * dAdR)
		 - ((1 / (2.0 * dblA * dblA)) * d2AdR2);
		double dR11dB = ((1 / (dblB * dblB * dblR)) * dBdR) + ((1 / (4.0 * dblA * dblB * dblB)) * dAdR * dBdR);

		double dR22dA = ((dblR / (2 * dblA * dblA * dblB)) * dAdR);
		double dR22dB = (1 / (dblB * dblB)) + ((dblR / (2 * dblA * dblB * dblB)) * dAdR)
		 - ((dblR / (dblB * dblB * dblB)) * dBdR);

		DoubleMatrix2D dmResult = DoubleFactory2D.dense.make(3, 2);
		dmResult.set(0, 0, dR00dA);
		dmResult.set(1, 0, dR11dA);
		dmResult.set(2, 0, dR22dA);
		dmResult.set(0, 1, dR00dB);
		dmResult.set(1, 1, dR11dB);
		dmResult.set(2, 1, dR22dB);
		return dmResult;
	}
	*/

	/**
	 * Calculate the Ricci tensor values at the given point in space-time (the radius).
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the
	 * same number of elements for the same radius values.
	 * This number of elements must be at least 5.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param nIndex
	 *   The zero-based index of the metric component of the point in space-time (the radius) to be used.
	 * @return
	 *   The Ricci tensor values at the given point in space-time (the radius) as the vector (1-D column matrix):
	 *   <code>(R00, R11, R22)T</code>.
	 */
	public static DoubleMatrix2D calculateRicciTensorValues(List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative, List<MetricComponents> liGSecondDerivative, int nIndex)
	{
		Entry<Double, Double> entry = getMetricComponent(liG, liGFirstDerivative, liGSecondDerivative,
		 None, nIndex, A);
		double dblR = entry.getKey().doubleValue();
		double dblA = entry.getValue().doubleValue();

		double dblB = getMetricComponent(liG, liGFirstDerivative, liGSecondDerivative, None, nIndex, B).
		 getValue().doubleValue();

		double dAdR = getMetricComponent(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, A)
		 .getValue().doubleValue();

		double dBdR = getMetricComponent(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, B)
		 .getValue().doubleValue();

		double d2AdR2 = getMetricComponent(liG, liGFirstDerivative, liGSecondDerivative, Second, nIndex, A)
		 .getValue().doubleValue();

		double dblR00 = ((1.0 / (dblB * dblR)) * dAdR)
		 - ((1.0 / (4.0 * dblA * dblB)) * dAdR * dAdR)
		 - ((1.0 / (4.0 * dblB * dblB)) * dAdR * dBdR)
		 + ((1.0 / (2.0 * dblB)) * d2AdR2);

		double dblR11 = -((1.0 / (dblB * dblR)) * dBdR)
		 - ((1.0 / (4.0 * dblA * dblB)) * dAdR * dBdR)
		 - ((1.0 / (4.0 * dblA * dblA)) * dAdR * dAdR)
		 + ((1.0 / (2.0 * dblA)) * d2AdR2);

		double dblR22 = -1.0 - (1.0 / dblB)
		 - ((dblR / (2.0 * dblA * dblB)) * dAdR)
		 + ((dblR / (2.0 * dblB * dblB)) * dBdR);

		DoubleMatrix2D dvResult = DoubleFactory2D.dense.make(3, 1);
		dvResult.set(0, 0, dblR00);
		dvResult.set(1, 0, dblR11);
		dvResult.set(2, 0, dblR22);
		return dvResult;
	}

	private void reportFinalTensorValues()
	{
		logger.info(String.format("The metric components (in the format \"index, r, A, B\") after the final run are:"));
		StringBuilder sbLog = new StringBuilder();

		// For use in CSV format
 // sbLog.append(String.format(
 // 	 "%n      i,                  R,                  A,                  B,              dA/dR,              dB/dR,            d2A/dR2,            d2B/dR2"
 //  + "%n"));

		sbLog.append(String.format(
			 "%n      i                   R                   A                   B               dA/dR               dB/dR             d2A/dR2             d2B/dR2"
		 + "%n  -----  ------------------  ------------------  ------------------  ------------------  ------------------  ------------------  ------------------"));

		for (int i = 0; i < m_liG.size(); i++)
		{
			Entry<Double, Double> entry = getMetricComponent(m_liG, null, null, None, i, A);
			double dblR = entry.getKey().doubleValue();
			double dblA = entry.getValue().doubleValue();
			double dblB = getMetricComponent(m_liG, null, null, None, i, B).getValue().doubleValue();

			double dAdR = getMetricComponent(
			 m_liG, m_liGFirstDerivative, m_liGSecondDerivative, First, i, A).getValue().doubleValue();
			double dBdR = getMetricComponent(
			 m_liG, m_liGFirstDerivative, m_liGSecondDerivative, First, i, B).getValue().doubleValue();
			double d2AdR2 = getMetricComponent(
			 m_liG, m_liGFirstDerivative, m_liGSecondDerivative, Second, i, A).getValue().doubleValue();
			double d2BdR2 = getMetricComponent(
			 m_liG, m_liGFirstDerivative, m_liGSecondDerivative, Second, i, B).getValue().doubleValue();

	 // String sFormat = "%n  %5d, %,18.12f, %,18.12f, %,18.12f, %,18.12f, %,18.12f, %,18.12f, %,18.12f";    // For use in CSV format
			String sFormat = "%n  %5d  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f  %,18.12f";

			sbLog.append(String.format(sFormat, i, dblR, dblA, dblB, dAdR, dBdR, d2AdR2, d2BdR2));
		}

		logger.info(sbLog.toString());
	}
}
