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
package io.trino.plugin.redshift;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import io.trino.plugin.jdbc.DefaultQueryBuilder;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcNamedRelationHandle;
import io.trino.plugin.jdbc.JdbcQueryRelationHandle;
import io.trino.plugin.jdbc.JdbcRelationHandle;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static io.trino.plugin.redshift.RedshiftSessionProperties.useUnload;
import static java.lang.String.format;

public class RedshiftUnloadQueryBuilder
        extends DefaultQueryBuilder
{
    public RedshiftUnloadQueryBuilder(RemoteQueryModifier queryModifier)
    {
        super(queryModifier);
    }

    @Override
    public PreparedQuery prepareSelectQuery(
            JdbcClient client,
            ConnectorSession session,
            Connection connection,
            JdbcRelationHandle baseRelation,
            Optional<List<List<JdbcColumnHandle>>> groupingSets,
            List<JdbcColumnHandle> columns,
            Map<String, ParameterizedExpression> columnExpressions,
            TupleDomain<ColumnHandle> tupleDomain,
            Optional<ParameterizedExpression> additionalPredicate)
    {
        if (!useUnload(session)) {
            return super.prepareSelectQuery(
                    client,
                    session,
                    connection,
                    baseRelation,
                    groupingSets,
                    columns,
                    columnExpressions,
                    tupleDomain,
                    additionalPredicate);
        }
        return prepareSelectQueryWithoutParameters(
                client,
                session,
                connection,
                baseRelation,
                groupingSets,
                columns,
                columnExpressions,
                tupleDomain,
                additionalPredicate);
    }

    private PreparedQuery prepareSelectQueryWithoutParameters(
            JdbcClient client,
            ConnectorSession session,
            Connection connection,
            JdbcRelationHandle baseRelation,
            Optional<List<List<JdbcColumnHandle>>> groupingSets,
            List<JdbcColumnHandle> columns,
            Map<String, ParameterizedExpression> columnExpressions,
            TupleDomain<ColumnHandle> tupleDomain,
            Optional<ParameterizedExpression> additionalPredicate)
    {
        if (!tupleDomain.isNone()) {
            Map<ColumnHandle, Domain> domains = tupleDomain.getDomains().orElseThrow();
            columns.stream()
                    .filter(domains::containsKey)
                    .filter(column -> columnExpressions.containsKey(column.getColumnName()))
                    .findFirst()
                    .ifPresent(column -> {throw new IllegalArgumentException(format("Column %s has an expression and a constraint attached at the same time", column));});
        }

        ImmutableList.Builder<String> conjuncts = ImmutableList.builder();
        ImmutableList.Builder<QueryParameter> accumulator = ImmutableList.builder();

        String sql = "SELECT " + getProjection(client, columns, columnExpressions, accumulator::add);
        sql += getFrom(client, baseRelation, accumulator::add);

        toConjuncts(client, session, connection, tupleDomain, conjuncts, accumulator::add);
        additionalPredicate.ifPresent(predicate -> {
            conjuncts.add(predicate.expression());
            accumulator.addAll(predicate.parameters());
        });
        List<String> clauses = conjuncts.build();
        if (!clauses.isEmpty()) {
            sql += " WHERE " + Joiner.on(" AND ").join(clauses);
        }

        sql += getGroupBy(client, groupingSets);

        return new PreparedQuery(sql, accumulator.build());
    }

    private String getFrom(JdbcClient client, JdbcRelationHandle baseRelation, Consumer<QueryParameter> accumulator)
    {
        if (baseRelation instanceof JdbcNamedRelationHandle) {
            return " FROM " + getRelation(client, ((JdbcNamedRelationHandle) baseRelation).getRemoteTableName());
        }
        if (baseRelation instanceof JdbcQueryRelationHandle) {
            PreparedQuery preparedQuery = ((JdbcQueryRelationHandle) baseRelation).getPreparedQuery();
            preparedQuery.parameters().forEach(accumulator);
            return " FROM (" + preparedQuery.query() + ") o";
        }
        throw new IllegalArgumentException("Unsupported relation: " + baseRelation);
    }

    @Override
    public PreparedStatement prepareStatement(JdbcClient client, ConnectorSession session, Connection connection, PreparedQuery preparedQuery, Optional<Integer> columnCount)
            throws SQLException
    {
        if (!useUnload(session)) {
            return super.prepareStatement(client, session, connection, preparedQuery, columnCount);
        }
        // TODO is this needed at all?
        return super.prepareStatement(client, session, connection, preparedQuery, columnCount);
    }
}
