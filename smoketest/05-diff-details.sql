-- Run details
-- List results for every metric for all runs in every environments.
-- TODO this should be generate in a loop, presenting run properties as a separate table in a header
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
    FROM execution_measurements em
    JOIN executions ex ON ex.id = em.execution_id
    JOIN benchmark_runs runs ON runs.id = ex.benchmark_run_id
    JOIN measurements m ON m.id = em.measurement_id
    GROUP BY 1, 2, 3
)
SELECT
    env.name AS env_name
  , run.id AS run_id
  , attrs.tuples AS run_attrs
  , vars.tuples AS run_vars
  , ex.name AS metric
  , ex.unit AS unit
  , ex.mean AS mean
  , ex.stddev AS stddev
  , 100 * ex.stddev / nullif(cast(ex.mean as real), 0) AS stddev_pct
  , ex.min AS min
  , ex.max AS max
FROM runs run
JOIN execution_devs ex ON ex.run_id = run.id
JOIN environments env ON env.id = run.environment_id
JOIN attributes attrs ON attrs.benchmark_run_id = run.id
JOIN variables vars ON vars.benchmark_run_id = run.id
ORDER BY env_name, run_id, metric
;
