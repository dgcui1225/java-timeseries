package timeseries.models;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.markers.None;

import com.google.common.primitives.Doubles;

import stats.distributions.Normal;
import timeseries.TimeSeries;

public final class RandomWalkForecast implements Forecast {

  private final Model model;
  private final TimeSeries forecast;
  private final TimeSeries upperInterval;
  private final TimeSeries lowerInterval;
  private final int steps;
  private final double alpha;

  public RandomWalkForecast(final Model model, final int steps, final double alpha) {
    this.model = model;
    this.forecast = model.pointForecast(steps);
    this.alpha = alpha;
    this.steps = steps;
    this.upperInterval = upperPredictionInterval(steps, alpha);
    this.lowerInterval = lowerPredictionInterval(steps, alpha);
  }

  /* (non-Javadoc)
   * @see timeseries.models.Forecast#upperPredictionInterval(int, double)
   */
  @Override
  public final TimeSeries upperPredictionInterval(final int steps, final double alpha) {
    double[] upperPredictionValues = new double[steps];
    double criticalValue = new Normal(0, model.residuals().stdDeviation()).quantile(1 - alpha / 2);
    for (int t = 0; t < steps; t++) {
      upperPredictionValues[t] = forecast.at(t) + criticalValue * Math.sqrt(t + 1);
    }
    return new TimeSeries(forecast.timeScale(), forecast.observationTimes().get(0), forecast.periodLength(),
        upperPredictionValues);
  }

  /* (non-Javadoc)
   * @see timeseries.models.Forecast#lowerPredictionInterval(int, double)
   */
  @Override
  public final TimeSeries lowerPredictionInterval(final int steps, final double alpha) {
    double[] upperPredictionValues = new double[steps];
    double criticalValue = new Normal(0, model.residuals().stdDeviation()).quantile(1 - alpha / 2);
    for (int t = 0; t < steps; t++) {
      upperPredictionValues[t] = forecast.at(t) - criticalValue * Math.sqrt(t + 1);
    }
    return new TimeSeries(forecast.timeScale(), forecast.observationTimes().get(0), forecast.periodLength(),
        upperPredictionValues);
  }

  /* (non-Javadoc)
   * @see timeseries.models.Forecast#pastAndFuture()
   */
  @Override
  public final void pastAndFuture() {
    new Thread(() -> {
      final List<Date> xAxis = new ArrayList<>(forecast.observationTimes().size());
      final List<Date> xAxisObs = new ArrayList<>(model.timeSeries().n());
      for (OffsetDateTime dateTime : model.timeSeries().observationTimes()) {
        xAxisObs.add(Date.from(dateTime.toInstant()));
      }
      for (OffsetDateTime dateTime : forecast.observationTimes()) {
        xAxis.add(Date.from(dateTime.toInstant()));
      }

      double critValue = new Normal(0, model.residuals().stdDeviation()).quantile(1 - alpha / 2);
      double[] errors = new double[forecast.n()];
      for (int t = 0; t < errors.length; t++) {
        errors[t] = critValue * Math.sqrt(t + 1);
      }

      List<Double> errorList = Doubles.asList(errors);
      List<Double> seriesList = Doubles.asList(model.timeSeries().series());
      List<Double> forecastList = Doubles.asList(forecast.series());
      final XYChart chart = new XYChartBuilder().theme(ChartTheme.GGPlot2).height(800).width(1200)
          .title("Random Walk Past and Future").build();

      XYSeries observationSeries = chart.addSeries("Past", xAxisObs, seriesList);
      XYSeries forecastSeries = chart.addSeries("Future", xAxis, forecastList, errorList);

      observationSeries.setMarker(new None());
      forecastSeries.setMarker(new None());

      observationSeries.setLineWidth(0.75f);
      forecastSeries.setLineWidth(1.5f);

      chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line).setErrorBarsColor(Color.RED);
      observationSeries.setLineColor(Color.BLACK);
      forecastSeries.setLineColor(Color.BLUE);

      JPanel panel = new XChartPanel<>(chart);
      JFrame frame = new JFrame("Random Walk Past and Future");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(panel);
      frame.pack();
      frame.setVisible(true);
    }).start();
  }

  /* (non-Javadoc)
   * @see timeseries.models.Forecast#plot()
   */
  @Override
  public final void plot() {
    new Thread(() -> {
      final List<Date> xAxis = new ArrayList<>(forecast.observationTimes().size());
      for (OffsetDateTime dateTime : forecast.observationTimes()) {
        xAxis.add(Date.from(dateTime.toInstant()));
      }

      double critValue = new Normal(0, model.residuals().stdDeviation()).quantile(1 - alpha / 2);
      double[] errors = new double[forecast.n()];
      for (int t = 0; t < errors.length; t++) {
        errors[t] = critValue * Math.sqrt(t + 1);
      }

      List<Double> errorList = Doubles.asList(errors);
      List<Double> forecastList = Doubles.asList(forecast.series());
      final XYChart chart = new XYChartBuilder().theme(ChartTheme.GGPlot2).height(600).width(800)
          .title("Random Walk Forecast").build();

      XYSeries forecastSeries = chart.addSeries("Forecast", xAxis, forecastList, errorList);
      forecastSeries.setMarker(new None());
      forecastSeries.setLineWidth(1.5f);

      chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line).setErrorBarsColor(Color.RED);
      forecastSeries.setLineColor(Color.BLUE);

      JPanel panel = new XChartPanel<>(chart);
      JFrame frame = new JFrame("Random Walk Forecast");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.add(panel);
      frame.pack();
      frame.setVisible(true);
    }).start();
  }

}