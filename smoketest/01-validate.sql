-- Validate benchmarks
-- Check for statistical outliers on every metric across all executions of every run (query?) of every benchmark.
/* TODO remaining questions:
- how to explain different number of runs between envs
- do we want to distinguish duration from other metrics?
- do we want to distinguish execution and benchmark (system) metrics?
*/
WITH
measurements AS (
    SELECT
        id
      , regexp_replace(name, '^prestoQuery-', '') AS name
      , unit
      , value
    FROM measurements
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
        devs.run_id
      , count(DISTINCT ex.id) AS num_executions
      , count(DISTINCT ex.id) FILTER (WHERE m.value NOT BETWEEN devs.low AND devs.high) AS num_invalid_executions
      , count(*) AS num_measurements
      , count(*) FILTER (WHERE m.value NOT BETWEEN devs.low AND devs.high) AS num_outliers
      , count(*) FILTER (WHERE m.name = 'duration' AND m.value NOT BETWEEN devs.low AND devs.high) AS num_duration_outliers
      , array_agg(DISTINCT m.name ORDER BY m.name) FILTER (WHERE m.value NOT BETWEEN devs.low AND devs.high) AS names_outliers
      , array_agg(DISTINCT m.name ORDER BY m.name) AS names_all
    FROM execution_devs devs
    JOIN executions ex ON ex.benchmark_run_id = devs.run_id
    JOIN execution_measurements em ON em.execution_id = ex.id
    JOIN measurements m ON m.id = em.measurement_id AND m.name = devs.name
    GROUP BY 1
)
SELECT
    env.name
  , runs.sequence_id
  , array_agg(DISTINCT runs.status) AS statuses
  , count(DISTINCT runs.id) AS num_runs
  , sum(s.num_executions) AS num_executions
  , sum(s.num_invalid_executions) AS num_invalid_executions
  , sum(s.num_measurements) AS num_measurements
  , sum(s.num_outliers) AS num_outliers
  , sum(s.num_duration_outliers) AS num_duration_outliers
  -- TODO use this in Trino
  -- , array_sort(array_distinct(flatten(array_agg(s.names_outliers)))) AS names_outliers
  -- , array_sort(array_distinct(flatten(array_agg(s.names_all)))) AS names_ok
  , array_union_agg(s.names_outliers) AS names_outliers
  , array_subtraction(array_union_agg(s.names_all), array_union_agg(s.names_outliers)) AS names_ok
FROM environments env
LEFT JOIN benchmark_runs runs ON runs.environment_id = env.id
LEFT JOIN execution_stats s ON s.run_id = runs.id
GROUP BY 1, 2
;
