-- Validate metrics
-- For every metric, check for statistical outliers across all executions of every run (query?) of every benchmark.
-- The number of runs should be equal across all metrics and there should be no invalid executions and no outliers.
WITH
metrics AS (
    SELECT
        m.id
      , m.name
      , m.unit
      , min(a.value) FILTER (WHERE a.name = 'scope') AS scope
      , array_agg(row(a.name, a.value) ORDER BY a.name) AS attributes
    FROM metrics m
    JOIN metric_attributes a ON a.metric_id = m.id
    GROUP BY m.id, m.name, m.unit
)
, measurements AS (
    SELECT
        v.id
      , v.metric_id
      , m.name
      , m.unit
      , v.value
      , m.scope
    FROM measurements v
    JOIN metrics m ON m.id = v.metric_id
)
, execution_devs AS (
    SELECT
        runs.id AS run_id
      , m.name
      , m.unit
      -- consider using this: https://github.com/sharkdp/hyperfine/blob/master/src/outlier_detection.rs
      -- this doesn't work well for low number of runs, because the ratio of the distance from the mean
      -- divided by the SD can never exceed (N-1)/sqrt(N)
      -- so for 3 runs no outlier can possibly be more than 1.155*SD from the mean
      , avg(m.value) - 2 * stddev(m.value) AS low
      , avg(m.value) + 2 * stddev(m.value) AS high
    FROM execution_measurements em
    JOIN executions ex ON ex.id = em.execution_id
    JOIN benchmark_runs runs ON runs.id = ex.benchmark_run_id
    JOIN measurements m ON m.id = em.measurement_id
    GROUP BY 1, 2, 3
)
, execution_stats AS (
    SELECT
        m.metric_id
      , count(DISTINCT ex.id) AS num_executions
      , count(DISTINCT ex.id) FILTER (WHERE m.value NOT BETWEEN devs.low AND devs.high) AS num_invalid_executions
      , count(*) AS num_measurements
      , count(*) FILTER (WHERE m.value NOT BETWEEN devs.low AND devs.high) AS num_outliers
      , count(*) FILTER (WHERE m.scope = 'driver' AND m.value NOT BETWEEN devs.low AND devs.high) AS num_driver_outliers
      , array_agg(DISTINCT m.name ORDER BY m.name) FILTER (WHERE m.value NOT BETWEEN devs.low AND devs.high) AS names_outliers
      , array_agg(DISTINCT m.name ORDER BY m.name) AS names_all
    FROM execution_devs devs
    JOIN executions ex ON ex.benchmark_run_id = devs.run_id
    JOIN execution_measurements em ON em.execution_id = ex.id
    JOIN measurements m ON m.id = em.measurement_id AND m.name = devs.name
    GROUP BY 1
)
SELECT
    m.name
  , m.attributes
  , sum(s.num_executions) AS num_executions
  , sum(s.num_invalid_executions) AS num_invalid_executions
  , sum(s.num_measurements) AS num_measurements
  , sum(s.num_outliers) AS num_outliers
  , sum(s.num_driver_outliers) AS num_driver_outliers
  -- TODO use this in Trino
  -- , array_sort(array_distinct(flatten(array_agg(s.names_outliers)))) AS names_outliers
  -- , array_sort(array_distinct(flatten(array_agg(s.names_all)))) AS names_ok
  , array_union_agg(s.names_outliers) AS names_outliers
  -- TODO only print these if there are any outliers?
  --, array_subtraction(array_union_agg(s.names_all), array_union_agg(s.names_outliers)) AS names_ok
FROM metrics m
LEFT JOIN execution_stats s ON s.metric_id = m.id
GROUP BY 1, 2
;
