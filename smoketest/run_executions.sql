WITH
measurements AS (
    SELECT
        id
      , regexp_replace(name, '^prestoQuery-', '') AS name
      , unit
      , value
    FROM measurements
)
SELECT
    ex.id
  , m.name
  , m.unit
-- TODO format into human readable values
  , m.value
FROM execution_measurements em
JOIN executions ex ON ex.id = em.execution_id
JOIN measurements m ON m.id = em.measurement_id
WHERE ex.benchmark_run_id = :id
ORDER BY ex.id, name, unit, value
