/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.trino.plugin.faker;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.airlift.slice.Slice;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.catalog.CatalogName;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorOutputMetadata;
import io.trino.spi.connector.ConnectorOutputTableHandle;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableLayout;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.ConnectorViewDefinition;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.ConstraintApplicationResult;
import io.trino.spi.connector.LimitApplicationResult;
import io.trino.spi.connector.RelationColumnsMetadata;
import io.trino.spi.connector.RetryMode;
import io.trino.spi.connector.SaveMode;
import io.trino.spi.connector.SchemaNotFoundException;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;
import io.trino.spi.function.BoundSignature;
import io.trino.spi.function.FunctionDependencyDeclaration;
import io.trino.spi.function.FunctionId;
import io.trino.spi.function.FunctionMetadata;
import io.trino.spi.function.SchemaFunctionName;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.security.TrinoPrincipal;
import io.trino.spi.statistics.ColumnStatisticMetadata;
import io.trino.spi.statistics.ColumnStatisticType;
import io.trino.spi.statistics.ComputedStatistics;
import io.trino.spi.statistics.TableStatisticType;
import io.trino.spi.statistics.TableStatisticsMetadata;
import io.trino.spi.type.BigintType;
import io.trino.spi.type.CharType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarbinaryType;
import io.trino.spi.type.VarcharType;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.trino.spi.StandardErrorCode.INVALID_COLUMN_PROPERTY;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.trino.spi.StandardErrorCode.SCHEMA_ALREADY_EXISTS;
import static io.trino.spi.StandardErrorCode.SCHEMA_NOT_FOUND;
import static io.trino.spi.StandardErrorCode.TABLE_ALREADY_EXISTS;
import static io.trino.spi.connector.RetryMode.NO_RETRIES;
import static io.trino.spi.type.BigintType.BIGINT;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class FakerMetadata
        implements ConnectorMetadata
{
    public static final String SCHEMA_NAME = "default";
    public static final String ROW_ID_COLUMN_NAME = "$row_id";
    private static final String VIEW_SUFFIX = "_view";

    @GuardedBy("this")
    private final List<SchemaInfo> schemas = new ArrayList<>();
    private final double nullProbability;
    private final long defaultLimit;
    private final double maxDistinctValuesRatio;
    private final FakerFunctionProvider functionsProvider;
    private final CatalogName catalogName;

    @GuardedBy("this")
    private final Map<SchemaTableName, TableInfo> tables = new HashMap<>();
    private final Map<SchemaTableName, String> views = new HashMap<>();

    @Inject
    public FakerMetadata(FakerConfig config, FakerFunctionProvider functionProvider, CatalogName catalogName)
    {
        this.schemas.add(new SchemaInfo(SCHEMA_NAME, Map.of()));
        this.nullProbability = config.getNullProbability();
        this.defaultLimit = config.getDefaultLimit();
        this.maxDistinctValuesRatio = config.getMaxDistinctValuesRatio();
        this.functionsProvider = requireNonNull(functionProvider, "functionProvider is null");
        this.catalogName = requireNonNull(catalogName, "catalogName is null");
    }

    @Override
    public synchronized List<String> listSchemaNames(ConnectorSession connectorSession)
    {
        return schemas.stream()
                .map(SchemaInfo::name)
                .collect(toImmutableList());
    }

    @Override
    public synchronized void createSchema(ConnectorSession session, String schemaName, Map<String, Object> properties, TrinoPrincipal owner)
    {
        if (schemas.stream().anyMatch(schema -> schema.name().equals(schemaName))) {
            throw new TrinoException(SCHEMA_ALREADY_EXISTS, format("Schema '%s' already exists", schemaName));
        }
        schemas.add(new SchemaInfo(schemaName, properties));
    }

    @Override
    public synchronized void dropSchema(ConnectorSession session, String schemaName, boolean cascade)
    {
        verify(schemas.remove(getSchema(schemaName)));
    }

    private synchronized SchemaInfo getSchema(String name)
    {
        Optional<SchemaInfo> schema = schemas.stream()
                .filter(schemaInfo -> schemaInfo.name().equals(name))
                .findAny();
        if (schema.isEmpty()) {
            throw new TrinoException(SCHEMA_NOT_FOUND, format("Schema '%s' does not exist", name));
        }
        return schema.get();
    }

    @Override
    public synchronized ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName, Optional<ConnectorTableVersion> startVersion, Optional<ConnectorTableVersion> endVersion)
    {
        if (startVersion.isPresent() || endVersion.isPresent()) {
            throw new TrinoException(NOT_SUPPORTED, "This connector does not support versioned tables");
        }
        SchemaInfo schema = getSchema(tableName.getSchemaName());
        TableInfo tableInfo = tables.get(tableName);
        if (tableInfo == null) {
            return null;
        }
        long schemaLimit = (long) schema.properties().getOrDefault(SchemaInfo.DEFAULT_LIMIT_PROPERTY, defaultLimit);
        long tableLimit = (long) tables.get(tableName).properties().getOrDefault(TableInfo.DEFAULT_LIMIT_PROPERTY, schemaLimit);
        return new FakerTableHandle(tableName, TupleDomain.all(), tableLimit);
    }

    @Override
    public synchronized ConnectorTableMetadata getTableMetadata(
            ConnectorSession connectorSession,
            ConnectorTableHandle connectorTableHandle)
    {
        FakerTableHandle tableHandle = (FakerTableHandle) connectorTableHandle;
        SchemaTableName name = tableHandle.schemaTableName();
        TableInfo tableInfo = tables.get(tableHandle.schemaTableName());
        return new ConnectorTableMetadata(
                name,
                tableInfo.columns().stream().map(ColumnInfo::metadata).collect(toImmutableList()),
                tableInfo.properties(),
                tableInfo.comment());
    }

    @Override
    public synchronized Optional<ConnectorViewDefinition> getView(ConnectorSession session, SchemaTableName viewName)
    {
        SchemaTableName tableName = SchemaTableName.schemaTableName(viewName.getSchemaName(), stripSuffix(viewName.getTableName(), VIEW_SUFFIX));
        TableInfo tableInfo = tables.get(tableName);
        if (tableInfo == null || !views.containsKey(viewName)) {
            return Optional.empty();
        }
        return Optional.of(new ConnectorViewDefinition(
                views.get(viewName),
                Optional.empty(),
                Optional.empty(),
                tableInfo.columns().stream()
                        .map(column -> new ConnectorViewDefinition.ViewColumn(
                                column.name(),
                                column.type().getTypeId(),
                                Optional.empty()))
                        .collect(toImmutableList()),
                Optional.empty(),
                Optional.empty(),
                true,
                List.of()));
    }

    private static String stripSuffix(String key, String suffix)
    {
        return key.endsWith(suffix)
                ? key.substring(0, key.length() - suffix.length())
                : key;
    }

    @Override
    public synchronized List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        return Stream.concat(tables.keySet().stream(), views.keySet().stream())
                .filter(table -> schemaName.map(table.getSchemaName()::contentEquals).orElse(true))
                .collect(toImmutableList());
    }

    @Override
    public synchronized List<SchemaTableName> listViews(ConnectorSession session, Optional<String> schemaName)
    {
        return views.keySet().stream()
                .filter(table -> schemaName.map(table.getSchemaName()::contentEquals).orElse(true))
                .collect(toImmutableList());
    }

    @Override
    public synchronized Map<String, ColumnHandle> getColumnHandles(
            ConnectorSession connectorSession,
            ConnectorTableHandle connectorTableHandle)
    {
        FakerTableHandle tableHandle = (FakerTableHandle) connectorTableHandle;
        return tables.get(tableHandle.schemaTableName())
                .columns().stream()
                .collect(toImmutableMap(ColumnInfo::name, ColumnInfo::handle));
    }

    @Override
    public synchronized ColumnMetadata getColumnMetadata(
            ConnectorSession connectorSession,
            ConnectorTableHandle connectorTableHandle,
            ColumnHandle columnHandle)
    {
        FakerTableHandle tableHandle = (FakerTableHandle) connectorTableHandle;
        return tables.get(tableHandle.schemaTableName())
                .column(columnHandle)
                .metadata();
    }

    @Override
    public synchronized Iterator<RelationColumnsMetadata> streamRelationColumns(
            ConnectorSession session,
            Optional<String> schemaName,
            UnaryOperator<Set<SchemaTableName>> relationFilter)
    {
        Map<SchemaTableName, RelationColumnsMetadata> relationColumns = new HashMap<>();

        SchemaTablePrefix prefix = schemaName.map(SchemaTablePrefix::new)
                .orElseGet(SchemaTablePrefix::new);
        tables.entrySet().stream()
                .filter(entry -> prefix.matches(entry.getKey()))
                .forEach(entry -> {
                    SchemaTableName name = entry.getKey();
                    RelationColumnsMetadata columns = RelationColumnsMetadata.forTable(
                            name,
                            entry.getValue().columns().stream()
                                    .map(ColumnInfo::metadata)
                                    .collect(toImmutableList()));
                    relationColumns.put(name, columns);
                });

        for (Map.Entry<SchemaTableName, ConnectorViewDefinition> entry : getViews(session, schemaName).entrySet()) {
            relationColumns.put(entry.getKey(), RelationColumnsMetadata.forView(entry.getKey(), entry.getValue().getColumns()));
        }

        return relationFilter.apply(relationColumns.keySet()).stream()
                .map(relationColumns::get)
                .iterator();
    }

    @Override
    public synchronized void dropTable(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        FakerTableHandle handle = (FakerTableHandle) tableHandle;
        tables.remove(handle.schemaTableName());
    }

    @Override
    public synchronized void renameTable(ConnectorSession session, ConnectorTableHandle tableHandle, SchemaTableName newTableName)
    {
        checkSchemaExists(newTableName.getSchemaName());
        checkTableNotExists(newTableName);

        FakerTableHandle handle = (FakerTableHandle) tableHandle;
        SchemaTableName oldTableName = handle.schemaTableName();

        tables.remove(oldTableName);
        tables.put(newTableName, tables.get(oldTableName));
    }

    @Override
    public synchronized void setTableProperties(ConnectorSession session, ConnectorTableHandle tableHandle, Map<String, Optional<Object>> properties)
    {
        FakerTableHandle handle = (FakerTableHandle) tableHandle;
        SchemaTableName tableName = handle.schemaTableName();

        TableInfo oldInfo = tables.get(tableName);
        Map<String, Object> newProperties = Stream.concat(
                        oldInfo.properties().entrySet().stream()
                                .filter(entry -> !properties.containsKey(entry.getKey())),
                        properties.entrySet().stream()
                                .filter(entry -> entry.getValue().isPresent()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tables.put(tableName, oldInfo.withProperties(newProperties));
    }

    @Override
    public synchronized void setTableComment(ConnectorSession session, ConnectorTableHandle tableHandle, Optional<String> comment)
    {
        FakerTableHandle handle = (FakerTableHandle) tableHandle;
        SchemaTableName tableName = handle.schemaTableName();

        TableInfo oldInfo = requireNonNull(tables.get(tableName), "tableInfo is null");
        tables.put(tableName, oldInfo.withComment(comment));
    }

    @Override
    public synchronized void setColumnComment(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle column, Optional<String> comment)
    {
        FakerTableHandle handle = (FakerTableHandle) tableHandle;
        SchemaTableName tableName = handle.schemaTableName();

        TableInfo oldInfo = tables.get(tableName);
        List<ColumnInfo> columns = oldInfo.columns().stream()
                .map(columnInfo -> {
                    if (ROW_ID_COLUMN_NAME.equals(columnInfo.handle().name())) {
                        throw new IllegalArgumentException(String.format("Cannot set comment for %s column", ROW_ID_COLUMN_NAME));
                    }
                    if (columnInfo.handle().equals(column)) {
                        return columnInfo.withComment(comment);
                    }
                    return columnInfo;
                })
                .collect(toImmutableList());
        tables.put(tableName, oldInfo.withColumns(columns));
    }

    @Override
    public synchronized void createTable(ConnectorSession session, ConnectorTableMetadata tableMetadata, SaveMode saveMode)
    {
        if (saveMode == SaveMode.REPLACE) {
            throw new TrinoException(NOT_SUPPORTED, "This connector does not support replacing tables");
        }
        ConnectorOutputTableHandle outputTableHandle = beginCreateTable(session, tableMetadata, Optional.empty(), NO_RETRIES, false);
        finishCreateTable(session, outputTableHandle, ImmutableList.of(), ImmutableList.of());
    }

    @Override
    public synchronized FakerOutputTableHandle beginCreateTable(ConnectorSession session, ConnectorTableMetadata tableMetadata, Optional<ConnectorTableLayout> layout, RetryMode retryMode, boolean replace)
    {
        if (replace) {
            throw new TrinoException(NOT_SUPPORTED, "This connector does not support replacing tables");
        }
        SchemaTableName tableName = tableMetadata.getTable();
        SchemaInfo schema = getSchema(tableName.getSchemaName());
        checkTableNotExists(tableMetadata.getTable());

        double schemaNullProbability = (double) schema.properties().getOrDefault(SchemaInfo.NULL_PROBABILITY_PROPERTY, nullProbability);
        double tableNullProbability = (double) tableMetadata.getProperties().getOrDefault(TableInfo.NULL_PROBABILITY_PROPERTY, schemaNullProbability);
        double schemaMaxDistinctValuesRatio = (double) schema.properties().getOrDefault(SchemaInfo.MAX_DISTINCT_VALUES_RATIO, maxDistinctValuesRatio);
        double tableMaxDistinctValuesRatio = (double) tableMetadata.getProperties().getOrDefault(TableInfo.MAX_DISTINCT_VALUES_RATIO, schemaMaxDistinctValuesRatio);

        ImmutableList.Builder<ColumnInfo> columnsBuilder = ImmutableList.builder();
        int columnId = 0;
        for (; columnId < tableMetadata.getColumns().size(); columnId++) {
            ColumnMetadata column = tableMetadata.getColumns().get(columnId);
            double nullProbability = 0;
            if (column.isNullable()) {
                nullProbability = (double) column.getProperties().getOrDefault(ColumnInfo.NULL_PROBABILITY_PROPERTY, tableNullProbability);
            }
            String generator = (String) column.getProperties().get(ColumnInfo.GENERATOR_PROPERTY);
            if (generator != null) {
                if (!isCharacterColumn(column)) {
                    throw new TrinoException(INVALID_COLUMN_PROPERTY, "The `generator` property can only be set for CHAR, VARCHAR or VARBINARY columns");
                }
                if (column.getProperties().containsKey(ColumnInfo.NULL_PROBABILITY_PROPERTY)) {
                    throw new TrinoException(INVALID_COLUMN_PROPERTY, "The `max_distinct_values_ratio` and the `generator` properties cannot be used at the same time");
                }
            }
            double maxDistinctValuesRatio = (double) column.getProperties().getOrDefault(ColumnInfo.MAX_DISTINCT_VALUES_RATIO, tableMaxDistinctValuesRatio);
            columnsBuilder.add(new ColumnInfo(
                    new FakerColumnHandle(
                            columnId,
                            column.getName(),
                            column.getType(),
                            nullProbability,
                            maxDistinctValuesRatio,
                            generator),
                    column));
        }

        columnsBuilder.add(new ColumnInfo(
                new FakerColumnHandle(
                        columnId,
                        ROW_ID_COLUMN_NAME,
                        BigintType.BIGINT,
                        0,
                        1,
                        ""),
                ColumnMetadata.builder()
                        .setName(ROW_ID_COLUMN_NAME)
                        .setType(BigintType.BIGINT)
                        .setHidden(true)
                        .setNullable(false)
                        .build()));

        List<ColumnInfo> columns = columnsBuilder.build();
        tables.put(tableName, new TableInfo(
                columns,
                tableMetadata.getProperties(),
                tableMetadata.getComment()));

        return new FakerOutputTableHandle(
                tableName,
                columns.stream()
                        .map(ColumnInfo::name)
                        .collect(toImmutableList()),
                columns.stream()
                        .map(ColumnInfo::type)
                        .collect(toImmutableList()));
    }

    private boolean isCharacterColumn(ColumnMetadata column)
    {
        return column.getType() instanceof CharType || column.getType() instanceof VarcharType || column.getType() instanceof VarbinaryType;
    }

    private synchronized void checkSchemaExists(String schemaName)
    {
        if (schemas.stream().noneMatch(schema -> schema.name().equals(schemaName))) {
            throw new SchemaNotFoundException(schemaName);
        }
    }

    private synchronized void checkTableNotExists(SchemaTableName tableName)
    {
        if (tables.containsKey(tableName)) {
            throw new TrinoException(TABLE_ALREADY_EXISTS, format("Table '%s' already exists", tableName));
        }
    }

    @Override
    public synchronized Optional<ConnectorOutputMetadata> finishCreateTable(
            ConnectorSession session,
            ConnectorOutputTableHandle tableHandle,
            Collection<Slice> fragments,
            Collection<ComputedStatistics> computedStatistics)
    {
        requireNonNull(tableHandle, "tableHandle is null");
        FakerOutputTableHandle fakerOutputHandle = (FakerOutputTableHandle) tableHandle;

        SchemaTableName tableName = fakerOutputHandle.schemaTableName();

        TableInfo info = tables.get(tableName);
        requireNonNull(info, "info is null");

        Map<String, Object> minimums = new HashMap<>();
        Map<String, Object> maximums = new HashMap<>();
        Map<String, Long> distincts = new HashMap<>();
        AtomicReference<Long> rowCount = new AtomicReference<>(0L);
        if (!computedStatistics.isEmpty()) {
            computedStatistics.forEach(statistic -> {
                Block rowCountBlock = statistic.getTableStatistics().get(TableStatisticType.ROW_COUNT);
                requireNonNull(rowCountBlock, "rowCountBlock is null");
                // TODO overwrite, or compare?
                rowCount.set((Long) getValue(BIGINT, 0, rowCountBlock));

                statistic.getColumnStatistics().forEach((metadata, block) -> {
                    checkState(block.getPositionCount() == 1, "Expected a single position in aggregation result block");
                    String columnName = metadata.getColumnName();
                    Type type = fakerOutputHandle.columnTypes().get(fakerOutputHandle.columnNames().indexOf(columnName));
                    if (metadata.getStatisticType().equals(ColumnStatisticType.MIN_VALUE)) {
                        // TODO overwrite, or compare?
                        minimums.put(columnName, getValue(type, 0, block));
                    }
                    else if (metadata.getStatisticType().equals(ColumnStatisticType.MAX_VALUE)) {
                        maximums.put(columnName, getValue(type, 0, block));
                    }
                    else if (metadata.getStatisticType().equals(ColumnStatisticType.NUMBER_OF_DISTINCT_VALUES)) {
                        distincts.put(columnName, (Long) getValue(BIGINT, 0, block));
                    }
                    else {
                        throw new IllegalArgumentException("Unsupported column statistic type: " + metadata.getStatisticType());
                    }
                });
            });
        }

        List<ColumnInfo> columns = info.columns().stream()
                .map(column -> new ColumnInfo(
                        !distincts.containsKey(column.name()) ?
                                column.handle()
                                : column.handle().withMaxDistinctValuesRatio(distincts.get(column.name()).doubleValue() / rowCount.get()),
                        column.metadata()))
                .collect(toImmutableList());
        tables.put(tableName, new TableInfo(columns, info.properties(), info.comment()));
        views.put(
                new SchemaTableName(tableName.getSchemaName(), tableName.getTableName() + VIEW_SUFFIX),
                "SELECT *, \"%s\" FROM %s.%s WHERE %s".formatted(
                        ROW_ID_COLUMN_NAME,
                        catalogName,
                        tableName,
                        minimums.entrySet().stream()
                                .map(entry -> "%s BETWEEN %s AND %s".formatted(
                                        entry.getKey(),
                                        entry.getValue().toString(),
                                        maximums.get(entry.getKey()).toString()))
                                .collect(joining(" AND "))));
        return Optional.empty();
    }

    private Object getValue(Type type, int position, Block block)
    {
        Class<?> javaType = type.getJavaType();
        if (javaType == boolean.class) {
            return type.getBoolean(block, position);
        }
        if (javaType == long.class) {
            return type.getLong(block, position);
        }
        if (javaType == double.class) {
            return type.getDouble(block, position);
        }
        if (javaType == Slice.class) {
            return type.getSlice(block, position).toStringUtf8();
        }
        return type.getObject(block, position);
    }

    @Override
    public TableStatisticsMetadata getStatisticsCollectionMetadataForWrite(ConnectorSession session, ConnectorTableMetadata tableMetadata)
    {
        return new TableStatisticsMetadata(
                tableMetadata.getColumns().stream()
                        .flatMap(column -> Stream.of(
                                new ColumnStatisticMetadata(column.getName(), ColumnStatisticType.MIN_VALUE),
                                new ColumnStatisticMetadata(column.getName(), ColumnStatisticType.MAX_VALUE),
                                new ColumnStatisticMetadata(column.getName(), ColumnStatisticType.NUMBER_OF_DISTINCT_VALUES)))
                        .collect(toImmutableSet()),
                Set.of(TableStatisticType.ROW_COUNT),
                List.of());
    }

    @Override
    public synchronized Map<String, Object> getSchemaProperties(ConnectorSession session, String schemaName)
    {
        Optional<SchemaInfo> schema = schemas.stream()
                .filter(s -> s.name().equals(schemaName))
                .findAny();
        if (schema.isEmpty()) {
            throw new TrinoException(SCHEMA_NOT_FOUND, format("Schema '%s' does not exist", schemaName));
        }

        return schema.get().properties();
    }

    @Override
    public Optional<LimitApplicationResult<ConnectorTableHandle>> applyLimit(
            ConnectorSession session,
            ConnectorTableHandle table,
            long limit)
    {
        FakerTableHandle fakerTable = (FakerTableHandle) table;
        if (fakerTable.limit() == limit) {
            return Optional.empty();
        }
        return Optional.of(new LimitApplicationResult<>(
                fakerTable.withLimit(limit),
                false,
                true));
    }

    @Override
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(
            ConnectorSession session,
            ConnectorTableHandle table,
            Constraint constraint)
    {
        FakerTableHandle fakerTable = (FakerTableHandle) table;

        TupleDomain<ColumnHandle> summary = constraint.getSummary();
        if (summary.isAll()) {
            return Optional.empty();
        }
        // the only reason not to use isNone is so the linter doesn't complain about not checking an Optional
        if (summary.getDomains().isEmpty()) {
            throw new IllegalArgumentException("summary cannot be none");
        }

        TupleDomain<ColumnHandle> currentConstraint = fakerTable.constraint();
        if (currentConstraint.getDomains().isEmpty()) {
            throw new IllegalArgumentException("currentConstraint is none but should be all!");
        }

        // push down everything, unsupported constraints will throw an exception during data generation
        boolean anyUpdated = false;
        for (Map.Entry<ColumnHandle, Domain> entry : summary.getDomains().get().entrySet()) {
            FakerColumnHandle column = (FakerColumnHandle) entry.getKey();
            Domain domain = entry.getValue();

            if (currentConstraint.getDomains().get().containsKey(column)) {
                Domain currentDomain = currentConstraint.getDomains().get().get(column);
                // it is important to avoid processing same constraint multiple times
                // so that planner doesn't get stuck in a loop
                if (currentDomain.equals(domain)) {
                    continue;
                }
                // TODO write test cases for this, it doesn't seem to work with IS NULL
                currentDomain.union(domain);
            }
            else {
                Map<ColumnHandle, Domain> domains = new HashMap<>(currentConstraint.getDomains().get());
                domains.put(column, domain);
                currentConstraint = TupleDomain.withColumnDomains(domains);
            }
            anyUpdated = true;
        }
        if (!anyUpdated) {
            return Optional.empty();
        }

        return Optional.of(new ConstraintApplicationResult<>(
                fakerTable.withConstraint(currentConstraint),
                TupleDomain.all(),
                constraint.getExpression(),
                true));
    }

    @Override
    public Collection<FunctionMetadata> listFunctions(ConnectorSession session, String schemaName)
    {
        return functionsProvider.functionsMetadata();
    }

    @Override
    public Collection<FunctionMetadata> getFunctions(ConnectorSession session, SchemaFunctionName name)
    {
        return functionsProvider.functionsMetadata().stream()
                .filter(function -> function.getCanonicalName().equals(name.getFunctionName()))
                .collect(toImmutableList());
    }

    @Override
    public FunctionMetadata getFunctionMetadata(ConnectorSession session, FunctionId functionId)
    {
        return functionsProvider.functionsMetadata().stream()
                .filter(function -> function.getFunctionId().equals(functionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown function " + functionId));
    }

    @Override
    public FunctionDependencyDeclaration getFunctionDependencies(ConnectorSession session, FunctionId functionId, BoundSignature boundSignature)
    {
        return FunctionDependencyDeclaration.NO_DEPENDENCIES;
    }
}
