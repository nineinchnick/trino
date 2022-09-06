-- Comparison validation
-- Check which benchmark runs are comparable across environments and if not, which attributes are different
WITH
attributes AS (
    SELECT
        benchmark_run_id
      , array_agg(row(name, value) ORDER BY name) AS tuples
    FROM benchmark_runs_attributes
    GROUP BY 1
)
, variables AS (
    SELECT
        benchmark_run_id
      , array_agg(row(name, value) ORDER BY name) AS tuples
    FROM benchmark_runs_variables
    GROUP BY 1
)
, runs AS (
    SELECT
        runs.id
      , runs.environment_id
      , runs.status
      , attrs.tuples AS attributes
      , vars.tuples AS variables
      , attrs.tuples || vars.tuples AS properties
    FROM benchmark_runs runs
    LEFT JOIN attributes attrs ON attrs.benchmark_run_id = runs.id
    LEFT JOIN variables vars ON vars.benchmark_run_id = runs.id
)
, unique_runs AS (
    SELECT
        runs.properties
      , runs.status
      , row_number() OVER (ORDER BY runs.properties, runs.status) AS rownum
      , array_agg(runs.id) AS ids
      , array_agg(runs.environment_id) AS environment_ids
      , array_agg(envs.name) AS environment_names
    FROM runs
    JOIN environments envs ON envs.id = runs.environment_id
    GROUP BY runs.properties, runs.status
)
SELECT
    env.name AS env_name
  , count(*) FILTER (WHERE contains(runs.environment_ids, env.id)) AS num_runs
  , count(*) FILTER (WHERE contains(runs.environment_ids, env.id) AND runs.status != 'ENDED') AS num_invalid_runs
  , count(*) FILTER (WHERE contains(runs.environment_ids, env.id) AND runs.status = 'ENDED' AND cardinality(runs.environment_ids) > 1) AS num_comparable_runs
  , count(*) FILTER (WHERE NOT contains(runs.environment_ids, env.id) AND runs.status = 'ENDED') AS num_missing_runs
  , count(*) FILTER (WHERE contains(runs.environment_ids, env.id) AND runs.status = 'ENDED' AND cardinality(runs.environment_ids) = 1) AS num_extra_runs
  , array_subtraction(
        array_union_agg(runs.environment_names) FILTER (WHERE contains(runs.environment_ids, env.id) AND runs.status = 'ENDED' AND cardinality(runs.environment_ids) > 1),
        ARRAY[env.name]) AS comparable_envs
  , array_subtraction(
        array_union_agg(runs.properties::text[]) FILTER (WHERE NOT contains(runs.environment_ids, env.id) AND runs.status = 'ENDED'),
        array_union_agg(runs.properties::text[]) FILTER (WHERE contains(runs.environment_ids, env.id) AND runs.status = 'ENDED')) AS missing_run_properties
  , array_subtraction(
        array_union_agg(runs.properties::text[]) FILTER (WHERE contains(runs.environment_ids, env.id) AND runs.status = 'ENDED' AND cardinality(runs.environment_ids) = 1),
        array_union_agg(runs.properties::text[]) FILTER (WHERE NOT contains(runs.environment_ids, env.id) AND runs.status = 'ENDED')) AS extra_run_properties
FROM environments env
CROSS JOIN unique_runs runs
GROUP BY env.name
ORDER BY env.name
;
