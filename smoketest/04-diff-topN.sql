-- Runs comparison
-- Find top 20 runs with same properties but from different environments with mean differences greater than the standard deviation
-- of the other metric and 5%.
-- There should be relatively few results with low difference percentage.
WITH
attributes AS (
    SELECT
        benchmark_run_id
      , array_agg(row(name, value) ORDER BY name, value) AS tuples
    FROM benchmark_runs_attributes
    GROUP BY 1
)
, variables AS (
    SELECT
        benchmark_run_id
      , array_agg(row(name, value) ORDER BY name, value) AS tuples
    FROM benchmark_runs_variables
    GROUP BY 1
)
, runs AS (
    SELECT
        runs.id
      , runs.environment_id
      , runs.sequence_id
      , attrs.tuples AS attributes
      , vars.tuples AS variables
      , attrs.tuples || vars.tuples AS properties
    FROM benchmark_runs runs
    LEFT JOIN attributes attrs ON attrs.benchmark_run_id = runs.id
    LEFT JOIN variables vars ON vars.benchmark_run_id = runs.id
    WHERE runs.status = 'ENDED'
)
, measurements AS (
    SELECT
        id
      , regexp_replace(name, '^prestoQuery-', '') AS name
      , unit
      , value
    FROM measurements
    -- TODO exclude some metrics that are expected to have lots of differences, like peakTotalMemoryReservation; or deliberately only include duration?
)
, execution_devs AS (
    SELECT
        runs.id AS run_id
      , m.name
      , m.unit
      , avg(m.value) AS mean
      , min(m.value) AS min
      , max(m.value) AS max
      , stddev(m.value) AS stddev
      , avg(m.value) - greatest(stddev(m.value), 0.05 * avg(m.value)) AS low
      , avg(m.value) + greatest(stddev(m.value), 0.05 * avg(m.value)) AS high
    FROM execution_measurements em
    JOIN executions ex ON ex.id = em.execution_id
    JOIN benchmark_runs runs ON runs.id = ex.benchmark_run_id
    JOIN measurements m ON m.id = em.measurement_id
    GROUP BY 1, 2, 3
)
SELECT
    env_left.name AS left_env_name
  , env_right.name AS right_env_name
  , run_left.id AS left_run_id
  , run_right.id AS right_run_id
  -- TODO this is too much info, replace with a link to details
  --, vars.tuples AS run_vars
  , ex_left.name AS metric
  , ex_left.unit AS unit
  -- result
  , ex_right.mean - ex_left.mean AS diff
  , 100 * abs(ex_right.mean - ex_left.mean) / nullif(cast(greatest(ex_right.mean, ex_left.mean) as real), 0) AS diff_pct
  -- details
  , ex_left.mean AS left_mean
  , ex_left.stddev AS left_stddev
  , 100 * ex_left.stddev / nullif(cast(ex_left.mean as real), 0) AS left_stddev_pct
  , ex_left.min AS left_min
  , ex_left.max AS left_max
  , ex_right.mean AS right_mean
  , ex_right.stddev AS right_stddev
  , 100 * ex_right.stddev / nullif(cast(ex_right.mean as real), 0) AS right_stddev_pct
  , ex_right.min AS right_min
  , ex_right.max AS right_max
FROM runs run_left
JOIN runs run_right ON run_left.environment_id != run_right.environment_id AND run_left.properties = run_right.properties
JOIN execution_devs ex_left ON ex_left.run_id = run_left.id
JOIN execution_devs ex_right ON ex_right.run_id = run_right.id AND ex_left.name = ex_right.name
JOIN environments env_left ON env_left.id = run_left.environment_id
JOIN environments env_right ON env_right.id = run_right.environment_id
JOIN variables vars ON vars.benchmark_run_id = run_left.id
WHERE
env_left.name < env_right.name
AND (ex_left.mean NOT BETWEEN ex_right.low AND ex_right.high OR ex_right.mean NOT BETWEEN ex_left.low AND ex_left.high)
ORDER BY diff_pct DESC, left_env_name, right_env_name, left_run_id, right_run_id, metric
LIMIT 20
;
